package de.htwg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for recording itinerary creation or modification events.
 * When a user creates or modifies an itinerary, this should be sent to update the graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryEventDTO {

    private String userId;
    private Long itineraryId;
    private String title;
    private String description;
    private List<String> locationNames;
    private Integer likesCount;
    private String eventType; // "CREATED" or "UPDATED"
}

