package de.htwg.tenant.service;

import de.htwg.tenant.client.dto.WorkflowRun;
import de.htwg.tenant.model.Tenant;
import de.htwg.tenant.repository.TenantRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Service that monitors GitHub workflow status and updates tenant state accordingly.
 * Runs on a scheduled interval to check provisioning status.
 */
@ApplicationScoped
public class WorkflowMonitoringService {

    private static final Logger LOG = Logger.getLogger(WorkflowMonitoringService.class);

    @Inject
    TenantRepository tenantRepository;

    @Inject
    TenantService tenantService;

    @Inject
    EmailService emailService;

    @Inject
    GitHubService githubService;

    @ConfigProperty(name = "tenant.environment", defaultValue = "prod")
    String environment;

    @ConfigProperty(name = "tenant.cluster", defaultValue = "tripico-cluster")
    String cluster;

    /**
     * Poll for tenants in TERRAFORM_PROVISIONING state and check if Terraform has completed.
     * When Terraform completes successfully, trigger the Kubernetes deployment.
     * This runs every 60 seconds.
     */
    @Scheduled(every = "60s", delay = 5)
    void checkTerraformProvisioningTenants() {
        try {
            List<Tenant> terraformTenants = tenantService.getTenantsByState(Tenant.ProvisioningState.TERRAFORM_PROVISIONING);
            
            if (!terraformTenants.isEmpty()) {
                LOG.infof("🔍 Checking status of %d tenants in TERRAFORM_PROVISIONING state", terraformTenants.size());
                
                for (Tenant tenant : terraformTenants) {
                    LOG.debugf("Checking Terraform status for tenant %s", tenant.tenantId);
                    
                    // Query GitHub API for Terraform workflow status
                    Optional<WorkflowRun> latestRun = githubService.getLatestTerraformRun();
                    
                    if (latestRun.isPresent()) {
                        WorkflowRun run = latestRun.get();
                        String status = run.getStatus();
                        String conclusion = run.getConclusion();
                        
                        LOG.debugf("Latest Terraform run status: %s, conclusion: %s", status, conclusion);
                        
                        if ("completed".equals(status)) {
                            if ("success".equals(conclusion)) {
                                LOG.infof("✅ Terraform completed successfully for tenant: %s", tenant.tenantId);
                                triggerKubernetesDeployment(tenant);
                            } else {
                                LOG.errorf("❌ Terraform failed for tenant %s: %s", tenant.tenantId, conclusion);
                                String errorMessage = String.format("Terraform workflow failed with status: %s", conclusion);
                                tenantService.updateTenantState(tenant, Tenant.ProvisioningState.FAILED, errorMessage);
                                emailService.sendTenantProvisioningFailedEmail(tenant.ownerEmail, tenant.name, errorMessage);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error checking Terraform provisioning tenants: %s", e.getMessage());
        }
    }

    /**
     * Trigger Kubernetes deployment after Terraform completes successfully.
     * 
     * First retrieves real values from Terraform outputs and updates the tenant:
     * - api_gateway_url (the actual GCP API Gateway URL)
     * - identity_platform_tenant_id (the actual Identity Platform tenant ID)
     */
    private void triggerKubernetesDeployment(Tenant tenant) {
        try {
            // Retrieve Terraform outputs from GitHub Actions artifacts
            String tenantName = tenant.tier == Tenant.TenantTier.ENTERPRISE 
                ? tenant.enterpriseName 
                : "standard-" + tenant.tenantNumber;
            
            LOG.infof("📥 Retrieving Terraform outputs for: %s", tenantName);
            
            Optional<de.htwg.tenant.client.dto.TerraformOutputs> outputs = 
                githubService.getTerraformOutputs(tenantName);
            
            if (outputs.isPresent()) {
                de.htwg.tenant.client.dto.TerraformOutputs tfOutputs = outputs.get();
                
                // Update tenant with real values from Terraform
                if (tfOutputs.getApiGatewayUrl() != null) {
                    tenant.apiGatewayUrl = tfOutputs.getApiGatewayUrl().getValue();
                    LOG.infof("✅ Updated API Gateway URL: %s", tenant.apiGatewayUrl);
                }
                
                if (tfOutputs.getIdentityPlatformTenantId() != null) {
                    tenant.identityPlatformTenantId = tfOutputs.getIdentityPlatformTenantId().getValue();
                    LOG.infof("✅ Updated Identity Platform Tenant ID: %s", tenant.identityPlatformTenantId);
                }
                
                tenant.updatedAt = java.time.LocalDateTime.now();
                tenantRepository.update(tenant);
                
            } else {
                LOG.warnf("⚠️ Could not retrieve Terraform outputs, using template-based values");
                LOG.infof("   Current API Gateway URL: %s", tenant.apiGatewayUrl);
                LOG.infof("   Current Identity Platform Tenant ID: %s", tenant.identityPlatformTenantId);
            }
            
            String multiNamespaceDispatchId;
            
            if (tenant.tier == Tenant.TenantTier.ENTERPRISE) {
                // Trigger enterprise deployment
                multiNamespaceDispatchId = githubService.triggerEnterpriseDeployment(
                    tenant.enterpriseName,
                    environment,
                    tenant.clusterName,
                    tenant.identityPlatformTenantId
                );
                
                LOG.infof("✅ Enterprise deployment triggered for tenant: %s (dispatch ID: %s)", 
                    tenant.tenantId, multiNamespaceDispatchId);
                
                // Trigger shared services on dedicated cluster
                String sharedServicesDispatchId = githubService.triggerSharedServicesDeployment(
                    environment,
                    tenant.clusterName,
                    tenant.enterpriseName
                );
                
                LOG.infof("✅ Enterprise shared services deployment triggered (dispatch ID: %s)", 
                    sharedServicesDispatchId);
            } else {
                // Trigger standard deployment
                multiNamespaceDispatchId = githubService.triggerTenantDeployment(
                    tenant.tenantNumber,
                    environment,
                    tenant.clusterName,
                    tenant.identityPlatformTenantId
                );
                
                LOG.infof("✅ Standard deployment triggered for tenant: %s (dispatch ID: %s)", 
                    tenant.tenantId, multiNamespaceDispatchId);
                
                // Trigger shared services deployment
                String sharedServicesDispatchId = githubService.triggerSharedServicesDeployment(
                    environment,
                    tenant.clusterName
                );
                
                LOG.infof("✅ Shared services deployment triggered (dispatch ID: %s)", 
                    sharedServicesDispatchId);
            }

            // Update tenant state to PROVISIONING
            tenant.state = Tenant.ProvisioningState.PROVISIONING;
            tenant.provisioningDispatchId = multiNamespaceDispatchId;
            tenant.updatedAt = java.time.LocalDateTime.now();
            tenantRepository.update(tenant);
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger Kubernetes deployment for tenant: %s", tenant.tenantId);
            String errorMessage = "Failed to trigger Kubernetes deployment: " + e.getMessage();
            tenantService.updateTenantState(tenant, Tenant.ProvisioningState.FAILED, errorMessage);
            emailService.sendTenantProvisioningFailedEmail(tenant.ownerEmail, tenant.name, errorMessage);
        }
    }

    /**
     * Poll for tenants in PROVISIONING state and check if their backend deployment has completed.
     * This runs every 60 seconds.
     * 
     * Note: This is a simplified implementation. In a production system, you would:
     * 1. Query GitHub API for workflow run status
     * 2. Match runs to tenants using dispatch IDs
     * 3. Update state based on actual workflow completion
     * 
     * For now, this serves as a placeholder for the manual activation endpoint.
     */
    @Scheduled(every = "60s", delay = 5)
    void checkProvisioningTenants() {
        try {
            List<Tenant> provisioningTenants = tenantService.getTenantsByState(Tenant.ProvisioningState.PROVISIONING);
            
            if (!provisioningTenants.isEmpty()) {
                LOG.infof("🔍 Checking status of %d tenants in PROVISIONING state", provisioningTenants.size());
                
                for (Tenant tenant : provisioningTenants) {
                    LOG.debugf("Tenant %s is in PROVISIONING state (dispatch ID: %s)", 
                        tenant.tenantId, tenant.provisioningDispatchId);
                    
                    // In a full implementation, you would:
                    // 1. Query GitHub API for workflow runs
                    // 2. Find the run matching this tenant's dispatch ID
                    // 3. Check if it's completed (success/failure)
                    // 4. Call activateTenant() or markAsFailed() accordingly
                    
                    // For now, this is handled manually via the REST endpoint
                }
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error checking provisioning tenants: %s", e.getMessage());
        }
    }

    /**
     * Activate a tenant after backend deployment completes successfully.
     * This triggers:
     * 1. User creation in Identity Platform
     * 2. Frontend deployment
     * 3. State update to ACTIVE
     * 4. Activation email
     */
    public void activateTenant(String tenantId) {
        try {
            Tenant tenant = Tenant.findByTenantId(tenantId);
            if (tenant == null) {
                throw new IllegalArgumentException("Tenant not found: " + tenantId);
            }

            if (tenant.state != Tenant.ProvisioningState.PROVISIONING) {
                LOG.warnf("Tenant %s is not in PROVISIONING state (current: %s)", 
                    tenantId, tenant.state);
                throw new IllegalStateException("Tenant is not in PROVISIONING state");
            }

            LOG.infof("🎉 Activating tenant: %s", tenantId);

            // Complete provisioning (adds user to Identity Platform and deploys frontend)
            tenantService.completeTenantProvisioning(tenant);

            // Update state to ACTIVE
            tenantService.updateTenantState(tenant, Tenant.ProvisioningState.ACTIVE, null);

            // Send activation email
            emailService.sendTenantActivationEmail(
                tenant.ownerEmail,
                tenant.name,
                tenant.frontendDomain,
                tenant.tenantId
            );

            LOG.infof("✅ Tenant activated successfully: %s", tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to activate tenant: %s", tenantId);
            throw new RuntimeException("Failed to activate tenant", e);
        }
    }

    /**
     * Mark a tenant as failed if backend deployment fails.
     */
    public void markTenantAsFailed(String tenantId, String errorMessage) {
        try {
            Tenant tenant = Tenant.findByTenantId(tenantId);
            if (tenant == null) {
                throw new IllegalArgumentException("Tenant not found: " + tenantId);
            }

            LOG.warnf("❌ Marking tenant as FAILED: %s - %s", tenantId, errorMessage);

            tenantService.updateTenantState(tenant, Tenant.ProvisioningState.FAILED, errorMessage);

            // Send failure email
            emailService.sendTenantProvisioningFailedEmail(tenant.ownerEmail, tenant.name, errorMessage);

            LOG.infof("Tenant marked as FAILED: %s", tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to mark tenant as failed: %s", tenantId);
            throw new RuntimeException("Failed to mark tenant as failed", e);
        }
    }

    /**
     * Poll for tenants in DEPROVISIONING state and check if Kubernetes cleanup has completed.
     * When cleanup completes successfully, trigger Terraform destroy.
     * This runs every 60 seconds.
     */
    @Scheduled(every = "60s", delay = 10)
    void checkDeprovisioningTenants() {
        try {
            List<Tenant> deprovisioningTenants = tenantService.getTenantsByState(Tenant.ProvisioningState.DEPROVISIONING);
            
            if (!deprovisioningTenants.isEmpty()) {
                LOG.infof("🔍 Checking status of %d tenants in DEPROVISIONING state", deprovisioningTenants.size());
                
                for (Tenant tenant : deprovisioningTenants) {
                    LOG.debugf("Checking Kubernetes cleanup status for tenant %s", tenant.tenantId);
                    
                    // In a full implementation, you would query the cleanup workflow status
                    // For now, this needs to be implemented similar to the provisioning monitoring
                    // After confirming Kubernetes cleanup is complete, trigger Terraform destroy
                    
                    // TODO: Query GitHub API for cleanup workflow status
                    // If cleanup is complete, call triggerTerraformDestroy()
                }
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error checking deprovisioning tenants: %s", e.getMessage());
        }
    }

    /**
     * Poll for tenants in TERRAFORM_DESTROYING state and check if Terraform destroy has completed.
     * When destroy completes successfully, mark tenant as DELETED and send notification.
     * This runs every 60 seconds.
     */
    @Scheduled(every = "60s", delay = 15)
    void checkTerraformDestroyingTenants() {
        try {
            List<Tenant> destroyingTenants = tenantService.getTenantsByState(Tenant.ProvisioningState.TERRAFORM_DESTROYING);
            
            if (!destroyingTenants.isEmpty()) {
                LOG.infof("🔍 Checking status of %d tenants in TERRAFORM_DESTROYING state", destroyingTenants.size());
                
                for (Tenant tenant : destroyingTenants) {
                    LOG.debugf("Checking Terraform destroy status for tenant %s", tenant.tenantId);
                    
                    // Query GitHub API for Terraform workflow status
                    Optional<WorkflowRun> latestRun = githubService.getLatestTerraformRun();
                    
                    if (latestRun.isPresent()) {
                        WorkflowRun run = latestRun.get();
                        String status = run.getStatus();
                        String conclusion = run.getConclusion();
                        
                        LOG.debugf("Latest Terraform run status: %s, conclusion: %s", status, conclusion);
                        
                        if ("completed".equals(status)) {
                            if ("success".equals(conclusion)) {
                                LOG.infof("✅ Terraform destroy completed successfully for tenant: %s", tenant.tenantId);
                                completeTenantDeletion(tenant);
                            } else {
                                LOG.errorf("❌ Terraform destroy failed for tenant %s: %s", tenant.tenantId, conclusion);
                                String errorMessage = String.format("Terraform destroy failed with status: %s", conclusion);
                                tenantService.updateTenantState(tenant, Tenant.ProvisioningState.FAILED, errorMessage);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error checking Terraform destroying tenants: %s", e.getMessage());
        }
    }

    /**
     * Trigger Terraform destroy after Kubernetes cleanup completes.
     */
    private void triggerTerraformDestroy(Tenant tenant) {
        try {
            String tier = tenant.tier == Tenant.TenantTier.ENTERPRISE ? "enterprise" : "standard";
            String tenantName = tenant.tier == Tenant.TenantTier.ENTERPRISE 
                ? tenant.enterpriseName 
                : null;
            
            LOG.infof("🚀 Triggering Terraform destroy for tenant: %s", tenant.tenantId);
            
            String destroyDispatchId = githubService.triggerTerraformDestroy(
                tier,
                tenant.tenantNumber,
                tenantName
            );
            
            // Update tenant state to TERRAFORM_DESTROYING
            tenant.state = Tenant.ProvisioningState.TERRAFORM_DESTROYING;
            tenant.terraformDestroyDispatchId = destroyDispatchId;
            tenant.updatedAt = java.time.LocalDateTime.now();
            tenantRepository.update(tenant);
            
            LOG.infof("✅ Terraform destroy triggered for tenant: %s (dispatch ID: %s)", 
                tenant.tenantId, destroyDispatchId);
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger Terraform destroy for tenant: %s", tenant.tenantId);
            String errorMessage = "Failed to trigger Terraform destroy: " + e.getMessage();
            tenantService.updateTenantState(tenant, Tenant.ProvisioningState.FAILED, errorMessage);
        }
    }

    /**
     * Complete tenant deletion after Terraform destroy completes successfully.
     * Marks tenant as DELETED and sends notification email.
     */
    private void completeTenantDeletion(Tenant tenant) {
        try {
            LOG.infof("✅ Completing deletion for tenant: %s", tenant.tenantId);
            
            // Update state to DELETED
            tenant.state = Tenant.ProvisioningState.DELETED;
            tenant.updatedAt = java.time.LocalDateTime.now();
            tenantRepository.update(tenant);
            
            // Send deletion email
            emailService.sendTenantDeletionEmail(tenant.ownerEmail, tenant.name);
            
            LOG.infof("✅ Tenant deletion completed: %s", tenant.tenantId);
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to complete tenant deletion: %s", tenant.tenantId);
        }
    }
}
