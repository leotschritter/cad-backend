package de.htwg.api.comment;

import de.htwg.api.comment.model.MessageResponseDto;
import de.htwg.api.itinerary.model.CommentDto;
import de.htwg.service.firestore.CommentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/comment")
@Tag(name = "Comment Management", description = "Operations for managing comments on itineraries")
public class CommentApi {

    private final CommentService commentService;

    @Inject
    public CommentApi(CommentService commentService) {
        this.commentService = commentService;
    }

    @POST
    @Path("/itinerary/{itineraryId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Add comment to itinerary",
        description = "Adds a comment to an itinerary. Users can add multiple comments."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Comment added successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CommentDto.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - Missing required fields or empty comment",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Comment text cannot be empty\"}"
            )
        )
    })
    public Response addComment(
        @Parameter(
            description = "ID of the itinerary to comment on",
            required = true,
            example = "1"
        ) @PathParam("itineraryId") Long itineraryId,
        final CommentRequest request) {

        if (itineraryId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        if (request == null || request.userEmail == null || request.userEmail.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User email is required\"}")
                    .build();
        }

        if (request.comment == null || request.comment.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Comment text cannot be empty\"}")
                    .build();
        }

        try {
            CommentDto createdComment = commentService.addComment(request.userEmail, itineraryId, request.comment);
            return Response.ok(createdComment).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while adding the comment\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{commentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete comment",
        description = "Deletes a comment. Users can only delete their own comments."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Comment deleted successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = MessageResponseDto.class),
                examples = @ExampleObject(
                    name = "Delete Success",
                    summary = "Example of successful deletion",
                    value = """
                        {
                          "message": "Comment deleted successfully"
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - Missing required fields or unauthorized",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"You can only delete your own comments\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Comment not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Comment not found\"}"
            )
        )
    })
    public Response deleteComment(
        @Parameter(
            description = "ID of the comment to delete",
            required = true,
            example = "abc123"
        ) @PathParam("commentId") String commentId,
        @Parameter(
            description = "Email of the user deleting the comment",
            required = true,
            example = "john.doe@example.com"
        ) @QueryParam("userEmail") String userEmail) {

        if (commentId == null || commentId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Comment ID is required\"}")
                    .build();
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User email is required\"}")
                    .build();
        }

        try {
            commentService.deleteComment(commentId, userEmail);
            MessageResponseDto response = new MessageResponseDto("Comment deleted successfully");
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while deleting the comment\"}")
                    .build();
        }
    }

    @GET
    @Path("/itinerary/{itineraryId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get comments for itinerary",
        description = "Retrieves all comments for a specific itinerary, ordered by creation date (newest first)."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Comments retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CommentDto[].class),
                examples = @ExampleObject(
                    name = "Comments response",
                    summary = "Example of comments response",
                    value = """
                        [
                          {
                            "id": "abc123",
                            "userEmail": "john@example.com",
                            "itineraryId": 1,
                            "comment": "Amazing trip! Would love to visit.",
                            "createdAt": "2024-06-15T10:30:00"
                          },
                          {
                            "id": "def456",
                            "userEmail": "jane@example.com",
                            "itineraryId": 1,
                            "comment": "Great photos!",
                            "createdAt": "2024-06-14T14:20:00"
                          }
                        ]
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
    public Response getCommentsForItinerary(
        @Parameter(
            description = "ID of the itinerary to get comments for",
            required = true,
            example = "1"
        ) @PathParam("itineraryId") Long itineraryId) {

        if (itineraryId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        try {
            List<CommentDto> comments = commentService.getCommentsForItinerary(itineraryId);
            return Response.ok(comments).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while retrieving comments\"}")
                    .build();
        }
    }

    @GET
    @Path("/user/{userEmail}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get comments by user",
        description = "Retrieves all comments made by a specific user, ordered by creation date (newest first)."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "User comments retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CommentDto[].class)
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
    public Response getCommentsByUser(
        @Parameter(
            description = "Email of the user to get comments for",
            required = true,
            example = "john.doe@example.com"
        ) @PathParam("userEmail") String userEmail) {

        if (userEmail == null || userEmail.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"User email is required\"}")
                    .build();
        }

        try {
            List<CommentDto> comments = commentService.getCommentsByUser(userEmail);
            return Response.ok(comments).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while retrieving user comments\"}")
                    .build();
        }
    }

    // Request DTO for adding comments
    public static class CommentRequest {
        public String userEmail;
        public String comment;
    }
}

