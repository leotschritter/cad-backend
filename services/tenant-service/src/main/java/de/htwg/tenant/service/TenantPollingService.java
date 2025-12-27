package de.htwg.tenant.service;

import de.htwg.tenant.client.dto.WorkflowRun;
import de.htwg.tenant.model.Tenant;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Service that polls GitHub Actions workflow status and updates tenant states.
 */
@ApplicationScoped
public class TenantPollingService {

    private static final Logger LOG = Logger.getLogger(TenantPollingService.class);

    @Inject
    TenantService tenantService;

    @Inject
    GitHubService githubService;

    @Inject
    EmailService emailService;

    /**
     * Poll provisioning workflows every 30 seconds (configurable).
     */
    @Scheduled(every = "{tenant.polling.interval}s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void pollProvisioningTenants() {
        List<Tenant> provisioningTenants = tenantService.getTenantsByState(Tenant.ProvisioningState.PROVISIONING);

        if (provisioningTenants.isEmpty()) {
            return;
        }

        LOG.infof("🔍 Polling %d provisioning tenant(s)", provisioningTenants.size());

        for (Tenant tenant : provisioningTenants) {
            try {
                pollTenantProvisioningStatus(tenant);
            } catch (Exception e) {
                LOG.errorf(e, "❌ Error polling tenant %s: %s", tenant.tenantId, e.getMessage());
            }
        }
    }

    /**
     * Poll deprovisioning workflows every 30 seconds (configurable).
     */
    @Scheduled(every = "{tenant.polling.interval}s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void pollDeprovisioningTenants() {
        List<Tenant> deprovisioningTenants = tenantService.getTenantsByState(Tenant.ProvisioningState.DEPROVISIONING);

        if (deprovisioningTenants.isEmpty()) {
            return;
        }

        LOG.infof("🔍 Polling %d deprovisioning tenant(s)", deprovisioningTenants.size());

        for (Tenant tenant : deprovisioningTenants) {
            try {
                pollTenantDeprovisioningStatus(tenant);
            } catch (Exception e) {
                LOG.errorf(e, "❌ Error polling tenant %s: %s", tenant.tenantId, e.getMessage());
            }
        }
    }

    private void pollTenantProvisioningStatus(Tenant tenant) {
        if (tenant.provisioningWorkflowRunId == null) {
            LOG.warnf("⚠️ Tenant %s has no workflow run ID, skipping", tenant.tenantId);
            return;
        }

        Optional<WorkflowRun> workflowRun = githubService.getWorkflowRun(tenant.provisioningWorkflowRunId);

        if (workflowRun.isEmpty()) {
            LOG.warnf("⚠️ Could not fetch workflow run %d for tenant %s", 
                tenant.provisioningWorkflowRunId, tenant.tenantId);
            return;
        }

        WorkflowRun run = workflowRun.get();
        LOG.debugf("Workflow run %d status: %s, conclusion: %s", 
            run.getId(), run.getStatus(), run.getConclusion());

        // Check if workflow is completed
        if ("completed".equalsIgnoreCase(run.getStatus())) {
            handleProvisioningCompleted(tenant, run);
        }
    }

    private void pollTenantDeprovisioningStatus(Tenant tenant) {
        if (tenant.deprovisioningWorkflowRunId == null) {
            LOG.warnf("⚠️ Tenant %s has no deprovisioning workflow run ID, skipping", tenant.tenantId);
            return;
        }

        Optional<WorkflowRun> workflowRun = githubService.getWorkflowRun(tenant.deprovisioningWorkflowRunId);

        if (workflowRun.isEmpty()) {
            LOG.warnf("⚠️ Could not fetch workflow run %d for tenant %s", 
                tenant.deprovisioningWorkflowRunId, tenant.tenantId);
            return;
        }

        WorkflowRun run = workflowRun.get();
        LOG.debugf("Deprovisioning workflow run %d status: %s, conclusion: %s", 
            run.getId(), run.getStatus(), run.getConclusion());

        // Check if workflow is completed
        if ("completed".equalsIgnoreCase(run.getStatus())) {
            handleDeprovisioningCompleted(tenant, run);
        }
    }

    private void handleProvisioningCompleted(Tenant tenant, WorkflowRun run) {
        String conclusion = run.getConclusion();

        if ("success".equalsIgnoreCase(conclusion)) {
            LOG.infof("✅ Provisioning succeeded for tenant: %s", tenant.tenantId);
            tenantService.updateTenantState(tenant, Tenant.ProvisioningState.ACTIVE, null);

            // Send success email
            emailService.sendTenantActivationEmail(
                tenant.ownerEmail,
                tenant.name,
                tenant.subdomain,
                tenant.tenantId
            );

        } else {
            LOG.errorf("❌ Provisioning failed for tenant: %s (conclusion: %s)", 
                tenant.tenantId, conclusion);
            
            String errorMessage = String.format("GitHub workflow failed with conclusion: %s. " +
                "Check workflow run: %s", conclusion, run.getHtmlUrl());
            
            tenantService.updateTenantState(tenant, Tenant.ProvisioningState.FAILED, errorMessage);

            // Send failure email
            emailService.sendTenantProvisioningFailedEmail(
                tenant.ownerEmail,
                tenant.name,
                errorMessage
            );
        }
    }

    private void handleDeprovisioningCompleted(Tenant tenant, WorkflowRun run) {
        String conclusion = run.getConclusion();

        if ("success".equalsIgnoreCase(conclusion)) {
            LOG.infof("✅ Deprovisioning succeeded for tenant: %s", tenant.tenantId);
            tenantService.updateTenantState(tenant, Tenant.ProvisioningState.DELETED, null);

            // Send deletion confirmation email
            emailService.sendTenantDeletionEmail(
                tenant.ownerEmail,
                tenant.name
            );

        } else {
            LOG.errorf("❌ Deprovisioning failed for tenant: %s (conclusion: %s)", 
                tenant.tenantId, conclusion);
            
            String errorMessage = String.format("GitHub workflow failed with conclusion: %s. " +
                "Check workflow run: %s", conclusion, run.getHtmlUrl());
            
            // Keep in DEPROVISIONING state but log error
            tenantService.updateTenantState(tenant, Tenant.ProvisioningState.DEPROVISIONING, errorMessage);
        }
    }
}

