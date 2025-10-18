package de.htwg.api.itinerary.service;

import de.htwg.api.itinerary.mapper.ItineraryMapper;
import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.api.itinerary.model.ItinerarySearchDto;
import de.htwg.api.itinerary.model.ItinerarySearchResponseDto;
import de.htwg.persistence.entity.Itinerary;
import de.htwg.persistence.entity.User;
import de.htwg.persistence.repository.ItineraryRepository;
import de.htwg.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final ItineraryMapper itineraryMapper;

    @Inject
    public ItineraryServiceImpl(ItineraryRepository itineraryRepository, 
                               UserRepository userRepository, 
                               ItineraryMapper itineraryMapper) {
        this.itineraryRepository = itineraryRepository;
        this.userRepository = userRepository;
        this.itineraryMapper = itineraryMapper;
    }

    @Override
    @Transactional
    public void createItinerary(ItineraryDto itineraryDto, Long userId) {
        Optional<User> userOptional = userRepository.findByIdOptional(userId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User with id " + userId + " not found");
        }
        
        User user = userOptional.get();
        Itinerary itinerary = itineraryMapper.toEntity(itineraryDto, user);
        itineraryRepository.persist(itinerary);
    }

    @Override
    @Transactional
    public void createItineraryByEmail(ItineraryDto itineraryDto, String email) {
        Optional<User> userOptional = userRepository.find("email", email).firstResultOptional();
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User with email " + email + " not found");
        }

        User user = userOptional.get();
        Itinerary itinerary = itineraryMapper.toEntity(itineraryDto, user);
        itineraryRepository.persist(itinerary);
    }



    @Override
    public List<ItineraryDto> getItinerariesByUserId(Long userId) {
        List<Itinerary> itineraries = itineraryRepository.findByUserId(userId);
        return itineraryMapper.toDtoList(itineraries);
    }

    @Override
    public List<ItineraryDto> getItinerariesByEmail(String email) {
        List<Itinerary> itineraries = itineraryRepository.findByUserEmail(email);
        return itineraryMapper.toDtoList(itineraries);
    }

    @Override
    public List<ItinerarySearchResponseDto> searchItineraries(ItinerarySearchDto searchDto) {
        List<Itinerary> itineraries = itineraryRepository.searchItineraries(
            searchDto.userName(),
            searchDto.userEmail(),
            searchDto.title(),
            searchDto.destination(),
            searchDto.description(),
            searchDto.startDateFrom(),
            searchDto.startDateTo()
        );
        return itineraryMapper.toSearchResponseDtoList(itineraries);
    }
}
