package de.htwg.tenant.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for GitHub repository dispatch API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDispatchRequest {

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("client_payload")
    private Map<String, Object> clientPayload;
}
