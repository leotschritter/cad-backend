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
                LOG.errorf("❌ Could not retrieve Terraform outputs for tenant: %s", tenantName);
                LOG.errorf("   Cannot proceed with Kubernetes deployment without Terraform outputs");
                LOG.errorf("   Current API Gateway URL: %s", tenant.apiGatewayUrl);
                LOG.errorf("   Current Identity Platform Tenant ID: %s", tenant.identityPlatformTenantId);
                throw new IllegalStateException("Terraform outputs not available for tenant: " + tenantName + 
                    ". Cannot proceed with deployment without Identity Platform tenant ID.");
            }
            
            // Validate that we have the required values from Terraform
            if (tenant.identityPlatformTenantId == null || tenant.identityPlatformTenantId.isEmpty()) {
                LOG.errorf("❌ Identity Platform Tenant ID is missing after retrieving Terraform outputs");
                throw new IllegalStateException("Identity Platform Tenant ID is not set from Terraform outputs for tenant: " + tenantName);
            }
            
            if (tenant.apiGatewayUrl == null || tenant.apiGatewayUrl.isEmpty()) {
                LOG.errorf("❌ API Gateway URL is missing after retrieving Terraform outputs");
                throw new IllegalStateException("API Gateway URL is not set from Terraform outputs for tenant: " + tenantName);
            }
            
            // Verify that the Identity Platform tenant ID is not the placeholder value
            // The placeholder is the original tenantId (e.g., "stdtest7"), while the real ID is from Terraform (e.g., "std-standard-7-7wt9c")
            if (tenant.identityPlatformTenantId.equals(tenant.tenantId)) {
                LOG.errorf("❌ Identity Platform Tenant ID appears to be placeholder value: %s", tenant.identityPlatformTenantId);
                LOG.errorf("   Expected format: std-standard-{number}-{suffix} or enterprise-{name}-{suffix}");
                throw new IllegalStateException("Identity Platform Tenant ID is still placeholder value. " +
                    "Terraform outputs may not have been retrieved correctly for tenant: " + tenantName);
            }
            
            LOG.infof("✅ Validated Terraform outputs - Identity Platform Tenant ID: %s, API Gateway URL: %s", 
                tenant.identityPlatformTenantId, tenant.apiGatewayUrl);
            
            String multiNamespaceDispatchId;
            
            if (tenant.tier == Tenant.TenantTier.ENTERPRISE) {
                // Trigger enterprise deployment
                LOG.infof("🚀 Triggering enterprise deployment with Identity Platform Tenant ID: %s", 
                    tenant.identityPlatformTenantId);
                multiNamespaceDispatchId = githubService.triggerEnterpriseDeployment(
                    tenant.enterpriseName,
                    environment,
                    tenant.clusterName,
                    tenant.identityPlatformTenantId
                );
                
                LOG.infof("✅ Enterprise deployment triggered for tenant: %s (dispatch ID: %s)", 
                    tenant.tenantId, multiNamespaceDispatchId);
                LOG.infof("   Using Identity Platform Tenant ID: %s", tenant.identityPlatformTenantId);
                
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
                LOG.infof("🚀 Triggering standard deployment with Identity Platform Tenant ID: %s", 
                    tenant.identityPlatformTenantId);
                multiNamespaceDispatchId = githubService.triggerTenantDeployment(
                    tenant.tenantNumber,
                    environment,
                    tenant.clusterName,
                    tenant.identityPlatformTenantId
                );
                
                LOG.infof("✅ Standard deployment triggered for tenant: %s (dispatch ID: %s)", 
                    tenant.tenantId, multiNamespaceDispatchId);
                LOG.infof("   Using Identity Platform Tenant ID: %s", tenant.identityPlatformTenantId);
                
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
     * When backend deployment completes successfully, trigger frontend deployment and user creation.
     * This runs every 60 seconds.
     */
    @Scheduled(every = "60s", delay = 5)
    void checkProvisioningTenants() {
        try {
            List<Tenant> provisioningTenants = tenantService.getTenantsByState(Tenant.ProvisioningState.PROVISIONING);
            
            if (!provisioningTenants.isEmpty()) {
                LOG.infof("🔍 Checking status of %d tenants in PROVISIONING state", provisioningTenants.size());
                
                for (Tenant tenant : provisioningTenants) {
                    LOG.debugf("Checking backend deployment status for tenant %s", tenant.tenantId);
                    
                    // Query GitHub API for deploy-multi-namespace workflow status
                    Optional<WorkflowRun> latestRun = githubService.getLatestDeploymentRun();
                    
                    if (latestRun.isPresent()) {
                        WorkflowRun run = latestRun.get();
                        String status = run.getStatus();
                        String conclusion = run.getConclusion();
                        
                        LOG.debugf("Latest deployment run status: %s, conclusion: %s", status, conclusion);
                        
                        if ("completed".equals(status)) {
                            if ("success".equals(conclusion)) {
                                LOG.infof("✅ Backend deployment completed successfully for tenant: %s", tenant.tenantId);
                                
                                // Refresh tenant from database to ensure we have latest Terraform outputs
                                Tenant refreshedTenant = Tenant.findByTenantId(tenant.tenantId);
                                if (refreshedTenant == null) {
                                    LOG.errorf("❌ Tenant not found in database: %s", tenant.tenantId);
                                    continue;
                                }
                                
                                LOG.infof("   Refreshed tenant - Identity Platform Tenant ID: %s", refreshedTenant.identityPlatformTenantId);
                                LOG.infof("   Refreshed tenant - API Gateway URL: %s", refreshedTenant.apiGatewayUrl);
                                
                                // Complete provisioning: add user to Identity Platform and deploy frontend
                                try {
                                    tenantService.completeTenantProvisioning(refreshedTenant);
                                    
                                    // Update state to ACTIVE
                                    tenantService.updateTenantState(refreshedTenant, Tenant.ProvisioningState.ACTIVE, null);
                                    
                                    // Send activation email
                                    emailService.sendTenantActivationEmail(
                                        refreshedTenant.ownerEmail,
                                        refreshedTenant.name,
                                        refreshedTenant.frontendDomain,
                                        refreshedTenant.tenantId
                                    );
                                    
                                    LOG.infof("✅ Tenant activated successfully: %s", refreshedTenant.tenantId);
                                    
                                } catch (Exception e) {
                                    LOG.errorf(e, "❌ Failed to complete provisioning for tenant: %s", refreshedTenant.tenantId);
                                    String errorMessage = "Failed to complete provisioning: " + e.getMessage();
                                    tenantService.updateTenantState(refreshedTenant, Tenant.ProvisioningState.FAILED, errorMessage);
                                    emailService.sendTenantProvisioningFailedEmail(refreshedTenant.ownerEmail, refreshedTenant.name, errorMessage);
                                }
                                
                            } else {
                                LOG.errorf("❌ Backend deployment failed for tenant %s: %s", tenant.tenantId, conclusion);
                                String errorMessage = String.format("Backend deployment failed with status: %s", conclusion);
                                tenantService.updateTenantState(tenant, Tenant.ProvisioningState.FAILED, errorMessage);
                                emailService.sendTenantProvisioningFailedEmail(tenant.ownerEmail, tenant.name, errorMessage);
                            }
                        }
                    } else {
                        // If we can't find workflow status, check if tenant has been waiting too long
                        java.time.Duration timeSinceCreation = java.time.Duration.between(
                            tenant.createdAt, 
                            java.time.LocalDateTime.now()
                        );
                        
                        // If tenant has been in PROVISIONING for > 30 minutes, assume deployment is done
                        if (timeSinceCreation.toMinutes() >= 30) {
                            LOG.warnf("⏰ Backend deployment timeout for tenant %s (%d minutes), assuming deployment completed",
                                tenant.tenantId, timeSinceCreation.toMinutes());
                            
                            // Refresh tenant from database to ensure we have latest Terraform outputs
                            Tenant refreshedTenant = Tenant.findByTenantId(tenant.tenantId);
                            if (refreshedTenant == null) {
                                LOG.errorf("❌ Tenant not found in database: %s", tenant.tenantId);
                                continue;
                            }
                            
                            try {
                                tenantService.completeTenantProvisioning(refreshedTenant);
                                tenantService.updateTenantState(refreshedTenant, Tenant.ProvisioningState.ACTIVE, null);
                                emailService.sendTenantActivationEmail(
                                    refreshedTenant.ownerEmail,
                                    refreshedTenant.name,
                                    refreshedTenant.frontendDomain,
                                    refreshedTenant.tenantId
                                );
                            } catch (Exception e) {
                                LOG.errorf(e, "❌ Failed to complete provisioning after timeout for tenant: %s", refreshedTenant.tenantId);
                            }
                        }
                    }
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
                    
                    // Check if Terraform destroy has already been triggered
                    if (tenant.terraformDestroyDispatchId != null && !tenant.terraformDestroyDispatchId.isEmpty()) {
                        LOG.debugf("Terraform destroy already triggered for tenant %s (dispatch ID: %s), skipping", 
                            tenant.tenantId, tenant.terraformDestroyDispatchId);
                        continue;
                    }
                    
                    // Check if tenant is already in TERRAFORM_DESTROYING state (shouldn't happen, but safety check)
                    if (tenant.state == Tenant.ProvisioningState.TERRAFORM_DESTROYING) {
                        LOG.debugf("Tenant %s is already in TERRAFORM_DESTROYING state, skipping", tenant.tenantId);
                        continue;
                    }
                    
                    // Query GitHub API for cleanup-tenant workflow status
                    // The cleanup workflow is triggered via repository_dispatch with event type "cleanup-tenant"
                    // We need to check the latest workflow runs to see if cleanup completed
                    
                    // For now, we can check if the workflow completed by querying recent workflow runs
                    // In a production system, you'd match the dispatchId to a specific run
                    
                    // As a simplified approach: if the tenant has been in DEPROVISIONING for more than 5 minutes,
                    // assume cleanup is done and trigger Terraform destroy
                    // This is because the cleanup workflow is typically fast (< 2 minutes)
                    
                    java.time.Duration timeSinceDeletion = java.time.Duration.between(
                        tenant.deletedAt, 
                        java.time.LocalDateTime.now()
                    );
                    
                    if (timeSinceDeletion.toMinutes() >= 5) {
                        LOG.infof("⏰ Kubernetes cleanup period expired for tenant %s (%d minutes), triggering Terraform destroy", 
                            tenant.tenantId, timeSinceDeletion.toMinutes());
                        triggerTerraformDestroyManually(tenant);
                    } else {
                        LOG.debugf("Waiting for Kubernetes cleanup to complete for tenant %s (elapsed: %d minutes)", 
                            tenant.tenantId, timeSinceDeletion.toMinutes());
                    }
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
     * Can also be called manually via REST API.
     */
    public void triggerTerraformDestroyManually(Tenant tenant) {
        try {
            // Refresh tenant from database to get latest state
            Tenant refreshedTenant = Tenant.findByTenantId(tenant.tenantId);
            if (refreshedTenant == null) {
                throw new IllegalArgumentException("Tenant not found: " + tenant.tenantId);
            }
            
            // Check if Terraform destroy has already been triggered
            if (refreshedTenant.terraformDestroyDispatchId != null && !refreshedTenant.terraformDestroyDispatchId.isEmpty()) {
                LOG.warnf("⚠️ Terraform destroy already triggered for tenant %s (dispatch ID: %s), skipping duplicate trigger", 
                    refreshedTenant.tenantId, refreshedTenant.terraformDestroyDispatchId);
                return;
            }
            
            // Check if tenant is already in TERRAFORM_DESTROYING state
            if (refreshedTenant.state == Tenant.ProvisioningState.TERRAFORM_DESTROYING) {
                LOG.warnf("⚠️ Tenant %s is already in TERRAFORM_DESTROYING state, skipping duplicate trigger", 
                    refreshedTenant.tenantId);
                return;
            }
            
            String tier = refreshedTenant.tier == Tenant.TenantTier.ENTERPRISE ? "enterprise" : "standard";
            String tenantName = refreshedTenant.tier == Tenant.TenantTier.ENTERPRISE 
                ? refreshedTenant.enterpriseName 
                : null;
            
            LOG.infof("🚀 Triggering Terraform destroy for tenant: %s", refreshedTenant.tenantId);
            
            String destroyDispatchId = githubService.triggerTerraformDestroy(
                tier,
                refreshedTenant.tenantNumber,
                tenantName
            );
            
            // Update tenant state to TERRAFORM_DESTROYING
            refreshedTenant.state = Tenant.ProvisioningState.TERRAFORM_DESTROYING;
            refreshedTenant.terraformDestroyDispatchId = destroyDispatchId;
            refreshedTenant.updatedAt = java.time.LocalDateTime.now();
            tenantRepository.update(refreshedTenant);
            
            LOG.infof("✅ Terraform destroy triggered for tenant: %s (dispatch ID: %s)", 
                refreshedTenant.tenantId, destroyDispatchId);
            
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
