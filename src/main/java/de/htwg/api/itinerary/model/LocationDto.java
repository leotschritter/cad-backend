package de.htwg.api.itinerary.model;

import de.htwg.api.location.model.AccommodationDto;
import de.htwg.api.location.model.TransportDto;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record LocationDto(
    Long id,
    String name,
    String description,
    LocalDate fromDate,
    LocalDate toDate,
    List<String> imageUrls,
    TransportDto transportDto,
    AccommodationDto accommodationDto
) {
}
