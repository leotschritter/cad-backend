package de.htwg.api.itinerary.model;

import lombok.Builder;

import java.util.List;

@Builder
public record LikeResponseDto(
    Long itineraryId,
    int likeCount,
    List<LikeDto> likes
) {
}
