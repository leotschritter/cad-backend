package de.htwg.tenant.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Terraform outputs from tf-outputs.json artifact.
 * Each output has a "value" field containing the actual value.
 */
public class TerraformOutputs {
    
    @JsonProperty("api_gateway_url")
    private TerraformOutput apiGatewayUrl;
    
    @JsonProperty("identity_platform_tenant_id")
    private TerraformOutput identityPlatformTenantId;
    
    @JsonProperty("tenant_name")
    private TerraformOutput tenantName;

    public TerraformOutputs() {
    }

    public TerraformOutput getApiGatewayUrl() {
        return apiGatewayUrl;
    }

    public void setApiGatewayUrl(TerraformOutput apiGatewayUrl) {
        this.apiGatewayUrl = apiGatewayUrl;
    }

    public TerraformOutput getIdentityPlatformTenantId() {
        return identityPlatformTenantId;
    }

    public void setIdentityPlatformTenantId(TerraformOutput identityPlatformTenantId) {
        this.identityPlatformTenantId = identityPlatformTenantId;
    }

    public TerraformOutput getTenantName() {
        return tenantName;
    }

    public void setTenantName(TerraformOutput tenantName) {
        this.tenantName = tenantName;
    }

    /**
     * Individual Terraform output structure.
     */
    public static class TerraformOutput {
        @JsonProperty("value")
        private String value;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("sensitive")
        private Boolean sensitive;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getSensitive() {
            return sensitive;
        }

        public void setSensitive(Boolean sensitive) {
            this.sensitive = sensitive;
        }
    }
}
