package de.htwg.tenant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request to trigger continuous deployment for all active tenants.
 */
@Data
public class TriggerCdRequest {

    /**
     * Comma-separated list of services to deploy (e.g., "itinerary,comments-likes,recommendation")
     */
    @NotNull
    private String services;

    /**
     * Environment to deploy to (prod or dev)
     */
    private String environment = "prod";

    /**
     * Parse services string into a list
     */
    public List<String> getServicesList() {
        if (services == null || services.trim().isEmpty()) {
            return List.of();
        }
        return List.of(services.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
