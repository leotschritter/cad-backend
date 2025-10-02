package de.htwg.api.user;

import de.htwg.api.user.model.UserDto;
import de.htwg.api.user.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@Path("/user")
public class UserApi {

    private final UserService userService;

    @Inject
    public UserApi(UserService userService) {
        this.userService = userService;
    }

    @POST
    @Path("/register")
    public Response registerUser(@RequestBody final UserDto userDto) {
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
    public Response getUserByEmail(@QueryParam("email") String email) {
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
