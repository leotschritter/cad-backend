package de.htwg.tenant.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from GitHub Actions artifacts API.
 */
public class ArtifactsResponse {
    
    @JsonProperty("total_count")
    private Integer totalCount;
    
    @JsonProperty("artifacts")
    private List<Artifact> artifacts;

    public ArtifactsResponse() {
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }
}
