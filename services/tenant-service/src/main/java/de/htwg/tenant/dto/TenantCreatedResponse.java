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
    private Integer tenantNumber;
    private String name;
    private String namespace;
    private String frontendDomain;
    private String state;
    private String message;

    public static TenantCreatedResponse from(Tenant tenant) {
        return new TenantCreatedResponse(
            tenant.id.toString(),
            tenant.tenantId,
            tenant.tenantNumber,
            tenant.name,
            tenant.namespace,
            tenant.frontendDomain,
            tenant.state.toString(),
            String.format("Tenant provisioning initiated in namespace %s. You will receive an email when your tenant is ready at %s", 
                tenant.namespace, tenant.frontendDomain)
        );
    }
}

