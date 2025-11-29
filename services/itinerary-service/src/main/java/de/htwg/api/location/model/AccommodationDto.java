package de.htwg.api.location.model;

import lombok.Builder;

@Builder
public record AccommodationDto(
        Long id,
        String name,
        Double pricePerNight,
        Float rating,
        String notes,
        String accommodationImageUrl,
        String bookingPageUrl
) {
}
