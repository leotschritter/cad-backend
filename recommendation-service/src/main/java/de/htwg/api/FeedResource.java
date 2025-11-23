package de.htwg.api;

import de.htwg.dto.FeedResponseDTO;
import de.htwg.service.RecommendationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST API for personalized travel feed recommendations.
 * Implements the Personalized Live Feed Epic user stories.
 */
@Path("/api/v1/feed")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Feed", description = "Personalized travel feed recommendations")
public class FeedResource {

    private static final Logger LOG = Logger.getLogger(FeedResource.class);

    @Inject
    RecommendationService recommendationService;

    /**
     * Get personalized feed for a traveller.
     * Story 1: See and Explore Suggestions on Feed Page
     * Story 2: Discover Itineraries from Travellers Who Visited the Same Places
     * Story 3: Refine Recommendation Algorithm with Social Signals and Basic Feed
     *
     * @param travellerId The ID of the traveller requesting the feed
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return Paginated feed response with recommended itineraries
     */
    @GET
    @Operation(
            summary = "Get personalized feed",
            description = "Returns a personalized feed of itinerary recommendations based on the traveller's " +
                    "visited/planned destinations and social signals (likes). Falls back to basic feed for new users."
    )
    @APIResponse(responseCode = "200", description = "Feed retrieved successfully")
    @APIResponse(responseCode = "400", description = "Invalid request parameters")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response getPersonalizedFeed(
            @Parameter(description = "Traveller ID", required = true)
            @QueryParam("travellerId") String travellerId,
            
            @Parameter(description = "Page number (0-based)", required = false)
            @QueryParam("page") @DefaultValue("0") Integer page,
            
            @Parameter(description = "Page size", required = false)
            @QueryParam("pageSize") @DefaultValue("20") Integer pageSize
    ) {
        LOG.infof("Getting personalized feed for traveller: %s, page: %d, pageSize: %d", 
                travellerId, page, pageSize);

        if (travellerId == null || travellerId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Traveller ID is required")
                    .build();
        }

        if (page < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Page must be non-negative")
                    .build();
        }

        if (pageSize < 1 || pageSize > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Page size must be between 1 and 100")
                    .build();
        }

        try {
            FeedResponseDTO feed = recommendationService.getPersonalizedFeed(travellerId, page, pageSize);
            return Response.ok(feed).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error getting personalized feed for traveller: %s", travellerId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving feed")
                    .build();
        }
    }

    /**
     * Get basic feed with most popular itineraries.
     * This is exposed as a separate endpoint but also serves as fallback in the main feed.
     *
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return Paginated feed response with most liked itineraries
     */
    @GET
    @Path("/popular")
    @Operation(
            summary = "Get popular feed",
            description = "Returns a feed of the most popular itineraries based on likes"
    )
    @APIResponse(responseCode = "200", description = "Popular feed retrieved successfully")
    @APIResponse(responseCode = "400", description = "Invalid request parameters")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response getPopularFeed(
            @Parameter(description = "Page number (0-based)", required = false)
            @QueryParam("page") @DefaultValue("0") Integer page,
            
            @Parameter(description = "Page size", required = false)
            @QueryParam("pageSize") @DefaultValue("20") Integer pageSize
    ) {
        LOG.infof("Getting popular feed, page: %d, pageSize: %d", page, pageSize);

        if (page < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Page must be non-negative")
                    .build();
        }

        if (pageSize < 1 || pageSize > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Page size must be between 1 and 100")
                    .build();
        }

        try {
            FeedResponseDTO feed = recommendationService.getPopularFeed(page, pageSize);
            return Response.ok(feed).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error getting popular feed");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving popular feed")
                    .build();
        }
    }
}

