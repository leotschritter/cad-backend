package de.htwg.tenant.dto;

import de.htwg.tenant.model.Tenant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for tenant information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {

    private String id;
    private String name;
    private String tenantId;
    private String subdomain;
    private String ownerEmail;
    private String state;
    private String tier;
    private String namespace;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
            tenant.id.toString(),
            tenant.name,
            tenant.tenantId,
            tenant.subdomain,
            tenant.ownerEmail,
            tenant.state.toString(),
            tenant.tier.toString(),
            tenant.namespace,
            tenant.errorMessage,
            tenant.createdAt,
            tenant.updatedAt,
            tenant.activatedAt
        );
    }
}

