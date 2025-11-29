package de.htwg.api.like.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LikeDto(
    String id,
    String userEmail,
    Long itineraryId,
    LocalDateTime createdAt
) {
}
