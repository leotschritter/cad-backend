package de.htwg.api.itinerary.model;

import lombok.Builder;

@Builder
public record LikeResponseDto(
    Long itineraryId,
    long likeCount
) {
}
