package de.htwg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for a feed item representing an itinerary recommendation.
 * Exactly matches the structure of ItineraryDto from the Itinerary Service
 * so frontend can reuse the same components.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemDTO {
    
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

