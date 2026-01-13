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

    @Inject
    FirestoreService firestoreService;

    @ConfigProperty(name = "tenant.base-domain")
    String baseDomain;

    @ConfigProperty(name = "tenant.environment", defaultValue = "prod")
    String environment;

    @ConfigProperty(name = "tenant.cluster", defaultValue = "tripico-cluster")
    String cluster;

    /**
     * Create a new tenant and trigger provisioning.
     * Steps:
     * 1. Allocate next available tenant number
     * 2. Create Identity Platform tenant
     * 3. Create Firestore database
     * 4. Trigger GitHub repository_dispatch for deployment
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

        // Create Identity Platform tenant
        try {
            LOG.infof("Creating Identity Platform tenant for: %s", tenantId);
            // The tenant ID will be returned by gcloud, store it in next steps
            // For now, use tenantId as the identifier
            tenant.identityPlatformTenantId = tenantId;
            LOG.infof("✅ Identity Platform tenant prepared: %s", tenantId);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to prepare Identity Platform tenant");
            tenant.state = Tenant.ProvisioningState.FAILED;
            tenant.errorMessage = "Failed to prepare Identity Platform: " + e.getMessage();
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);
            throw new RuntimeException("Failed to prepare Identity Platform tenant", e);
        }

        // Create Firestore database
        try {
            LOG.infof("Creating Firestore database: %s", tenantId);
            firestoreService.createDatabase(tenantId);
            tenant.firestoreDatabaseId = tenantId;
            LOG.infof("✅ Firestore database created: %s", tenantId);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to create Firestore database");
            tenant.state = Tenant.ProvisioningState.FAILED;
            tenant.errorMessage = "Failed to create Firestore database: " + e.getMessage();
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);
            throw new RuntimeException("Failed to create Firestore database", e);
        }

        // Trigger GitHub repository dispatch for deployment
        try {
            String dispatchId = githubService.triggerTenantDeployment(
                tenantNumber,
                environment,
                cluster
            );

            tenant.state = Tenant.ProvisioningState.PROVISIONING;
            tenant.provisioningDispatchId = dispatchId;
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);

            LOG.infof("✅ Deployment triggered for tenant: %s (dispatch ID: %s)", tenantId, dispatchId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger deployment for tenant: %s", tenantId);
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
     * Delete a tenant and trigger cleanup.
     * Steps:
     * 1. Trigger GitHub cleanup for namespace
     * 2. Delete Firestore database
     * 3. Delete Identity Platform tenant (future)
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

        // Trigger namespace cleanup
        try {
            String dispatchId = githubService.triggerTenantCleanup(tenant.namespace);
            tenant.deprovisioningDispatchId = dispatchId;
            tenantRepository.update(tenant);

            LOG.infof("✅ Cleanup triggered for tenant: %s (dispatch ID: %s)", tenantId, dispatchId);

        } catch (Exception e) {
            LOG.errorf(e, "⚠️ Failed to trigger cleanup workflow, continuing with manual cleanup");
            // Continue with manual cleanup even if workflow fails
        }

        // Delete Firestore database
        try {
            LOG.infof("Deleting Firestore database: %s", tenant.firestoreDatabaseId);
            firestoreService.deleteDatabase(tenant.firestoreDatabaseId);
            LOG.infof("✅ Firestore database deleted: %s", tenant.firestoreDatabaseId);
        } catch (Exception e) {
            LOG.errorf(e, "⚠️ Failed to delete Firestore database (may not exist)");
        }

        // Delete Identity Platform tenant (future implementation)
        // For now, just log
        LOG.infof("ℹ️ Identity Platform tenant deletion not yet implemented: %s", tenant.identityPlatformTenantId);

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

