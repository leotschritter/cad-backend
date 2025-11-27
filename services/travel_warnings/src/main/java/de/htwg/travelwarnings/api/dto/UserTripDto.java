package de.htwg.travelwarnings.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for user trip management
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTripDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private String email;
    private String countryCode;
    private String countryName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String tripName;
    private Boolean notificationsEnabled;
}

