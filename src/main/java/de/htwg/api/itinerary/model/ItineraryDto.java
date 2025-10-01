package de.htwg.api.itinerary.model;

import lombok.Builder;

import java.util.Date;

@Builder
public record ItineraryDto(String title, String destination, Date startDate, String shortDescription,
                           String detailedDescription) {
}
