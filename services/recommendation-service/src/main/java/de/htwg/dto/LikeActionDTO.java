package de.htwg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for recording a like action.
 * When a user likes an itinerary in the frontend, this should be sent to create a LIKES relationship.
 * The user email is automatically obtained from the security context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeActionDTO {
    
    private Long itineraryId;
}

