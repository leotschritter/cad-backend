package de.htwg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for recording location visits.
 * When a user adds a location to an itinerary, this tracks the visit in the graph.
 * The user email is automatically obtained from the security context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationVisitDTO {

    private List<String> locationNames;
    private Long itineraryId;
}

