package de.htwg.api.itinerary.mapper;

import de.htwg.api.itinerary.model.LocationDto;
import de.htwg.persistence.entity.Location;
import de.htwg.service.storage.ImageStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class LocationMapper {

    private final ImageStorageService imageStorageService;

    @Inject
    public LocationMapper(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

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
        // Generate fresh signed URLs (or direct URLs in dev) for all images
        // The location.getImageUrls() contains filename paths, not URLs
        List<String> signedImageUrls = null;
        if (location.getImageUrls() != null && !location.getImageUrls().isEmpty()) {
            signedImageUrls = location.getImageUrls().stream()
                    .map(imageStorageService::getImageUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return LocationDto.builder()
                .id(location.getId())
                .name(location.getName())
                .description(location.getDescription())
                .fromDate(location.getFromDate())
                .toDate(location.getToDate())
                .imageUrls(signedImageUrls)
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
