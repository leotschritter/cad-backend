package de.htwg.travelwarnings.api.dto;

import de.htwg.travelwarnings.persistence.entity.WarningSeverity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for travel warning API responses
 * User Story 1: Alert severity is clearly indicated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelWarningDto {
    private String contentId;
    private Long lastModified;
    private Long effective;
    private String title;
    private String countryCode;
    private String iso3CountryCode;
    private String countryName;
    private Boolean warning;
    private Boolean partialWarning;
    private Boolean situationWarning;
    private Boolean situationPartWarning;
    private WarningSeverity severity;
    private String severityDisplay;
    private Boolean hasActiveWarning;
}

