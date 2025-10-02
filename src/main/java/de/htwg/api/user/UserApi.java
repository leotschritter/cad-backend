package de.htwg.api.user;

import de.htwg.api.user.model.UserDto;
import de.htwg.api.user.service.UserService;
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

@Path("/user")
@Tag(name = "User Management", description = "Operations for managing user accounts and profiles")
public class UserApi {

    private final UserService userService;

    @Inject
    public UserApi(UserService userService) {
        this.userService = userService;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with name and email. No password authentication is required. Email must be unique."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "User registered successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "Registered User Example",
                    summary = "Example of a successfully registered user",
                    value = """
                        {
                          "name": "John Doe",
                          "email": "john.doe@example.com"
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - User already exists or invalid data provided",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = {
                    @ExampleObject(
                        name = "Duplicate Email",
                        summary = "User already exists",
                        value = "{\"error\": \"User with email john.doe@example.com already exists\"}"
                    ),
                    @ExampleObject(
                        name = "Invalid Data",
                        summary = "Missing required fields",
                        value = "{\"error\": \"Name and email are required\"}"
                    )
                }
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"An error occurred while registering the user\"}"
            )
        )
    })
    public Response registerUser(
        @RequestBody(
            description = "User registration details",
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "Registration Example",
                    summary = "Example of user registration data",
                    value = """
                        {
                          "name": "John Doe",
                          "email": "john.doe@example.com"
                        }
                        """
                )
            )
        ) final UserDto userDto) {
        try {
            UserDto registeredUser = userService.registerUser(userDto);
            return Response.ok(registeredUser).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while registering the user")
                    .build();
        }
    }

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get user by email",
        description = "Retrieves user information by email address. Returns user details including name and email."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "User found successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "User Found Example",
                    summary = "Example of a found user",
                    value = """
                        {
                          "name": "John Doe",
                          "email": "john.doe@example.com"
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - Email parameter is required",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Email parameter is required\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User with email john.doe@example.com not found\"}"
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"An error occurred while retrieving the user\"}"
            )
        )
    })
    public Response getUserByEmail(
        @Parameter(
            description = "Email address of the user to retrieve",
            required = true,
            example = "john.doe@example.com"
        ) @QueryParam("email") String email) {
        if (email == null || email.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Email parameter is required")
                    .build();
        }

        try {
            UserDto user = userService.getUserByEmail(email);
            return Response.ok(user).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while retrieving the user")
                    .build();
        }
    }
}
