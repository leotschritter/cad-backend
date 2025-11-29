package de.htwg.api.location.service;

import de.htwg.api.itinerary.mapper.LocationMapper;
import de.htwg.api.itinerary.model.LocationDto;
import de.htwg.api.location.model.AccommodationDto;
import de.htwg.api.location.model.TransportDto;
import de.htwg.persistence.entity.Accommodation;
import de.htwg.persistence.entity.Itinerary;
import de.htwg.persistence.entity.Location;
import de.htwg.persistence.entity.Transport;
import de.htwg.persistence.repository.AccommodationRepository;
import de.htwg.persistence.repository.ItineraryRepository;
import de.htwg.persistence.repository.LocationRepository;
import de.htwg.persistence.repository.TransportRepository;
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
    private final TransportRepository transportRepository;
    private final AccommodationRepository accommodationRepository;

    @Inject
    public LocationServiceImpl(LocationRepository locationRepository,
                               ItineraryRepository itineraryRepository,
                               LocationMapper locationMapper,
                               TransportRepository transportRepository,
                               AccommodationRepository accommodationRepository) {
        this.locationRepository = locationRepository;
        this.itineraryRepository = itineraryRepository;
        this.locationMapper = locationMapper;
        this.transportRepository = transportRepository;
        this.accommodationRepository = accommodationRepository;
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
    public TransportDto addTransportToLocation(Long locationId, TransportDto transportDto) {
        Optional<Location> locationOptional = locationRepository.findByIdOptional(locationId);
        if (locationOptional.isEmpty()) {
            throw new IllegalArgumentException("Location with id " + locationId + " not found");
        }

        Location location = locationOptional.get();
        Transport transport = locationMapper.toEntity(transportDto, location);
        transport.setLocation(location);
        transportRepository.persist(transport);

        location.setTransport(transport);
        locationRepository.persist(location);

        return locationMapper.toDto(transport);
    }

    @Override
    public TransportDto getTransportByLocationId(Long locationId) {
        Optional<Location> locationOptional = locationRepository.findByIdOptional(locationId);

        if (locationOptional.isEmpty()) {
            throw new IllegalArgumentException("Location with id " + locationId + " not found");
        }

        Location location = locationOptional.get();
        Transport transport = location.getTransport();
        if (transport != null) {
            return locationMapper.toDto(transport);
        } else {
            return null;
        }
    }

    @Override
    public AccommodationDto addAccommodationToLocation(Long locationId, AccommodationDto accommodationDto) {
        Optional<Location> locationOptional = locationRepository.findByIdOptional(locationId);
        if (locationOptional.isEmpty()) {
            throw new IllegalArgumentException("Location with id " + locationId + " not found");
        }

        Location location = locationOptional.get();
        Accommodation accommodation = locationMapper.toEntity(accommodationDto, location);
        accommodation.setLocation(location);
        accommodationRepository.persist(accommodation);

        location.setAccommodation(accommodation);
        locationRepository.persist(location);

        return locationMapper.toDto(accommodation);
    }

    @Override
    public AccommodationDto getAccommodationByLocationId(Long locationId) {
        Optional<Location> locationOptional = locationRepository.findByIdOptional(locationId);

        if (locationOptional.isEmpty()) {
            throw new IllegalArgumentException("Location with id " + locationId + " not found");
        }

        Location location = locationOptional.get();
        Accommodation accommodation = location.getAccommodation();
        if (accommodation != null) {
            return locationMapper.toDto(accommodation);
        } else {
            return null;
        }
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

