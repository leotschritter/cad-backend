package de.htwg.tenant.service;

import de.htwg.tenant.client.GitHubActionsClient;
import de.htwg.tenant.client.dto.RepositoryDispatchRequest;
import de.htwg.tenant.client.dto.WorkflowDispatchRequest;
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
    private static final String DEPLOY_SHARED_EVENT_TYPE = "deploy-shared-services";

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

    @ConfigProperty(name = "github.frontend.repository.owner")
    String frontendRepoOwner;

    @ConfigProperty(name = "github.frontend.repository.name")
    String frontendRepoName;

    @ConfigProperty(name = "github.frontend.branch", defaultValue = "main")
    String frontendBranch;

    /**
     * Trigger deployment for a standard tier tenant.
     * Uses repository_dispatch to trigger deploy-multi-namespace workflow.
     *
     * @param tenantNumber The numeric tenant identifier (1, 2, 3, etc.)
     * @param environment "prod" or "dev"
     * @param cluster Cluster name (e.g., "tripico-cluster")
     * @return Dispatch ID for tracking
     */
    public String triggerTenantDeployment(Integer tenantNumber, String environment, String cluster, String identityPlatformTenantId) {
        try {
            String dispatchId = UUID.randomUUID().toString();
            
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("tier", "standard");
            clientPayload.put("tenant_number", tenantNumber.toString());
            clientPayload.put("environment", environment);
            clientPayload.put("cluster", cluster);
            clientPayload.put("tenant_id", identityPlatformTenantId);
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
     * Trigger deployment for an enterprise tier tenant.
     * Uses repository_dispatch to trigger deploy-multi-namespace workflow.
     *
     * @param enterpriseName The enterprise name (e.g., "acme-corp", "company-x")
     * @param environment "prod" or "dev"
     * @param cluster Dedicated cluster name (e.g., "tripico-acme-corp-cluster")
     * @return Dispatch ID for tracking
     */
    public String triggerEnterpriseDeployment(String enterpriseName, String environment, String cluster, String identityPlatformTenantId) {
        try {
            String dispatchId = UUID.randomUUID().toString();
            
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("tier", "enterprise");
            clientPayload.put("tenant_name", enterpriseName);
            clientPayload.put("environment", environment);
            clientPayload.put("cluster", cluster);
            clientPayload.put("tenant_id", identityPlatformTenantId);
            clientPayload.put("ref", branch);
            clientPayload.put("dispatch_id", dispatchId);

            RepositoryDispatchRequest request = new RepositoryDispatchRequest(
                DEPLOY_EVENT_TYPE,
                clientPayload
            );

            LOG.infof("🚀 Triggering enterprise deployment for %s (dispatch ID: %s)", enterpriseName, dispatchId);

            githubClient.dispatchRepository(
                repoOwner,
                repoName,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Enterprise deployment triggered successfully for %s", enterpriseName);
            return dispatchId;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger enterprise deployment for: %s", enterpriseName);
            throw new RuntimeException("Failed to trigger enterprise deployment", e);
        }
    }

    /**
     * Trigger deployment of shared services (weather & travel warnings).
     * These services are deployed to the 'shared' namespace and used by all tenants.
     *
     * @param environment "prod" or "dev"
     * @param cluster Cluster name (e.g., "tripico-cluster")
     * @return Dispatch ID for tracking
     */
    public String triggerSharedServicesDeployment(String environment, String cluster) {
        try {
            String dispatchId = UUID.randomUUID().toString();
            
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("environment", environment);
            clientPayload.put("cluster", cluster);
            clientPayload.put("ref", branch);
            clientPayload.put("dispatch_id", dispatchId);

            RepositoryDispatchRequest request = new RepositoryDispatchRequest(
                DEPLOY_SHARED_EVENT_TYPE,
                clientPayload
            );

            LOG.infof("🚀 Triggering shared services deployment (dispatch ID: %s)", dispatchId);

            githubClient.dispatchRepository(
                repoOwner,
                repoName,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Shared services deployment triggered successfully");
            return dispatchId;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger shared services deployment");
            throw new RuntimeException("Failed to trigger shared services deployment", e);
        }
    }

    /**
     * Trigger deployment of shared services for enterprise tier.
     * Includes tier and enterprise_name in payload.
     *
     * @param environment "prod" or "dev"
     * @param cluster Enterprise cluster name
     * @param enterpriseName Enterprise tenant name
     * @return Dispatch ID for tracking
     */
    public String triggerSharedServicesDeployment(String environment, String cluster, String enterpriseName) {
        try {
            String dispatchId = UUID.randomUUID().toString();
            
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("tier", "enterprise");
            clientPayload.put("enterprise_name", enterpriseName);
            clientPayload.put("environment", environment);
            clientPayload.put("cluster", cluster);
            clientPayload.put("ref", branch);
            clientPayload.put("dispatch_id", dispatchId);

            RepositoryDispatchRequest request = new RepositoryDispatchRequest(
                DEPLOY_SHARED_EVENT_TYPE,
                clientPayload
            );

            LOG.infof("🚀 Triggering enterprise shared services deployment (dispatch ID: %s)", dispatchId);

            githubClient.dispatchRepository(
                repoOwner,
                repoName,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Enterprise shared services deployment triggered successfully");
            return dispatchId;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger enterprise shared services deployment");
            throw new RuntimeException("Failed to trigger enterprise shared services deployment", e);
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
     * Trigger frontend deployment for a standard tier tenant.
     * Uses repository_dispatch to trigger deploy-multi-namespace workflow in the frontend repository.
     *
     * @param tenantNumber The numeric tenant identifier (1, 2, 3, etc.)
     * @param environment "prod" or "dev"
     * @param apiGatewayUrl The API Gateway URL for this tenant
     * @param tenantId The Identity Platform tenant ID
     * @return Dispatch ID for tracking
     */
    public String triggerFrontendDeployment(Integer tenantNumber, String environment, 
                                           String apiGatewayUrl, String tenantId) {
        try {
            String dispatchId = UUID.randomUUID().toString();
            
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("tier", "standard");
            clientPayload.put("tenant_number", tenantNumber.toString());
            clientPayload.put("environment", environment);
            clientPayload.put("apiGatewayUrl", apiGatewayUrl);
            clientPayload.put("tenant_id", tenantId);
            clientPayload.put("ref", frontendBranch);
            clientPayload.put("dispatch_id", dispatchId);

            RepositoryDispatchRequest request = new RepositoryDispatchRequest(
                "deploy-frontend-multi-namespace",
                clientPayload
            );

            LOG.infof("🚀 Triggering frontend deployment for tenant number %d (dispatch ID: %s)", 
                tenantNumber, dispatchId);
            LOG.infof("   API Gateway URL: %s", apiGatewayUrl);
            LOG.infof("   Tenant ID: %s", tenantId);

            githubClient.dispatchRepository(
                frontendRepoOwner,
                frontendRepoName,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Frontend deployment triggered successfully for tenant number %d", tenantNumber);
            return dispatchId;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger frontend deployment for tenant number: %d", tenantNumber);
            throw new RuntimeException("Failed to trigger frontend deployment", e);
        }
    }

    /**
     * Trigger frontend deployment for an enterprise tier tenant.
     * Uses repository_dispatch to trigger deploy-multi-namespace workflow in the frontend repository.
     *
     * @param enterpriseName The enterprise name (e.g., "acme-corp")
     * @param environment "prod" or "dev"
     * @param apiGatewayUrl The API Gateway URL for this tenant
     * @param tenantId The Identity Platform tenant ID
     * @return Dispatch ID for tracking
     */
    public String triggerEnterpriseFrontendDeployment(String enterpriseName, String environment, 
                                                      String apiGatewayUrl, String tenantId) {
        try {
            String dispatchId = UUID.randomUUID().toString();
            
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("tier", "enterprise");
            clientPayload.put("enterprise_name", enterpriseName);
            clientPayload.put("environment", environment);
            clientPayload.put("apiGatewayUrl", apiGatewayUrl);
            clientPayload.put("tenant_id", tenantId);
            clientPayload.put("ref", frontendBranch);
            clientPayload.put("dispatch_id", dispatchId);

            RepositoryDispatchRequest request = new RepositoryDispatchRequest(
                "deploy-frontend-multi-namespace",
                clientPayload
            );

            LOG.infof("🚀 Triggering enterprise frontend deployment for %s (dispatch ID: %s)", 
                enterpriseName, dispatchId);
            LOG.infof("   API Gateway URL: %s", apiGatewayUrl);
            LOG.infof("   Tenant ID: %s", tenantId);

            githubClient.dispatchRepository(
                frontendRepoOwner,
                frontendRepoName,
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Enterprise frontend deployment triggered successfully for %s", enterpriseName);
            return dispatchId;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger enterprise frontend deployment for: %s", enterpriseName);
            throw new RuntimeException("Failed to trigger enterprise frontend deployment", e);
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

    /**
     * Trigger Terraform workflow to provision infrastructure for a tenant.
     * This should be called BEFORE triggering the Kubernetes/Helm deployment.
     *
     * @param tier "standard" or "enterprise"
     * @param tenantNumber The numeric tenant identifier (for standard)
     * @param tenantName Custom tenant name (for enterprise, optional)
     * @return Workflow dispatch ID for tracking
     */
    public String triggerTerraformWorkflow(String tier, Integer tenantNumber, String tenantName) {
        try {
            Map<String, String> inputs = new HashMap<>();
            inputs.put("action", "apply");
            inputs.put("environment", tier); // "standard" or "enterprise"
            
            if ("standard".equals(tier)) {
                inputs.put("tenant_number", tenantNumber.toString());
            } else if ("enterprise".equals(tier)) {
                inputs.put("tenant_number", tenantNumber.toString());
                if (tenantName != null && !tenantName.isEmpty()) {
                    inputs.put("tenant_name", tenantName);
                }
            }

            WorkflowDispatchRequest request = new WorkflowDispatchRequest(branch, inputs);

            LOG.infof("🚀 Triggering Terraform workflow for %s tier (tenant: %s)", tier, 
                tenantName != null ? tenantName : "standard-" + tenantNumber);

            githubClient.dispatchWorkflow(
                repoOwner,
                repoName,
                "terraform.yml",
                "Bearer " + githubToken,
                "application/vnd.github+json",
                request
            );

            LOG.infof("✅ Terraform workflow triggered successfully");
            return UUID.randomUUID().toString(); // Return tracking ID

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger Terraform workflow");
            throw new RuntimeException("Failed to trigger Terraform workflow", e);
        }
    }

    /**
     * Get the most recent Terraform workflow run.
     * Used to monitor Terraform apply progress.
     */
    public Optional<WorkflowRun> getLatestTerraformRun() {
        try {
            WorkflowRunsResponse response = githubClient.getWorkflowRuns(
                repoOwner,
                repoName,
                "terraform.yml",
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
            LOG.errorf(e, "❌ Failed to get Terraform workflow runs");
            return Optional.empty();
        }
    }
}

