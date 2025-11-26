package de.htwg.api;

import de.htwg.dto.ItineraryEventDTO;
import de.htwg.dto.LikeActionDTO;
import de.htwg.dto.LocationVisitDTO;
import de.htwg.security.Authenticated;
import de.htwg.service.GraphService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST API for managing graph database entries.
 * These endpoints allow recording user interactions (likes, itinerary creation, location visits)
 * in the Neo4j graph database for recommendation purposes.
 * All endpoints require authentication via Google Cloud Identity Platform.
 */
@Path("/api/v1/graph")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Graph", description = "Graph database management for recommendations")
@Authenticated
public class GraphResource {

    private static final Logger LOG = Logger.getLogger(GraphResource.class);

    @Inject
    GraphService graphService;

    /**
     * Record a like action in the graph database.
     * When a user likes an itinerary in the frontend, this endpoint should be called
     * to create a LIKES relationship in the graph.
     *
     * @param likeAction The like action to record
     * @return Response indicating success or failure
     */
    @POST
    @Path("/likes")
    @Operation(
            summary = "Record a like action",
            description = "Creates a LIKES relationship between a user and an itinerary in the graph database"
    )
    @APIResponse(responseCode = "201", description = "Like recorded successfully")
    @APIResponse(responseCode = "400", description = "Invalid request data")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response recordLike(LikeActionDTO likeAction) {
        LOG.infof("Recording like: User %s likes Itinerary %d", likeAction.getUserId(), likeAction.getItineraryId());

        if (likeAction.getUserId() == null || likeAction.getUserId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User ID is required\"}")
                    .build();
        }

        if (likeAction.getItineraryId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        try {
            graphService.recordLike(likeAction);
            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Like recorded successfully\"}")
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error recording like");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to record like\"}")
                    .build();
        }
    }

    /**
     * Remove a like action from the graph database.
     * When a user unlikes an itinerary in the frontend, this endpoint should be called
     * to remove the LIKES relationship from the graph.
     *
     * @param likeAction The like action to remove
     * @return Response indicating success or failure
     */
    @DELETE
    @Path("/likes")
    @Operation(
            summary = "Remove a like action",
            description = "Removes a LIKES relationship between a user and an itinerary from the graph database"
    )
    @APIResponse(responseCode = "200", description = "Like removed successfully")
    @APIResponse(responseCode = "400", description = "Invalid request data")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response removeLike(LikeActionDTO likeAction) {
        LOG.infof("Removing like: User %s unlikes Itinerary %d", likeAction.getUserId(), likeAction.getItineraryId());

        if (likeAction.getUserId() == null || likeAction.getUserId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User ID is required\"}")
                    .build();
        }

        if (likeAction.getItineraryId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        try {
            graphService.removeLike(likeAction);
            return Response.ok()
                    .entity("{\"message\": \"Like removed successfully\"}")
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error removing like");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to remove like\"}")
                    .build();
        }
    }

    /**
     * Record an itinerary creation or update in the graph database.
     * When a user creates or modifies an itinerary, this endpoint should be called
     * to update the graph with the itinerary information and its locations.
     *
     * @param itineraryEvent The itinerary event to record
     * @return Response indicating success or failure
     */
    @POST
    @Path("/itineraries")
    @Operation(
            summary = "Record an itinerary event",
            description = "Creates or updates an itinerary node and its relationships in the graph database"
    )
    @APIResponse(responseCode = "201", description = "Itinerary recorded successfully")
    @APIResponse(responseCode = "400", description = "Invalid request data")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response recordItinerary(ItineraryEventDTO itineraryEvent) {
        LOG.infof("Recording itinerary: %d by user %s", itineraryEvent.getItineraryId(), itineraryEvent.getUserId());

        if (itineraryEvent.getUserId() == null || itineraryEvent.getUserId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User ID is required\"}")
                    .build();
        }

        if (itineraryEvent.getItineraryId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        if (itineraryEvent.getLocationNames() == null || itineraryEvent.getLocationNames().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"At least one location is required\"}")
                    .build();
        }

        try {
            graphService.recordItinerary(itineraryEvent);
            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Itinerary recorded successfully\"}")
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error recording itinerary");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to record itinerary\"}")
                    .build();
        }
    }

    /**
     * Record location visits in the graph database.
     * When a user adds locations to an itinerary, this endpoint should be called
     * to create VISITED relationships between the user and the locations.
     *
     * @param locationVisit The location visit data to record
     * @return Response indicating success or failure
     */
    @POST
    @Path("/locations/visits")
    @Operation(
            summary = "Record location visits",
            description = "Creates VISITED relationships between a user and locations in the graph database"
    )
    @APIResponse(responseCode = "201", description = "Location visits recorded successfully")
    @APIResponse(responseCode = "400", description = "Invalid request data")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response recordLocationVisits(LocationVisitDTO locationVisit) {
        LOG.infof("Recording location visits for user %s", locationVisit.getUserId());

        if (locationVisit.getUserId() == null || locationVisit.getUserId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User ID is required\"}")
                    .build();
        }

        if (locationVisit.getLocationNames() == null || locationVisit.getLocationNames().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"At least one location is required\"}")
                    .build();
        }

        try {
            graphService.recordLocationVisits(locationVisit);
            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Location visits recorded successfully\"}")
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error recording location visits");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to record location visits\"}")
                    .build();
        }
    }
}

