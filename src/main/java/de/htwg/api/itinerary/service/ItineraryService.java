package de.htwg.api.itinerary.service;

import de.htwg.api.itinerary.model.ItineraryDto;

import java.util.List;

public interface ItineraryService {

    void createItinerary(ItineraryDto itineraryDto, Long userId);

    List<ItineraryDto> getItinerariesByUserId(Long userId);
}
