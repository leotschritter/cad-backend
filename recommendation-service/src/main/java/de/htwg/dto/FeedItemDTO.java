package de.htwg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for a feed item representing an itinerary recommendation.
 * Based on Story 1: Each card shows title, short description, traveller name and number of likes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemDTO {
    
    private Long itineraryId;
    private String title;
    private String description;
    private String travellerName;
    private Integer likesCount;
    private List<String> destinations;
    private String matchReason; // e.g., "From travellers who visited: Paris"
    private Double relevanceScore;
}

