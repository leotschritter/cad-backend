package de.htwg.api.like;

import de.htwg.api.itinerary.model.LikeDto;
import de.htwg.api.itinerary.model.LikeResponseDto;
import de.htwg.service.firestore.LikeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/like")
@Tag(name = "Like Management", description = "Operations for managing likes and comments on itineraries")
public class LikeApi {

    private final LikeService likeService;

    @Inject
    public LikeApi(LikeService likeService) {
        this.likeService = likeService;
    }

    @POST
    @Path("/itinerary/{itineraryId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Like an itinerary",
        description = "Adds a like (with optional comment) to an itinerary. Users can only like once per itinerary."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Itinerary liked successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LikeDto.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - User already liked this itinerary",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User has already liked this itinerary\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Itinerary not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Itinerary not found\"}"
            )
        )
    })
    public Response likeItinerary(
        @Parameter(
            description = "ID of the itinerary to like",
            required = true,
            example = "1"
        ) @PathParam("itineraryId") Long itineraryId,
        @RequestBody(
            description = "Like details",
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LikeDto.class),
                examples = @ExampleObject(
                    name = "Like with comment",
                    summary = "Example of liking with a comment",
                    value = """
                        {
                          "userEmail": "john.doe@example.com",
                          "comment": "Amazing trip! Would love to visit this place."
                        }
                        """
                )
            )
        ) final LikeDto likeDto) {

        if (itineraryId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        if (likeDto.userEmail() == null || likeDto.userEmail().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User email is required\"}")
                    .build();
        }

        try {
            LikeDto createdLike = likeService.addLike(likeDto.userEmail(), itineraryId, likeDto.comment());
            return Response.ok(createdLike).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while liking the itinerary\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/itinerary/{itineraryId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Remove like from itinerary",
        description = "Removes a like from an itinerary."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Like removed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"message\": \"Like removed successfully\"}"
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - User email is required",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User email is required\"}"
            )
        )
    })
    public Response removeLikeFromItinerary(
        @Parameter(
            description = "ID of the itinerary to remove like from",
            required = true,
            example = "1"
        ) @PathParam("itineraryId") Long itineraryId,
        @Parameter(
            description = "Email of the user removing the like",
            required = true,
            example = "john.doe@example.com"
        ) @QueryParam("userEmail") String userEmail) {

        if (itineraryId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User email is required\"}")
                    .build();
        }

        try {
            likeService.removeLike(userEmail, itineraryId);
            return Response.ok()
                    .entity("{\"message\": \"Like removed successfully\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while removing the like\"}")
                    .build();
        }
    }

    @GET
    @Path("/itinerary/{itineraryId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get likes for itinerary",
        description = "Retrieves all likes and comments for a specific itinerary."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Likes retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LikeResponseDto.class),
                examples = @ExampleObject(
                    name = "Likes response",
                    summary = "Example of likes response",
                    value = """
                        {
                          "itineraryId": 1,
                          "likeCount": 2,
                          "likes": [
                            {
                              "id": "abc123",
                              "userEmail": "john@example.com",
                              "itineraryId": 1,
                              "comment": "Amazing trip!",
                              "createdAt": "2024-06-15T10:30:00"
                            },
                            {
                              "id": "def456",
                              "userEmail": "jane@example.com",
                              "itineraryId": 1,
                              "comment": null,
                              "createdAt": "2024-06-16T14:20:00"
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - Itinerary ID is required",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Itinerary ID is required\"}"
            )
        )
    })
    public Response getLikesForItinerary(
        @Parameter(
            description = "ID of the itinerary to get likes for",
            required = true,
            example = "1"
        ) @PathParam("itineraryId") Long itineraryId) {

        if (itineraryId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        try {
            LikeResponseDto likes = likeService.getLikesForItinerary(itineraryId);
            return Response.ok(likes).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while retrieving likes\"}")
                    .build();
        }
    }

    @GET
    @Path("/user/{userEmail}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get likes by user",
        description = "Retrieves all likes made by a specific user."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "User likes retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LikeDto[].class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - User email is required",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User email is required\"}"
            )
        )
    })
    public Response getLikesByUser(
        @Parameter(
            description = "Email of the user to get likes for",
            required = true,
            example = "john.doe@example.com"
        ) @PathParam("userEmail") String userEmail) {

        if (userEmail == null || userEmail.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User email is required\"}")
                    .build();
        }

        try {
            List<LikeDto> likes = likeService.getLikesByUser(userEmail);
            return Response.ok(likes).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while retrieving user likes\"}")
                    .build();
        }
    }
}

