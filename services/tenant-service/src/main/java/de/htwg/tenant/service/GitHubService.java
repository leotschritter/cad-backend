package de.htwg.tenant.service;

import de.htwg.tenant.client.GitHubActionsClient;
import de.htwg.tenant.client.dto.RepositoryDispatchRequest;
import de.htwg.tenant.client.dto.WorkflowRun;
import de.htwg.tenant.client.dto.WorkflowRunsResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for interacting with GitHub Actions via repository dispatch.
 */
@ApplicationScoped
public class GitHubService {

    private static final Logger LOG = Logger.getLogger(GitHubService.class);
    private static final String DEPLOY_EVENT_TYPE = "deploy-multi-namespace";

    @RestClient
    GitHubActionsClient githubClient;

    @ConfigProperty(name = "github.api.token")
    String githubToken;

    @ConfigProperty(name = "github.repository.owner")
    String repoOwner;

    @ConfigProperty(name = "github.repository.name")
    String repoName;

    @ConfigProperty(name = "github.branch", defaultValue = "main")
    String branch;

    /**
     * Trigger deployment for a standard tier tenant.
     * Uses repository_dispatch to trigger deploy-multi-namespace workflow.
     *
     * @param tenantNumber The numeric tenant identifier (1, 2, 3, etc.)
     * @param environment "prod" or "dev"
     * @param cluster Cluster name (e.g., "tripico-cluster")
     * @return Dispatch ID for tracking
     */
    public String triggerTenantDeployment(Integer tenantNumber, String environment, String cluster) {
        try {
            String dispatchId = UUID.randomUUID().toString();
            
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("tier", "standard");
            clientPayload.put("tenant_number", tenantNumber.toString());
            clientPayload.put("environment", environment);
            clientPayload.put("cluster", cluster);
            clientPayload.put("ref", branch);
            clientPayload.put("dispatch_id", dispatchId); // For tracking

            RepositoryDispatchRequest request = new RepositoryDispatchRequest(
                DEPLOY_EVENT_TYPE,
                clientPayload
            );

            LOG.infof("🚀 Triggering deployment for tenant number %d (dispatch ID: %s)", tenantNumber, dispatchId);

            githubClient.dispatchRepository(
                repoOwner,
                repoName,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Deployment triggered successfully for tenant number %d", tenantNumber);
            return dispatchId;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger deployment for tenant number: %d", tenantNumber);
            throw new RuntimeException("Failed to trigger tenant deployment", e);
        }
    }

    /**
     * Trigger cleanup for a tenant namespace.
     * This will delete the namespace and all resources within it.
     *
     * @param namespace The namespace to delete (e.g., "standard-1")
     * @return Dispatch ID for tracking
     */
    public String triggerTenantCleanup(String namespace) {
        try {
            String dispatchId = UUID.randomUUID().toString();
            
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("action", "cleanup");
            clientPayload.put("namespace", namespace);
            clientPayload.put("dispatch_id", dispatchId);

            RepositoryDispatchRequest request = new RepositoryDispatchRequest(
                "cleanup-tenant",
                clientPayload
            );

            LOG.infof("🗑️ Triggering cleanup for namespace %s (dispatch ID: %s)", namespace, dispatchId);

            githubClient.dispatchRepository(
                repoOwner,
                repoName,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Cleanup triggered successfully for namespace %s", namespace);
            return dispatchId;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger cleanup for namespace: %s", namespace);
            throw new RuntimeException("Failed to trigger tenant cleanup", e);
        }
    }

    /**
     * Get recent workflow runs for the deploy-multi-namespace workflow.
     * Can be used to check deployment status.
     */
    public Optional<WorkflowRun> getLatestDeploymentRun() {
        try {
            WorkflowRunsResponse response = githubClient.getWorkflowRuns(
                repoOwner,
                repoName,
                "deploy-multi-namespace.yml",
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
            LOG.errorf(e, "❌ Failed to get workflow runs");
            return Optional.empty();
        }
    }
}

