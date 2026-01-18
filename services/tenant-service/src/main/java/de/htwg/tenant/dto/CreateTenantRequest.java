package de.htwg.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a new tenant.
 */
@Data
public class CreateTenantRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 3, max = 50, message = "Tenant name must be between 3 and 50 characters")
    private String name;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Invalid email format")
    private String ownerEmail;

    @NotBlank(message = "Owner password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String ownerPassword;

    /**
     * Tenant tier: STANDARD or ENTERPRISE
     * Defaults to STANDARD if not provided
     */
    @Pattern(regexp = "STANDARD|ENTERPRISE", message = "Tier must be either STANDARD or ENTERPRISE")
    private String tier = "STANDARD";

    /**
     * Enterprise name (required for ENTERPRISE tier)
     * Used to create namespace: enterprise-{enterpriseName}
     * Example: "acme-corp" -> namespace: enterprise-acme-corp
     */
    @Size(min = 3, max = 30, message = "Enterprise name must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-z0-9-]*$", message = "Enterprise name must contain only lowercase letters, numbers, and hyphens")
    private String enterpriseName;
}

