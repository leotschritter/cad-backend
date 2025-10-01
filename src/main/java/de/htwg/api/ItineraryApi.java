package de.htwg.api;


import de.htwg.api.model.IterneraryDto;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@Path("/itinerary")
public class ItineraryApi {

    @POST
    @Path("/create")
    public Response createItinerary(@RequestBody final IterneraryDto iterneraryDto) {
        return Response.ok().build();
    }


}
