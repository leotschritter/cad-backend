package de.htwg.api.itinerary.mapper;

import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.api.itinerary.model.ItinerarySearchResponseDto;
import de.htwg.persistence.entity.Itinerary;
import de.htwg.persistence.entity.User;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ItineraryMapper {

    public Itinerary toEntity(ItineraryDto dto, User user) {
        return Itinerary.builder()
                .title(dto.title())
                .destination(dto.destination())
                .startDate(dto.startDate())
                .shortDescription(dto.shortDescription())
                .detailedDescription(dto.detailedDescription())
                .user(user)
                .build();
    }

    public ItineraryDto toDto(Itinerary entity) {
        return ItineraryDto.builder()
                .title(entity.getTitle())
                .destination(entity.getDestination())
                .startDate(entity.getStartDate())
                .shortDescription(entity.getShortDescription())
                .detailedDescription(entity.getDetailedDescription())
                .build();
    }

    public ItinerarySearchResponseDto toSearchResponseDto(Itinerary entity) {
        return ItinerarySearchResponseDto.builder()
                .title(entity.getTitle())
                .destination(entity.getDestination())
                .startDate(entity.getStartDate())
                .shortDescription(entity.getShortDescription())
                .detailedDescription(entity.getDetailedDescription())
                .userName(entity.getUser() != null ? entity.getUser().getName() : null)
                .build();
    }

    public List<ItineraryDto> toDtoList(List<Itinerary> entities) {
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ItinerarySearchResponseDto> toSearchResponseDtoList(List<Itinerary> entities) {
        return entities.stream()
                .map(this::toSearchResponseDto)
                .collect(Collectors.toList());
    }
}
