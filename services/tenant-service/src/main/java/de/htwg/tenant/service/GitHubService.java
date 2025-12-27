package de.htwg.tenant.service;

import de.htwg.tenant.client.GitHubActionsClient;
import de.htwg.tenant.client.dto.WorkflowDispatchRequest;
import de.htwg.tenant.client.dto.WorkflowRun;
import de.htwg.tenant.client.dto.WorkflowRunsResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Service for interacting with GitHub Actions.
 */
@ApplicationScoped
public class GitHubService {

    private static final Logger LOG = Logger.getLogger(GitHubService.class);
    private static final String GITHUB_API_VERSION = "2022-11-28";

    @RestClient
    GitHubActionsClient githubClient;

    @ConfigProperty(name = "github.api.token")
    String githubToken;

    @ConfigProperty(name = "github.repository.owner")
    String repoOwner;

    @ConfigProperty(name = "github.repository.name")
    String repoName;

    @ConfigProperty(name = "github.workflow.provision")
    String provisionWorkflow;

    @ConfigProperty(name = "github.workflow.deprovision")
    String deprovisionWorkflow;

    /**
     * Trigger the provision-tenant workflow.
     */
    public void triggerProvisionWorkflow(String tenantId, String namespace, String subdomain, String ownerEmail) {
        try {
            Map<String, String> inputs = Map.of(
                "tenant_id", tenantId,
                "namespace", namespace,
                "subdomain", subdomain,
                "owner_email", ownerEmail
            );

            WorkflowDispatchRequest request = new WorkflowDispatchRequest("main", inputs);

            LOG.infof("🚀 Triggering provision workflow for tenant: %s", tenantId);

            githubClient.dispatchWorkflow(
                repoOwner,
                repoName,
                provisionWorkflow,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Provision workflow triggered successfully for tenant: %s", tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger provision workflow for tenant: %s", tenantId);
            throw new RuntimeException("Failed to trigger provision workflow", e);
        }
    }

    /**
     * Trigger the deprovision-tenant workflow.
     */
    public void triggerDeprovisionWorkflow(String tenantId, String namespace) {
        try {
            Map<String, String> inputs = Map.of(
                "tenant_id", tenantId,
                "namespace", namespace
            );

            WorkflowDispatchRequest request = new WorkflowDispatchRequest("main", inputs);

            LOG.infof("🚀 Triggering deprovision workflow for tenant: %s", tenantId);

            githubClient.dispatchWorkflow(
                repoOwner,
                repoName,
                deprovisionWorkflow,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Deprovision workflow triggered successfully for tenant: %s", tenantId);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger deprovision workflow for tenant: %s", tenantId);
            throw new RuntimeException("Failed to trigger deprovision workflow", e);
        }
    }

    /**
     * Get the latest workflow run for a specific workflow.
     * This is used to track the workflow that was just triggered.
     */
    public Optional<WorkflowRun> getLatestWorkflowRun(String workflowId) {
        try {
            WorkflowRunsResponse response = githubClient.getWorkflowRuns(
                repoOwner,
                repoName,
                workflowId,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                5  // Get last 5 runs
            );

            if (response.getWorkflowRuns() != null && !response.getWorkflowRuns().isEmpty()) {
                // Return the most recent run
                return response.getWorkflowRuns().stream()
                    .max(Comparator.comparing(WorkflowRun::getCreatedAt));
            }

            return Optional.empty();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to get workflow runs for: %s", workflowId);
            return Optional.empty();
        }
    }

    /**
     * Get a specific workflow run by ID.
     */
    public Optional<WorkflowRun> getWorkflowRun(Long runId) {
        try {
            WorkflowRun run = githubClient.getWorkflowRun(
                repoOwner,
                repoName,
                runId,
                "Bearer " + githubToken,
                "application/vnd.github+json"
            );

            return Optional.of(run);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to get workflow run: %d", runId);
            return Optional.empty();
        }
    }
}

