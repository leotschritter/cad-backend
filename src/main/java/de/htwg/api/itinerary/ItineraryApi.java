package de.htwg.api.itinerary;


import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.api.itinerary.service.ItineraryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import java.util.List;

@Path("/itinerary")
public class ItineraryApi {

    private final ItineraryService itineraryService;

    @Inject
    public ItineraryApi(ItineraryService itineraryService) {

        this.itineraryService = itineraryService;
    }

    @POST
    @Path("/create")
    public Response createItinerary(@RequestBody final ItineraryDto itineraryDto) {

        itineraryService.createItinerary(itineraryDto);

        return Response.ok().build();
    }


    @GET
    @Path("/get")
    public Response getItinerary() {

        final List<ItineraryDto> itineraryDtos = itineraryService.getItinerary();

        return Response.ok(itineraryDtos).build();
    }


}
