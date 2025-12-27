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

    @ConfigProperty(name = "github.workflow.provision")
    String provisionWorkflow;

    @ConfigProperty(name = "github.workflow.deprovision")
    String deprovisionWorkflow;

    /**
     * Create a new tenant and trigger provisioning workflow.
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

        // Create tenant entity
        Tenant tenant = new Tenant();
        tenant.name = request.getName();
        tenant.tenantId = tenantId;
        tenant.subdomain = TenantIdGenerator.generateSubdomain(tenantId, baseDomain);
        tenant.namespace = TenantIdGenerator.generateNamespace(tenantId);
        tenant.firestoreDatabaseId = tenantId; // Firestore DB name = tenant ID
        tenant.ownerEmail = request.getOwnerEmail();
        tenant.ownerPasswordHash = hashPassword(request.getOwnerPassword()); // TODO: Use proper hashing
        tenant.state = Tenant.ProvisioningState.PENDING;
        tenant.tier = Tenant.TenantTier.STANDARD;
        tenant.createdAt = LocalDateTime.now();
        tenant.updatedAt = LocalDateTime.now();

        // Persist tenant
        tenantRepository.persist(tenant);
        LOG.infof("✅ Tenant created in database: %s (ID: %s)", tenant.name, tenant.id);

        // Trigger GitHub Actions workflow
        try {
            githubService.triggerProvisionWorkflow(
                tenant.tenantId,
                tenant.namespace,
                tenant.subdomain,
                tenant.ownerEmail
            );

            // Update state to PROVISIONING
            tenant.state = Tenant.ProvisioningState.PROVISIONING;
            tenant.updatedAt = LocalDateTime.now();

            // Try to get the workflow run ID (best effort)
            try {
                Thread.sleep(2000); // Wait a bit for GitHub to create the run
                Optional<WorkflowRun> latestRun = githubService.getLatestWorkflowRun(provisionWorkflow);
                latestRun.ifPresent(run -> {
                    tenant.provisioningWorkflowRunId = run.getId();
                    LOG.infof("📋 Tracked workflow run ID: %d", run.getId());
                });
            } catch (Exception e) {
                LOG.warnf("Could not retrieve workflow run ID: %s", e.getMessage());
            }

            tenantRepository.update(tenant);
            LOG.infof("✅ Provisioning workflow triggered for tenant: %s", tenant.tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger provisioning workflow for tenant: %s", tenant.tenantId);
            tenant.state = Tenant.ProvisioningState.FAILED;
            tenant.errorMessage = "Failed to trigger provisioning workflow: " + e.getMessage();
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);
            throw new RuntimeException("Failed to trigger tenant provisioning", e);
        }

        return tenant;
    }

    /**
     * Delete a tenant and trigger deprovisioning workflow.
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

        // Trigger GitHub Actions workflow
        try {
            githubService.triggerDeprovisionWorkflow(
                tenant.tenantId,
                tenant.namespace
            );

            // Try to get the workflow run ID (best effort)
            try {
                Thread.sleep(2000); // Wait a bit for GitHub to create the run
                Optional<WorkflowRun> latestRun = githubService.getLatestWorkflowRun(deprovisionWorkflow);
                latestRun.ifPresent(run -> {
                    tenant.deprovisioningWorkflowRunId = run.getId();
                    LOG.infof("📋 Tracked workflow run ID: %d", run.getId());
                });
            } catch (Exception e) {
                LOG.warnf("Could not retrieve workflow run ID: %s", e.getMessage());
            }

            tenantRepository.update(tenant);
            LOG.infof("✅ Deprovisioning workflow triggered for tenant: %s", tenant.tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger deprovisioning workflow for tenant: %s", tenant.tenantId);
            tenant.errorMessage = "Failed to trigger deprovisioning workflow: " + e.getMessage();
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);
            throw new RuntimeException("Failed to trigger tenant deprovisioning", e);
        }
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

