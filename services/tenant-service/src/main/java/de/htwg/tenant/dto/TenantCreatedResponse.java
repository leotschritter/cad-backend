package de.htwg.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for tenant creation (simplified for immediate response).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantCreatedResponse {

    private String id;
    private String tenantId;
    private String name;
    private String subdomain;
    private String state;
    private String message;

    public static TenantCreatedResponse from(String id, String tenantId, String name, String subdomain, String state) {
        return new TenantCreatedResponse(
            id,
            tenantId,
            name,
            subdomain,
            state,
            "Tenant provisioning initiated. You will receive an email when your tenant is ready."
        );
    }
}

