package de.htwg.api.itinerary.mapper;

import de.htwg.api.location.model.AccommodationDto;
import de.htwg.api.itinerary.model.LocationDto;
import de.htwg.api.location.model.TransportDto;
import de.htwg.persistence.entity.Accommodation;
import de.htwg.persistence.entity.Location;
import de.htwg.persistence.entity.Transport;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class LocationMapper {

    public Location toEntity(LocationDto locationDto) {
        return Location.builder()
                .name(locationDto.name())
                .description(locationDto.description())
                .latitude(locationDto.latitude())
                .longitude(locationDto.longitude())
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
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
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

    public Transport toEntity(TransportDto transportDto, Location location) {
        return Transport.builder()
                .transportType(transportDto.transportType())
                .duration(transportDto.duration())
                .distance(transportDto.distance())
                .location(location)
                .build();
    }

    public TransportDto toDto(Transport transport) {
        return TransportDto.builder()
                .id(transport.getId())
                .transportType(transport.getTransportType())
                .duration(transport.getDuration())
                .distance(transport.getDistance())
                .build();
    }

    public Accommodation toEntity(AccommodationDto accommodationDto, Location location) {
        return Accommodation.builder()
                .name(accommodationDto.name())
                .pricePerNight(accommodationDto.pricePerNight())
                .rating(accommodationDto.rating())
                .notes(accommodationDto.notes())
                .accommodationImageUrl(accommodationDto.accommodationImageUrl())
                .bookingPageUrl(accommodationDto.bookingPageUrl())
                .location(location)
                .build();
    }

    public AccommodationDto toDto(Accommodation accommodation) {
        return AccommodationDto.builder()
                .id(accommodation.getId())
                .name(accommodation.getName())
                .pricePerNight(accommodation.getPricePerNight())
                .rating(accommodation.getRating())
                .notes(accommodation.getNotes())
                .accommodationImageUrl(accommodation.getAccommodationImageUrl())
                .bookingPageUrl(accommodation.getBookingPageUrl())
                .build();
    }
}
