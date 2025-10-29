package de.htwg.api.location.service;

import de.htwg.api.itinerary.mapper.LocationMapper;
import de.htwg.api.itinerary.model.LocationDto;
import de.htwg.persistence.entity.Itinerary;
import de.htwg.persistence.entity.Location;
import de.htwg.persistence.repository.ItineraryRepository;
import de.htwg.persistence.repository.LocationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final ItineraryRepository itineraryRepository;
    private final LocationMapper locationMapper;

    @Inject
    public LocationServiceImpl(LocationRepository locationRepository,
                              ItineraryRepository itineraryRepository,
                              LocationMapper locationMapper) {
        this.locationRepository = locationRepository;
        this.itineraryRepository = itineraryRepository;
        this.locationMapper = locationMapper;
    }

    @Override
    @Transactional
    public LocationDto addLocationToItinerary(Long itineraryId, LocationDto locationDto) {
        Optional<Itinerary> itineraryOptional = itineraryRepository.findByIdOptional(itineraryId);
        if (itineraryOptional.isEmpty()) {
            throw new IllegalArgumentException("Itinerary with id " + itineraryId + " not found");
        }

        Itinerary itinerary = itineraryOptional.get();
        Location location = locationMapper.toEntity(locationDto);
        location.setItinerary(itinerary);
        locationRepository.persist(location);
        
        return locationMapper.toDto(location);
    }

    @Override
    public List<LocationDto> getLocationsForItinerary(Long itineraryId) {
        Optional<Itinerary> itineraryOptional = itineraryRepository.findByIdOptional(itineraryId);
        if (itineraryOptional.isEmpty()) {
            throw new IllegalArgumentException("Itinerary with id " + itineraryId + " not found");
        }

        Itinerary itinerary = itineraryOptional.get();
        return locationMapper.toDtoList(itinerary.getLocations());
    }

    @Override
    public LocationDto getLocationById(Long locationId) {
        Optional<Location> locationOptional = locationRepository.findByIdOptional(locationId);
        if (locationOptional.isEmpty()) {
            throw new IllegalArgumentException("Location with id " + locationId + " not found");
        }

        return locationMapper.toDto(locationOptional.get());
    }

    @Override
    @Transactional
    public void deleteLocation(Long locationId) {
        Optional<Location> locationOptional = locationRepository.findByIdOptional(locationId);
        if (locationOptional.isEmpty()) {
            throw new IllegalArgumentException("Location with id " + locationId + " not found");
        }

        locationRepository.delete(locationOptional.get());
    }

    @Override
    @Transactional
    public void addImagesToLocation(Long locationId, List<String> imageUrls) {
        Optional<Location> locationOptional = locationRepository.findByIdOptional(locationId);
        if (locationOptional.isEmpty()) {
            throw new IllegalArgumentException("Location with id " + locationId + " not found");
        }

        Location location = locationOptional.get();
        if (location.getImageUrls() == null) {
            location.setImageUrls(imageUrls);
        } else {
            location.getImageUrls().addAll(imageUrls);
        }
        locationRepository.persist(location);
    }

    @Override
    @Transactional
    public void removeImageFromLocation(Long locationId, String imageUrl) {
        Optional<Location> locationOptional = locationRepository.findByIdOptional(locationId);
        if (locationOptional.isEmpty()) {
            throw new IllegalArgumentException("Location with id " + locationId + " not found");
        }

        Location location = locationOptional.get();
        if (location.getImageUrls() != null) {
            location.getImageUrls().remove(imageUrl);
            locationRepository.persist(location);
        }
    }
}

