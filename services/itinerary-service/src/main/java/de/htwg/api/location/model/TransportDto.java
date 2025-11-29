package de.htwg.api.location.model;

import lombok.Builder;

@Builder
public record TransportDto(
        Long id,
        String transportType,
        Long duration,
        Long distance
) {
}
