package de.htwg.tenant.service;

import de.htwg.tenant.model.Tenant;
import de.htwg.tenant.repository.TenantRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

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
            emailService.sendTenantFailureEmail(tenant.ownerEmail, tenant.name, errorMessage);

            LOG.infof("Tenant marked as FAILED: %s", tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to mark tenant as failed: %s", tenantId);
            throw new RuntimeException("Failed to mark tenant as failed", e);
        }
    }
}
