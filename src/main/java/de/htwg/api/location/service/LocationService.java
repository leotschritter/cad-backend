package de.htwg.api.location.service;

import de.htwg.api.itinerary.model.LocationDto;

import java.util.List;

public interface LocationService {

    LocationDto addLocationToItinerary(Long itineraryId, LocationDto locationDto);

    List<LocationDto> getLocationsForItinerary(Long itineraryId);

    LocationDto getLocationById(Long locationId);

    void deleteLocation(Long locationId);

    void addImagesToLocation(Long locationId, List<String> imageUrls);

    void removeImageFromLocation(Long locationId, String imageUrl);
}

