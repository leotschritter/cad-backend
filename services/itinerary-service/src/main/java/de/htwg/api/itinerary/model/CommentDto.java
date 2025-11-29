package de.htwg.api.itinerary.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CommentDto(
    String id,
    String userEmail,
    Long itineraryId,
    String comment,
    LocalDateTime createdAt
) {
}

