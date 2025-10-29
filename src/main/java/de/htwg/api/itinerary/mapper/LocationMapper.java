package de.htwg.api.itinerary.mapper;

import de.htwg.api.itinerary.model.LocationDto;
import de.htwg.persistence.entity.Location;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class LocationMapper {

    public Location toEntity(LocationDto locationDto) {
        return Location.builder()
                .name(locationDto.name())
                .description(locationDto.description())
                .fromDate(locationDto.fromDate())
                .toDate(locationDto.toDate())
                .imageUrls(locationDto.imageUrls())
                .build();
    }

    public LocationDto toDto(Location location) {

        return LocationDto.builder()
                .id(location.getId())
                .name(location.getName())
                .description(location.getDescription())
                .fromDate(location.getFromDate())
                .toDate(location.getToDate())
                .imageUrls(location.getImageUrls())
                .build();
    }

    public List<LocationDto> toDtoList(List<Location> locations) {
        return locations.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<Location> toEntityList(List<LocationDto> locationDtos) {
        return locationDtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
