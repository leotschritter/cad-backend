package de.htwg.api.like;

import de.htwg.api.itinerary.model.LikeDto;
import de.htwg.api.itinerary.model.LikeResponseDto;
import de.htwg.api.like.model.MessageResponseDto;
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
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Like an itinerary",
        description = "Adds a like to an itinerary. Users can only like once per itinerary."
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
            description = "Bad request - User already liked this itinerary or missing user email",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User has already liked this itinerary\"}"
            )
        )
    })
    public Response likeItinerary(
        @Parameter(
            description = "ID of the itinerary to like",
            required = true,
            example = "1"
        ) @PathParam("itineraryId") Long itineraryId,
        @Parameter(
            description = "Email of the user liking the itinerary",
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
            LikeDto createdLike = likeService.addLike(userEmail, itineraryId);
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
                schema = @Schema(implementation = MessageResponseDto.class),
                examples = @ExampleObject(
                    name = "Like Removed",
                    summary = "Example of successful like removal",
                    value = """
                        {
                          "message": "Like removed successfully"
                        }
                        """
                )
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
            MessageResponseDto response = new MessageResponseDto("Like removed successfully");
            return Response.ok(response).build();
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
        summary = "Get like count for itinerary",
        description = "Retrieves the number of likes for a specific itinerary."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Like count retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LikeResponseDto.class),
                examples = @ExampleObject(
                    name = "Likes response",
                    summary = "Example of likes response",
                    value = """
                        {
                          "itineraryId": 1,
                          "likeCount": 5
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

