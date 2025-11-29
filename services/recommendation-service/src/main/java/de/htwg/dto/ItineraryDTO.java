package de.htwg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO representing an itinerary from the Itinerary Service.
 * Matches the structure of ItineraryDto from the itinerary service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDTO {
    
    private Long id;
    private String title;
    private String destination;
    
    @JsonProperty("startDate")
    private LocalDate startDate;
    
    @JsonProperty("shortDescription")
    private String shortDescription;
    
    @JsonProperty("detailedDescription")
    private String detailedDescription;
}

