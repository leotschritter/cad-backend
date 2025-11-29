package de.htwg.api;

import de.htwg.dto.FeedResponseDTO;
import de.htwg.security.Authenticated;
import de.htwg.security.SecurityContext;
import de.htwg.service.RecommendationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST API for personalized travel feed recommendations.
 * Implements the Personalized Live Feed Epic user stories.
 * All endpoints require authentication via Google Cloud Identity Platform.
 */
@Path("/feed")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Feed", description = "Personalized travel feed recommendations")
@SecurityRequirement(name = "BearerAuth")
@Authenticated
public class FeedResource {

    private static final Logger LOG = Logger.getLogger(FeedResource.class);

    @Inject
    RecommendationService recommendationService;

    @Inject
    SecurityContext securityContext;

    /**
     * Get personalized feed for the authenticated traveller.
     * Story 1: See and Explore Suggestions on Feed Page
     * Story 2: Discover Itineraries from Travellers Who Visited the Same Places
     * Story 3: Refine Recommendation Algorithm with Social Signals and Basic Feed
     *
     * @return Feed response with recommended itineraries (max items configured in application.properties)
     */
    @GET
    @Operation(
            summary = "Get personalized feed",
            description = "Returns a personalized feed of itinerary recommendations based on the authenticated traveller's " +
                    "visited/planned destinations and social signals (likes). Falls back to basic feed for new users."
    )
    @APIResponse(responseCode = "200", description = "Feed retrieved successfully")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response getPersonalizedFeed() {
        String userEmail = securityContext.getCurrentUserEmail();

        if (userEmail == null || userEmail.isBlank()) {
            LOG.error("No authenticated user email found in security context");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Authentication required\"}")
                    .build();
        }

        LOG.infof("Getting personalized feed for user: %s", userEmail);

        try {
            FeedResponseDTO feed = recommendationService.getPersonalizedFeed(userEmail);
            return Response.ok(feed).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error getting personalized feed for user: %s", userEmail);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error retrieving feed\"}")
                    .build();
        }
    }

    /**
     * Get basic feed with most popular itineraries.
     * This is exposed as a separate endpoint but also serves as fallback in the main feed.
     *
     * @return Feed response with popular itineraries
     */
    @GET
    @Path("/popular")
    @Operation(
            summary = "Get popular feed",
            description = "Returns a feed of the most popular itineraries based on likes"
    )
    @APIResponse(responseCode = "200", description = "Popular feed retrieved successfully")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response getPopularFeed() {
        LOG.info("Getting popular feed");

        try {
            FeedResponseDTO feed = recommendationService.getPopularFeed();
            return Response.ok(feed).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error getting popular feed");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error retrieving popular feed\"}")
                    .build();
        }
    }
}

