package de.htwg.tenant.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Request body for GitHub Actions workflow_dispatch API.
 * Used to trigger workflows with specific inputs.
 */
public class WorkflowDispatchRequest {
    
    @JsonProperty("ref")
    private String ref;
    
    @JsonProperty("inputs")
    private Map<String, String> inputs;

    public WorkflowDispatchRequest() {
    }

    public WorkflowDispatchRequest(String ref, Map<String, String> inputs) {
        this.ref = ref;
        this.inputs = inputs;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
    }
}
