package de.htwg.api.itinerary.model;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ItinerarySearchDto(
    String userName,
    String userEmail,
    String title,
    String destination,
    String description,
    LocalDate startDateFrom,
    LocalDate startDateTo
) {
}

