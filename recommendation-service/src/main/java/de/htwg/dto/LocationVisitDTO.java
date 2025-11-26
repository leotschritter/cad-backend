package de.htwg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for recording location visits.
 * When a user adds a location to an itinerary, this tracks the visit in the graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationVisitDTO {

    private String userId;
    private List<String> locationNames;
    private Long itineraryId;
}

