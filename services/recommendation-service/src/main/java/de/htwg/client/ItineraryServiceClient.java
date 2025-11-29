package de.htwg.client;

import de.htwg.dto.ItineraryDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * REST client for communicating with the Itinerary Service.
 * Forwards authentication headers for secure communication.
 */
@Path("/itinerary")
@RegisterRestClient(configKey = "itinerary-service")
public interface ItineraryServiceClient {

    /**
     * Get itineraries by their IDs.
     * Forwards the authentication token from the original request.
     *
     * @param authorizationHeader The Bearer token from the original request
     * @param ids List of itinerary IDs to fetch
     * @return List of itinerary DTOs
     */
    @POST
    @Path("/by-ids")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    List<ItineraryDTO> getItinerariesByIds(
            @HeaderParam("Authorization") String authorizationHeader,
            List<Long> ids
    );
}

