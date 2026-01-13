package de.htwg.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
}

