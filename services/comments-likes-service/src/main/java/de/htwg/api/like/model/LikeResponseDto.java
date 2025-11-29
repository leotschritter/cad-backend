package de.htwg.api.like.model;

import lombok.Builder;

@Builder
public record LikeResponseDto(
    Long itineraryId,
    long likeCount
) {
}
