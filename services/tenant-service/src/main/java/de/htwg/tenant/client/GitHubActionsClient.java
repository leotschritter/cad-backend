package de.htwg.tenant.client;

import de.htwg.tenant.client.dto.WorkflowDispatchRequest;
import de.htwg.tenant.client.dto.WorkflowRun;
import de.htwg.tenant.client.dto.WorkflowRunsResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for GitHub Actions API.
 */
@RegisterRestClient(configKey = "github-api")
@Path("/repos/{owner}/{repo}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GitHubActionsClient {

    /**
     * Trigger a workflow dispatch event.
     */
    @POST
    @Path("/actions/workflows/{workflow_id}/dispatches")
    void dispatchWorkflow(
        @PathParam("owner") String owner,
        @PathParam("repo") String repo,
        @PathParam("workflow_id") String workflowId,
        @HeaderParam("Authorization") String authorization,
        @HeaderParam("Accept") String accept,
        WorkflowDispatchRequest request
    );

    /**
     * Get workflow runs for a specific workflow.
     */
    @GET
    @Path("/actions/workflows/{workflow_id}/runs")
    WorkflowRunsResponse getWorkflowRuns(
        @PathParam("owner") String owner,
        @PathParam("repo") String repo,
        @PathParam("workflow_id") String workflowId,
        @HeaderParam("Authorization") String authorization,
        @HeaderParam("Accept") String accept,
        @QueryParam("per_page") int perPage
    );

    /**
     * Get a specific workflow run by ID.
     */
    @GET
    @Path("/actions/runs/{run_id}")
    WorkflowRun getWorkflowRun(
        @PathParam("owner") String owner,
        @PathParam("repo") String repo,
        @PathParam("run_id") Long runId,
        @HeaderParam("Authorization") String authorization,
        @HeaderParam("Accept") String accept
    );
}

