package de.htwg.tenant.service;

import de.htwg.tenant.client.dto.WorkflowRun;
import de.htwg.tenant.dto.CreateTenantRequest;
import de.htwg.tenant.dto.TenantResponse;
import de.htwg.tenant.model.Tenant;
import de.htwg.tenant.repository.TenantRepository;
import de.htwg.tenant.util.TenantIdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing tenant lifecycle.
 */
@ApplicationScoped
public class TenantService {

    private static final Logger LOG = Logger.getLogger(TenantService.class);

    @Inject
    TenantRepository tenantRepository;

    @Inject
    GitHubService githubService;

    @Inject
    EmailService emailService;

    @ConfigProperty(name = "tenant.base-domain")
    String baseDomain;

    @ConfigProperty(name = "tenant.environment", defaultValue = "prod")
    String environment;

    @ConfigProperty(name = "tenant.cluster", defaultValue = "tripico-cluster")
    String cluster;

    /**
     * Create a new tenant and trigger provisioning workflows.
     * Steps:
     * 1. Allocate next available tenant number
     * 2. Create tenant record in MongoDB
     * 3. Trigger deploy-multi-namespace workflow (creates namespace, deploys services, creates Identity Platform tenant, creates Firestore DB)
     * 4. Trigger deploy-shared-services workflow (ensures shared services are available)
     */
    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        LOG.infof("📝 Creating new tenant: %s", request.getName());

        // Generate tenant ID from name
        String tenantId = TenantIdGenerator.generateTenantId(request.getName());
        LOG.infof("Generated tenant ID: %s", tenantId);

        // Check if tenant with same ID already exists
        Tenant existing = Tenant.findByTenantId(tenantId);
        if (existing != null) {
            LOG.warnf("Tenant with ID %s already exists", tenantId);
            throw new IllegalArgumentException("A tenant with this name already exists");
        }

        // Allocate next tenant number
        Integer tenantNumber = allocateNextTenantNumber();
        LOG.infof("Allocated tenant number: %d", tenantNumber);

        // Build namespace and domain
        String namespace = "standard-" + tenantNumber;
        String frontendDomain = String.format("frontend-standard-%d.%s", tenantNumber, baseDomain);

        // Create tenant entity
        Tenant tenant = new Tenant();
        tenant.name = request.getName();
        tenant.tenantId = tenantId;
        tenant.tenantNumber = tenantNumber;
        tenant.namespace = namespace;
        tenant.frontendDomain = frontendDomain;
        tenant.ownerEmail = request.getOwnerEmail();
        tenant.ownerPasswordHash = hashPassword(request.getOwnerPassword());
        tenant.state = Tenant.ProvisioningState.PENDING;
        tenant.tier = Tenant.TenantTier.STANDARD;
        tenant.firestoreDatabaseId = tenantId;
        tenant.createdAt = LocalDateTime.now();
        tenant.updatedAt = LocalDateTime.now();

        // Persist tenant
        tenantRepository.persist(tenant);
        LOG.infof("✅ Tenant created in database: %s (Number: %d, Namespace: %s)", 
            tenant.name, tenant.tenantNumber, tenant.namespace);

        // Set placeholder IDs (will be created by GitHub workflows)
        tenant.identityPlatformTenantId = tenantId;
        tenant.firestoreDatabaseId = tenantId;

        // Trigger GitHub workflows in sequence:
        // 1. deploy-multi-namespace (creates namespace and deploys services)
        // 2. deploy-shared-services (deploys shared services if needed)
        try {
            // Step 1: Trigger multi-namespace deployment
            String multiNamespaceDispatchId = githubService.triggerTenantDeployment(
                tenantNumber,
                environment,
                cluster
            );

            tenant.state = Tenant.ProvisioningState.PROVISIONING;
            tenant.provisioningDispatchId = multiNamespaceDispatchId;
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);

            LOG.infof("✅ Multi-namespace deployment triggered for tenant: %s (dispatch ID: %s)", 
                tenantId, multiNamespaceDispatchId);

            // Step 2: Trigger shared services deployment
            String sharedServicesDispatchId = githubService.triggerSharedServicesDeployment(
                environment,
                cluster
            );

            LOG.infof("✅ Shared services deployment triggered (dispatch ID: %s)", sharedServicesDispatchId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger deployment workflows for tenant: %s", tenantId);
            tenant.state = Tenant.ProvisioningState.FAILED;
            tenant.errorMessage = "Failed to trigger deployment: " + e.getMessage();
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);
            throw new RuntimeException("Failed to trigger tenant deployment", e);
        }

        return tenant;
    }

    /**
     * Allocate the next available tenant number.
     * Thread-safe allocation using database queries.
     */
    private Integer allocateNextTenantNumber() {
        Integer maxNumber = Tenant.findMaxTenantNumber();
        return maxNumber + 1;
    }

    /**
     * Delete a tenant and trigger cleanup workflow.
     * Steps:
     * 1. Update tenant state to DEPROVISIONING
     * 2. Trigger cleanup-tenant workflow (deletes namespace, Firestore DB, Identity Platform tenant)
     * 3. Update tenant state to DELETED
     */
    @Transactional
    public void deleteTenant(String tenantId) {
        LOG.infof("🗑️ Deleting tenant: %s", tenantId);

        Tenant tenant = Tenant.findByTenantId(tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }

        // Check if tenant is already being deleted
        if (tenant.state == Tenant.ProvisioningState.DEPROVISIONING ||
            tenant.state == Tenant.ProvisioningState.DELETED) {
            throw new IllegalStateException("Tenant is already being deleted");
        }

        // Update state to DEPROVISIONING
        tenant.state = Tenant.ProvisioningState.DEPROVISIONING;
        tenant.deletedAt = LocalDateTime.now();
        tenant.updatedAt = LocalDateTime.now();

        // Trigger namespace cleanup (includes Firestore and Identity Platform deletion)
        try {
            String dispatchId = githubService.triggerTenantCleanup(tenant.namespace);
            tenant.deprovisioningDispatchId = dispatchId;
            tenantRepository.update(tenant);

            LOG.infof("✅ Cleanup triggered for tenant: %s (dispatch ID: %s)", tenantId, dispatchId);
            LOG.infof("ℹ️ Cleanup workflow will handle: namespace, Firestore DB, Identity Platform tenant");

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger cleanup workflow");
            tenant.state = Tenant.ProvisioningState.FAILED;
            tenant.errorMessage = "Failed to trigger cleanup: " + e.getMessage();
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);
            throw new RuntimeException("Failed to trigger tenant cleanup", e);
        }

        // Update state to DELETED
        tenant.state = Tenant.ProvisioningState.DELETED;
        tenant.updatedAt = LocalDateTime.now();
        tenantRepository.update(tenant);

        LOG.infof("✅ Tenant deleted: %s", tenantId);

        // Send deletion email
        emailService.sendTenantDeletionEmail(tenant.ownerEmail, tenant.name);
    }

    /**
     * Get a tenant by tenant ID.
     */
    public Optional<Tenant> getTenant(String tenantId) {
        return Optional.ofNullable(Tenant.findByTenantId(tenantId));
    }

    /**
     * Get all tenants.
     */
    public List<TenantResponse> getAllTenants() {
        return tenantRepository.listAll().stream()
            .map(TenantResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get tenants by state.
     */
    public List<Tenant> getTenantsByState(Tenant.ProvisioningState state) {
        return tenantRepository.list("state", state);
    }

    /**
     * Update tenant state after polling workflow status.
     */
    @Transactional
    public void updateTenantState(Tenant tenant, Tenant.ProvisioningState newState, String errorMessage) {
        tenant.state = newState;
        tenant.errorMessage = errorMessage;
        tenant.updatedAt = LocalDateTime.now();

        if (newState == Tenant.ProvisioningState.ACTIVE) {
            tenant.activatedAt = LocalDateTime.now();
            // Clear password hash after successful provisioning
            tenant.ownerPasswordHash = null;
        }

        tenantRepository.update(tenant);
        LOG.infof("Updated tenant %s state to %s", tenant.tenantId, newState);
    }

    /**
     * Simple password hashing (in production, use BCrypt or similar).
     * For now, just base64 encode for temporary storage.
     */
    private String hashPassword(String password) {
        return java.util.Base64.getEncoder().encodeToString(password.getBytes());
    }
}

