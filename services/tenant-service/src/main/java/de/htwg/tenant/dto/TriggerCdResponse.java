package de.htwg.tenant.dto;

import lombok.Data;
import java.util.List;

@Data
public class TriggerCdResponse {
    private String message;
    private int tenantsTriggered;
    private List<String> services;
    private List<TenantDeploymentInfo> tenants;

    @Data
    public static class TenantDeploymentInfo {
        private String tenantId;
        private String tenantName;
        private String namespace;
        private String tier;
        private String dispatchId;
    }
}
