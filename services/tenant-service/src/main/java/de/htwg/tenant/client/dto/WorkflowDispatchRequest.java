package de.htwg.tenant.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for GitHub Actions workflow dispatch.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDispatchRequest {

    @JsonProperty("ref")
    private String ref = "main";

    @JsonProperty("inputs")
    private Map<String, String> inputs;
}

