package de.htwg.api.itinerary.service;

import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.api.itinerary.model.ItinerarySearchDto;
import de.htwg.api.itinerary.model.ItinerarySearchResponseDto;

import java.util.List;

public interface ItineraryService {

    void createItinerary(ItineraryDto itineraryDto, Long userId);

    void createItineraryByEmail(ItineraryDto itineraryDto, String email);

    List<ItineraryDto> getItinerariesByUserId(Long userId);

    List<ItineraryDto> getItinerariesByEmail(String email);

    List<ItineraryDto> getItinerariesByIds(List<Long> ids);

    List<ItinerarySearchResponseDto> searchItineraries(ItinerarySearchDto searchDto);

}
