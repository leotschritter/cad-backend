package de.htwg.api.itinerary.model;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ItinerarySearchResponseDto(
    String title,
    String destination,
    LocalDate startDate,
    String shortDescription,
    String detailedDescription,
    String userName
) {
}

