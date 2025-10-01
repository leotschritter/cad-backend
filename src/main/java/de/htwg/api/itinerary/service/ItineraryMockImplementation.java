package de.htwg.api.itinerary.service;

import de.htwg.api.itinerary.model.ItineraryDto;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class ItineraryMockImplementation implements ItineraryService {


    @Override
    public void createItinerary(ItineraryDto itineraryDto) {
//        do nothing
    }

    @Override
    public List<ItineraryDto> getItinerary() {
        return List.of(
                ItineraryDto.builder()
                        .title("test1")
                        .startDate(new Date())
                        .destination("norway")
                        .detailedDescription("alalalalalaalaladshgoisHGOR9urfisagptriahsgiashdgäasigpwaugäisagäsadghoisah")
                        .shortDescription("afaughohgwoöoiewaaoidsuiwa")
                        .build()
        );
    }
}
