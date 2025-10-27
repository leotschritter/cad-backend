package de.htwg.api.itinerary.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LikeDto(
    String id,
    String userEmail,
    Long itineraryId,
    String comment,
    LocalDateTime createdAt
) {
}
