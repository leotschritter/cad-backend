package de.htwg.api.user;

import de.htwg.api.user.model.ProfileImageResponseDto;
import de.htwg.api.user.model.ProfileImageUploadResponseDto;
import de.htwg.api.user.model.UserDto;
import de.htwg.api.user.service.UserService;
import de.htwg.security.Authenticated;
import de.htwg.security.SecurityContext;
import de.htwg.service.storage.ImageStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.FileInputStream;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/user")
@Tag(name = "User Management", description = "Operations for managing user accounts and profiles")
public class UserApi {

    private final UserService userService;
    private final ImageStorageService imageStorageService;

    @Inject
    SecurityContext securityContext;

    @Inject
    public UserApi(UserService userService, ImageStorageService imageStorageService) {
        this.userService = userService;
        this.imageStorageService = imageStorageService;
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
                          "id": 1,
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
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "Registration Example",
                    summary = "Example of user registration data",
                    value = """
                        {
                          "id": 1,
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
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get user by email",
        description = "Retrieves user information by email address. Returns user details including name and email. Requires authentication."
    )
    @SecurityRequirement(name = "BearerAuth")
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
                          "id": 1,
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
            responseCode = "401",
            description = "Unauthorized - Missing or invalid token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Missing or invalid Authorization header\"}"
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

    @POST
    @Path("/profile-image")
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Upload profile image",
        description = "Uploads a profile image for the authenticated user. Requires authentication. The image will be stored in Google Cloud Storage."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Profile image uploaded successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ProfileImageUploadResponseDto.class),
                examples = @ExampleObject(
                    name = "Upload Success",
                    summary = "Example of successful upload response",
                    value = """
                        {
                          "message": "Profile image uploaded successfully",
                          "imageUrl": "https://storage.googleapis.com/bucket/profile-images/john.doe@example.com/1234567890_photo.jpg"
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - Invalid file",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Invalid file format\"}"
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Missing or invalid Authorization header\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User not found\"}"
            )
        )
    })
    public Response uploadProfileImage(
        @Parameter(
            description = "Image file to upload",
            required = true
        ) @FormParam("file") FileUpload file) {

        if (file == null || file.fileName() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("File is required")
                    .build();
        }

        try {
            String email = securityContext.getCurrentUserEmail();
            
            // Check if user already has a profile image and delete it
            try {
                String oldImageUrl = userService.getProfileImageUrl(email);
                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    // oldImageUrl is actually a filename, not a URL
                    imageStorageService.deleteImage(oldImageUrl);
                }
            } catch (Exception e) {
                // Log but don't fail if old image deletion fails
                System.err.println("Warning: Could not delete old profile image: " + e.getMessage());
            }

            // Upload new profile image
            String fileName = "profile-images/" + email + "/" + System.currentTimeMillis() + "_" + file.fileName();
            String uploadedFileName = imageStorageService.uploadImage(
                new FileInputStream(file.uploadedFile().toFile()),
                fileName,
                file.contentType()
            );

            // Store the filename in the database
            userService.updateProfileImage(email, uploadedFileName);

            // Get the signed URL for the response
            String signedUrl = imageStorageService.getImageUrl(uploadedFileName);

            ProfileImageUploadResponseDto response = new ProfileImageUploadResponseDto(
                "Profile image uploaded successfully",
                signedUrl
            );
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while uploading the profile image")
                    .build();
        }
    }

    @GET
    @Path("/profile-image")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get profile image URL",
        description = "Retrieves the profile image URL for the authenticated user. Requires authentication."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Profile image URL retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ProfileImageResponseDto.class),
                examples = @ExampleObject(
                    name = "Image URL Response",
                    summary = "Example of profile image URL response",
                    value = """
                        {
                          "imageUrl": "https://storage.googleapis.com/bucket/profile-images/john.doe@example.com/1234567890_photo.jpg"
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Missing or invalid Authorization header\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "User not found or no profile image",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User not found or no profile image\"}"
            )
        )
    })
    public Response getProfileImageUrl() {
        try {
            String email = securityContext.getCurrentUserEmail();
            String imageUrl = userService.getProfileImageUrl(email);
            if (imageUrl == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"No profile image found for user\"}")
                        .build();
            }
            ProfileImageResponseDto response = new ProfileImageResponseDto(imageUrl);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while retrieving the profile image")
                    .build();
        }
    }
}
