package de.htwg.tenant.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GitHub Actions workflow run details.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowRun {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("status")
    private String status; // queued, in_progress, completed

    @JsonProperty("conclusion")
    private String conclusion; // success, failure, cancelled, etc.

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}

