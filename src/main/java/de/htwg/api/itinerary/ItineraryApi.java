package de.htwg.api.itinerary;


import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.api.itinerary.service.ItineraryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
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
    public Response createItinerary(@RequestBody final ItineraryDto itineraryDto, 
                                   @QueryParam("userId") Long userId) {

        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("User ID is required")
                    .build();
        }

        itineraryService.createItinerary(itineraryDto, userId);

        return Response.ok().build();
    }


    @GET
    @Path("/get")
    public Response getItinerary(@QueryParam("userId") Long userId) {

        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("User ID is required")
                    .build();
        }

        final List<ItineraryDto> itineraryDtos = itineraryService.getItinerariesByUserId(userId);

        return Response.ok(itineraryDtos).build();
    }


}
