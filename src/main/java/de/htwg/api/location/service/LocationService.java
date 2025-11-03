package de.htwg.api.location.service;

import de.htwg.api.location.model.AccommodationDto;
import de.htwg.api.itinerary.model.LocationDto;
import de.htwg.api.location.model.TransportDto;

import java.util.List;

public interface LocationService {

    LocationDto addLocationToItinerary(Long itineraryId, LocationDto locationDto);

    List<LocationDto> getLocationsForItinerary(Long itineraryId);

    LocationDto getLocationById(Long locationId);

    TransportDto addTransportToLocation(Long locationId, TransportDto transportDto);

    TransportDto getTransportByLocationId(Long locationId);

    AccommodationDto addAccommodationToLocation(Long locationId, AccommodationDto accommodationDto);

    AccommodationDto getAccommodationByLocationId(Long locationId);

    void deleteLocation(Long locationId);

    void addImagesToLocation(Long locationId, List<String> imageUrls);

    void removeImageFromLocation(Long locationId, String imageUrl);
}

