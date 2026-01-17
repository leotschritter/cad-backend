package de.htwg.tenant.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response from GitHub Actions workflow runs API.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowRunsResponse {

    @JsonProperty("total_count")
    private int totalCount;

    @JsonProperty("workflow_runs")
    private List<WorkflowRun> workflowRuns;
}

