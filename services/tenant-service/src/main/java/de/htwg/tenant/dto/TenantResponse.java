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
    private Integer tenantNumber;
    private String namespace;
    private String frontendDomain;
    private String ownerEmail;
    private String state;
    private String tier;
    private String errorMessage;
    private String identityPlatformTenantId;
    private String apiGatewayUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;

    // Service endpoints for this tenant
    private String itineraryUrl;
    private String weatherUrl;
    private String warningsUrl;
    private String recommendationsUrl;
    private String commentsUrl;

    public static TenantResponse from(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.id.toString());
        response.setName(tenant.name);
        response.setTenantId(tenant.tenantId);
        response.setTenantNumber(tenant.tenantNumber);
        response.setNamespace(tenant.namespace);
        response.setFrontendDomain(tenant.frontendDomain);
        response.setOwnerEmail(tenant.ownerEmail);
        response.setState(tenant.state.toString());
        response.setTier(tenant.tier.toString());
        response.setErrorMessage(tenant.errorMessage);
        
        // Only set if available (from Terraform outputs)
        if (tenant.identityPlatformTenantId != null && !tenant.identityPlatformTenantId.isEmpty()) {
            response.setIdentityPlatformTenantId(tenant.identityPlatformTenantId);
        }
        if (tenant.apiGatewayUrl != null && !tenant.apiGatewayUrl.isEmpty()) {
            response.setApiGatewayUrl(tenant.apiGatewayUrl);
        }
        
        response.setCreatedAt(tenant.createdAt);
        response.setUpdatedAt(tenant.updatedAt);
        response.setActivatedAt(tenant.activatedAt);

        // Build service URLs based on tenant number
        if (tenant.tenantNumber != null) {
            String domain = tenant.frontendDomain.replace("frontend-", "");
            response.setItineraryUrl(String.format("https://itinerary-%s", domain));
            response.setWeatherUrl(String.format("https://weather.%s", domain.replace("standard-" + tenant.tenantNumber + ".", "")));
            response.setWarningsUrl(String.format("https://warnings.%s", domain.replace("standard-" + tenant.tenantNumber + ".", "")));
            response.setRecommendationsUrl(String.format("https://recommendation-%s", domain));
            response.setCommentsUrl(String.format("https://cl-%s", domain));
        }

        return response;
    }
}

