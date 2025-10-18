package de.htwg.api.itinerary;


import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.api.itinerary.model.ItinerarySearchDto;
import de.htwg.api.itinerary.model.ItinerarySearchResponseDto;
import de.htwg.api.itinerary.service.ItineraryService;
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

@Path("/itinerary")
@Tag(name = "Itinerary Management", description = "Operations for managing travel itineraries")
public class ItineraryApi {

    private final ItineraryService itineraryService;

    @Inject
    public ItineraryApi(ItineraryService itineraryService) {

        this.itineraryService = itineraryService;
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create a new itinerary",
        description = "Creates a new travel itinerary for a specific user. The itinerary must include title, destination, start date, and descriptions."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Itinerary created successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"message\": \"Itinerary created successfully\"}"
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - User ID is required or invalid data provided",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User ID is required\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User with id 123 not found\"}"
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"An error occurred while creating the itinerary\"}"
            )
        )
    })
    public Response createItinerary(
        @RequestBody(
            description = "Itinerary details",
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ItineraryDto.class),
                examples = @ExampleObject(
                    name = "Family Trip Example",
                    summary = "Example of a family trip itinerary",
                    value = """
                        {
                          "title": "Family Trip to Norway",
                          "destination": "Norway",
                          "startDate": "2024-06-15",
                          "shortDescription": "Explore the fjords of southern Norway",
                          "detailedDescription": "A wonderful family trip to explore the beautiful fjords of southern Norway. We will visit Bergen, Stavanger, and the famous Geirangerfjord."
                        }
                        """
                )
            )
        ) final ItineraryDto itineraryDto,
        @Parameter(
            description = "ID of the user creating the itinerary",
            required = true,
            example = "1"
        ) @QueryParam("userId") Long userId) {

        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("User ID is required")
                    .build();
        }

        itineraryService.createItinerary(itineraryDto, userId);

        return Response.ok().build();
    }

    @POST
    @Path("/create/{email}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create a new itinerary",
            description = "Creates a new travel itinerary for a specific user. The itinerary must include title, destination, start date, and descriptions."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Itinerary created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"message\": \"Itinerary created successfully\"}"
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Bad request - User Email is required or invalid data provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"User Email is required\"}"
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"User with Email not found\"}"
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"An error occurred while creating the itinerary\"}"
                    )
            )
    })
    public Response createItineraryByEmail(
            @RequestBody(
                    description = "Itinerary details",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ItineraryDto.class),
                            examples = @ExampleObject(
                                    name = "Family Trip Example",
                                    summary = "Example of a family trip itinerary",
                                    value = """
                        {
                          "title": "Family Trip to Norway",
                          "destination": "Norway",
                          "startDate": "2024-06-15",
                          "shortDescription": "Explore the fjords of southern Norway",
                          "detailedDescription": "A wonderful family trip to explore the beautiful fjords of southern Norway. We will visit Bergen, Stavanger, and the famous Geirangerfjord."
                        }
                        """
                            )
                    )
            ) final ItineraryDto itineraryDto,
            @Parameter(
                    description = "Email of the user creating the itinerary",
                    required = true,
                    example = "pete.david@gmail.com"
            ) @PathParam("email") String email) {

        if (email == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("User ID is required")
                    .build();
        }

        itineraryService.createItineraryByEmail(itineraryDto, email);

        return Response.ok().build();
    }





    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get itineraries for a user",
        description = "Retrieves all itineraries associated with a specific user ID. Returns a list of itinerary details including title, destination, start date, and descriptions."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Itineraries retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ItineraryDto[].class),
                examples = @ExampleObject(
                    name = "Itinerary List Example",
                    summary = "Example of returned itinerary list",
                    value = """
                        [
                          {
                            "title": "Family Trip to Norway",
                            "destination": "Norway",
                            "startDate": "2024-06-15",
                            "shortDescription": "Explore the fjords of southern Norway",
                            "detailedDescription": "A wonderful family trip to explore the beautiful fjords of southern Norway. We will visit Bergen, Stavanger, and the famous Geirangerfjord."
                          },
                          {
                            "title": "Business Trip to Tokyo",
                            "destination": "Japan",
                            "startDate": "2024-07-20",
                            "shortDescription": "Corporate meetings and cultural exploration",
                            "detailedDescription": "A business trip combining work meetings with cultural experiences in Tokyo, including visits to traditional temples and modern districts."
                          }
                        ]
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - User ID is required",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"User ID is required\"}"
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"An error occurred while retrieving itineraries\"}"
            )
        )
    })
    public Response getItinerary(
        @Parameter(
            description = "ID of the user whose itineraries to retrieve",
            required = true,
            example = "1"
        ) @QueryParam("userId") Long userId) {

        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("User ID is required")
                    .build();
        }

        final List<ItineraryDto> itineraryDtos = itineraryService.getItinerariesByUserId(userId);

        return Response.ok(itineraryDtos).build();
    }

    @GET
    @Path("/get/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get itineraries for a user",
            description = "Retrieves all itineraries associated with a specific user Email. Returns a list of itinerary details including title, destination, start date, and descriptions."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Itineraries retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ItineraryDto[].class),
                            examples = @ExampleObject(
                                    name = "Itinerary List Example",
                                    summary = "Example of returned itinerary list",
                                    value = """
                        [
                          {
                            "title": "Family Trip to Norway",
                            "destination": "Norway",
                            "startDate": "2024-06-15",
                            "shortDescription": "Explore the fjords of southern Norway",
                            "detailedDescription": "A wonderful family trip to explore the beautiful fjords of southern Norway. We will visit Bergen, Stavanger, and the famous Geirangerfjord."
                          },
                          {
                            "title": "Business Trip to Tokyo",
                            "destination": "Japan",
                            "startDate": "2024-07-20",
                            "shortDescription": "Corporate meetings and cultural exploration",
                            "detailedDescription": "A business trip combining work meetings with cultural experiences in Tokyo, including visits to traditional temples and modern districts."
                          }
                        ]
                        """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Bad request - User ID is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"User ID is required\"}"
                    )
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"An error occurred while retrieving itineraries\"}"
                    )
            )
    })
    public Response getItineraryByEmail(
        @Parameter(
                description = "Email of the user whose itineraries to retrieve",
                required = true,
                example = "xyz@gmail.com"
        ) @PathParam("email") String email) {
        if (email == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Email parameter is required")
                    .build();
        }

        final List<ItineraryDto> itineraryDtos = itineraryService.getItinerariesByEmail(email);

        return Response.ok(itineraryDtos).build();
    }

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Search itineraries",
        description = "Search for itineraries based on various criteria including user name, user email, title, destination, description, and start date range. All search parameters are optional - empty/null values will be ignored."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ItinerarySearchResponseDto[].class),
                examples = @ExampleObject(
                    name = "Search Results Example",
                    summary = "Example of search results",
                    value = """
                        [
                          {
                            "title": "Summer in Paris",
                            "destination": "France",
                            "startDate": "2025-07-01",
                            "shortDescription": "Romantic getaway in Paris",
                            "detailedDescription": "A week-long romantic trip exploring the city of lights, including visits to the Eiffel Tower, Louvre Museum, and Seine river cruises.",
                            "userName": "Alice Johnson"
                          },
                          {
                            "title": "Beach Vacation in Maldives",
                            "destination": "Maldives",
                            "startDate": "2025-08-15",
                            "shortDescription": "Tropical paradise retreat",
                            "detailedDescription": "Relaxing beach vacation in an overwater bungalow with snorkeling, diving, and spa treatments.",
                            "userName": "Bob Williams"
                          }
                        ]
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"An error occurred while searching itineraries\"}"
            )
        )
    })
    public Response searchItineraries(
        @RequestBody(
            description = "Search criteria - all fields are optional",
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ItinerarySearchDto.class),
                examples = {
                    @ExampleObject(
                        name = "Search by destination",
                        summary = "Search for all itineraries to Norway",
                        value = """
                            {
                              "destination": "Norway"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Search by user and date range",
                        summary = "Search for user's itineraries in a specific date range",
                        value = """
                            {
                              "userName": "John",
                              "startDateFrom": "2025-06-01",
                              "startDateTo": "2025-12-31"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Complex search",
                        summary = "Search with multiple criteria",
                        value = """
                            {
                              "userName": "Smith",
                              "destination": "Japan",
                              "description": "culture",
                              "startDateFrom": "2025-01-01"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Empty search",
                        summary = "Get all itineraries (up to 100)",
                        value = "{}"
                    )
                }
            )
        ) final ItinerarySearchDto searchDto) {

        final List<ItinerarySearchResponseDto> itineraryDtos = itineraryService.searchItineraries(searchDto);

        return Response.ok(itineraryDtos).build();
    }

}
