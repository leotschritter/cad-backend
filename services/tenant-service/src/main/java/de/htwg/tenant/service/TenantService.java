package de.htwg.tenant.service;

import de.htwg.tenant.dto.CreateTenantRequest;
import de.htwg.tenant.dto.TenantResponse;
import de.htwg.tenant.model.Tenant;
import de.htwg.tenant.repository.TenantRepository;
import de.htwg.tenant.util.TenantIdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    IdentityPlatformService identityPlatformService;

    @ConfigProperty(name = "tenant.base-domain")
    String baseDomain;

    @ConfigProperty(name = "tenant.environment", defaultValue = "prod")
    String environment;

    @ConfigProperty(name = "tenant.cluster", defaultValue = "tripico-cluster")
    String cluster;

    /**
     * Create a new tenant and trigger provisioning workflows.
     * Supports both STANDARD and ENTERPRISE tiers.
     * 
     * Steps:
     * 1. Determine tier and allocate identifiers (number for STANDARD, name for ENTERPRISE)
     * 2. Create tenant record in MongoDB
     * 3. Trigger deploy-multi-namespace workflow (creates namespace, deploys services, creates Identity Platform tenant, creates Firestore DB)
     * 4. Trigger deploy-shared-services workflow (ensures shared services are available)
     * 
     * Note: @Transactional removed - MongoDB standalone mode doesn't support transactions
     */
    public Tenant createTenant(CreateTenantRequest request) {
        LOG.infof("📝 Creating new %s tier tenant: %s", request.getTier(), request.getName());

        // Determine tier
        Tenant.TenantTier tier = "ENTERPRISE".equals(request.getTier()) 
            ? Tenant.TenantTier.ENTERPRISE 
            : Tenant.TenantTier.STANDARD;

        // Generate tenant ID from name
        String tenantId = TenantIdGenerator.generateTenantId(request.getName());
        LOG.infof("Generated tenant ID: %s", tenantId);

        // Check if tenant with same ID already exists
        Tenant existing = Tenant.findByTenantId(tenantId);
        if (existing != null) {
            LOG.warnf("Tenant with ID %s already exists", tenantId);
            throw new IllegalArgumentException("A tenant with this name already exists");
        }

        // Create tenant entity based on tier
        Tenant tenant = new Tenant();
        tenant.name = request.getName();
        tenant.tenantId = tenantId;
        tenant.tier = tier;
        tenant.ownerEmail = request.getOwnerEmail();
        tenant.ownerPasswordHash = hashPassword(request.getOwnerPassword());
        tenant.state = Tenant.ProvisioningState.PENDING;
        tenant.firestoreDatabaseId = tenantId;
        tenant.createdAt = LocalDateTime.now();
        tenant.updatedAt = LocalDateTime.now();

        if (tier == Tenant.TenantTier.ENTERPRISE) {
            // Enterprise tier: use provided enterprise name
            if (request.getEnterpriseName() == null || request.getEnterpriseName().isEmpty()) {
                throw new IllegalArgumentException("Enterprise name is required for ENTERPRISE tier");
            }
            
            String enterpriseName = request.getEnterpriseName().toLowerCase();
            tenant.enterpriseName = enterpriseName;
            tenant.namespace = "enterprise-" + enterpriseName;
            tenant.clusterName = "tripico-" + enterpriseName + "-cluster";
            tenant.frontendDomain = String.format("frontend.%s.%s", enterpriseName, baseDomain);
            tenant.apiGatewayUrl = String.format("https://api.%s.%s", enterpriseName, baseDomain);
            
            LOG.infof("Enterprise tenant config - Namespace: %s, Cluster: %s, Domain: %s", 
                tenant.namespace, tenant.clusterName, tenant.frontendDomain);
        } else {
            // Standard tier: allocate next tenant number
            Integer tenantNumber = allocateNextTenantNumber();
            LOG.infof("Allocated tenant number: %d", tenantNumber);
            
            tenant.tenantNumber = tenantNumber;
            tenant.namespace = "standard-" + tenantNumber;
            tenant.clusterName = cluster; // Use shared cluster from config
            tenant.frontendDomain = String.format("frontend-standard-%d.%s", tenantNumber, baseDomain);
            tenant.apiGatewayUrl = buildApiGatewayUrl(tenantNumber);
            
            LOG.infof("Standard tenant config - Namespace: %s, Cluster: %s, Domain: %s", 
                tenant.namespace, tenant.clusterName, tenant.frontendDomain);
        }

        // Persist tenant
        tenantRepository.persist(tenant);
        LOG.infof("✅ Tenant created in database: %s (Number: %d, Namespace: %s)", 
            tenant.name, tenant.tenantNumber, tenant.namespace);

        // Set placeholder IDs (will be created by GitHub workflows)
        tenant.identityPlatformTenantId = tenantId;
        tenant.firestoreDatabaseId = tenantId;

        // Trigger GitHub workflows in sequence:
        // 1. terraform.yml (provisions infrastructure: GCP resources, API Gateway, Identity Platform tenant)
        // 2. deploy-multi-namespace (creates namespace and deploys services) - triggered by WorkflowMonitoringService after Terraform completes
        // 3. deploy-shared-services (deploys shared services if needed) - already handled in deploy-multi-namespace
        // 4. deploy-frontend-multi-namespace (deploys frontend) - triggered by WorkflowMonitoringService after backend deployment completes
        try {
            String terraformDispatchId;
            
            if (tier == Tenant.TenantTier.ENTERPRISE) {
                // Step 1: Trigger Terraform for enterprise tier
                terraformDispatchId = githubService.triggerTerraformWorkflow(
                    "enterprise",
                    tenant.tenantNumber,
                    tenant.enterpriseName
                );
                
                LOG.infof("✅ Terraform workflow triggered for enterprise tenant: %s (dispatch ID: %s)", 
                    tenantId, terraformDispatchId);
            } else {
                // Step 1: Trigger Terraform for standard tier
                terraformDispatchId = githubService.triggerTerraformWorkflow(
                    "standard",
                    tenant.tenantNumber,
                    null
                );
                
                LOG.infof("✅ Terraform workflow triggered for standard tenant: %s (dispatch ID: %s)", 
                    tenantId, terraformDispatchId);
            }

            tenant.state = Tenant.ProvisioningState.TERRAFORM_PROVISIONING;
            tenant.terraformDispatchId = terraformDispatchId;
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger Terraform workflow for tenant: %s", tenantId);
            tenant.state = Tenant.ProvisioningState.FAILED;
            tenant.errorMessage = "Failed to trigger Terraform: " + e.getMessage();
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);
            throw new RuntimeException("Failed to trigger Terraform workflow", e);
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
     * 
     * Note: @Transactional removed - MongoDB standalone mode doesn't support transactions
     */
    public void deleteTenant(String tenantId) {
        LOG.infof("🗑️ Deleting tenant: %s", tenantId);

        Tenant tenant = Tenant.findByTenantId(tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }

        // Check if tenant is already being deleted
        if (tenant.state == Tenant.ProvisioningState.DEPROVISIONING ||
            tenant.state == Tenant.ProvisioningState.TERRAFORM_DESTROYING ||
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

        // Note: The rest of the deletion flow (Terraform destroy, marking as DELETED, sending email)
        // is handled by WorkflowMonitoringService after Kubernetes cleanup completes
    }

    /**
     * Complete tenant provisioning after backend deployment is successful.
     * This method should be called after the backend GitHub workflow completes.
     * It will:
     * 1. Add the owner user to the Identity Platform tenant
     * 2. Trigger the frontend deployment
     *
     * @param tenant The tenant that was successfully provisioned
     * 
     * Note: @Transactional removed - MongoDB standalone mode doesn't support transactions
     */
    public void completeTenantProvisioning(Tenant tenant) {
        try {
            LOG.infof("🎉 Completing provisioning for tenant: %s", tenant.tenantId);
            LOG.infof("   Identity Platform Tenant ID: %s", tenant.identityPlatformTenantId);
            LOG.infof("   API Gateway URL: %s", tenant.apiGatewayUrl);

            // Validate required fields
            if (tenant.identityPlatformTenantId == null || tenant.identityPlatformTenantId.isEmpty()) {
                LOG.errorf("❌ Identity Platform Tenant ID is missing for tenant: %s", tenant.tenantId);
                LOG.errorf("   This usually means Terraform outputs were not retrieved successfully.");
                throw new IllegalStateException("Identity Platform Tenant ID is not set for tenant: " + tenant.tenantId + 
                    ". Terraform outputs may not have been retrieved.");
            }
            
            if (tenant.apiGatewayUrl == null || tenant.apiGatewayUrl.isEmpty()) {
                LOG.errorf("❌ API Gateway URL is missing for tenant: %s", tenant.tenantId);
                throw new IllegalStateException("API Gateway URL is not set for tenant: " + tenant.tenantId);
            }
            
            if (tenant.ownerPasswordHash == null || tenant.ownerPasswordHash.isEmpty()) {
                LOG.errorf("❌ Owner password hash is missing for tenant: %s", tenant.tenantId);
                throw new IllegalStateException("Owner password hash is not set for tenant: " + tenant.tenantId);
            }

            // Decode the password from base64 (temporary storage)
            String password = new String(java.util.Base64.getDecoder().decode(tenant.ownerPasswordHash));

            // Step 1: Add owner user to Identity Platform tenant
            LOG.infof("👤 Adding owner user to Identity Platform tenant: %s (tenant: %s)", 
                tenant.ownerEmail, tenant.identityPlatformTenantId);
            String ownerUid = identityPlatformService.addUserToTenant(
                tenant.identityPlatformTenantId,
                tenant.ownerEmail,
                password
            );
            
            tenant.ownerUid = ownerUid;
        tenant.updatedAt = LocalDateTime.now();
        tenantRepository.update(tenant);

            LOG.infof("✅ Owner user added to Identity Platform: %s (UID: %s)", 
                tenant.ownerEmail, ownerUid);

            // Step 2: Trigger frontend deployment
            LOG.infof("🚀 Triggering frontend deployment for tenant: %s", tenant.tenantId);
            String frontendDispatchId;
            
            if (tenant.tier == Tenant.TenantTier.ENTERPRISE) {
                frontendDispatchId = githubService.triggerEnterpriseFrontendDeployment(
                    tenant.enterpriseName,
                    environment,
                    tenant.apiGatewayUrl,
                    tenant.identityPlatformTenantId
                );
            } else {
                frontendDispatchId = githubService.triggerFrontendDeployment(
                    tenant.tenantNumber,
                    environment,
                    tenant.apiGatewayUrl,
                    tenant.identityPlatformTenantId
                );
            }

            tenant.frontendDeploymentDispatchId = frontendDispatchId;
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);

            LOG.infof("✅ Frontend deployment triggered successfully");

            // Clear the password hash now that user is created
            tenant.ownerPasswordHash = null;
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);

            LOG.infof("🎉 Tenant provisioning completed successfully: %s", tenant.tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to complete tenant provisioning: %s", tenant.tenantId);
            tenant.errorMessage = "Failed to complete provisioning: " + e.getMessage();
            tenant.updatedAt = LocalDateTime.now();
            tenantRepository.update(tenant);
            throw new RuntimeException("Failed to complete tenant provisioning", e);
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
     * 
     * Note: @Transactional removed - MongoDB standalone mode doesn't support transactions
     */
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

    /**
     * Build the API Gateway URL for a tenant based on tenant number.
     * 
     * @param tenantNumber The tenant number
     * @return The API Gateway URL (e.g., "https://api-standard-1.tripico.fun")
     */
    private String buildApiGatewayUrl(Integer tenantNumber) {
        // For development environment
        if ("dev".equalsIgnoreCase(environment)) {
            return String.format("https://api-standard-%d.dev.%s", tenantNumber, baseDomain);
        }
        // For production environment
        return String.format("https://api-standard-%d.%s", tenantNumber, baseDomain);
    }
}

