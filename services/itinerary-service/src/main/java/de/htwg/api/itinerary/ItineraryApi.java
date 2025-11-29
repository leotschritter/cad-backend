package de.htwg.api.itinerary;


import de.htwg.api.itinerary.model.ItineraryDto;
import de.htwg.api.itinerary.model.ItinerarySearchDto;
import de.htwg.api.itinerary.model.ItinerarySearchResponseDto;
import de.htwg.api.itinerary.service.ItineraryService;
import de.htwg.security.Authenticated;
import de.htwg.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/itinerary")
@Tag(name = "Itinerary Management", description = "Operations for managing travel itineraries")
public class ItineraryApi {

    private final ItineraryService itineraryService;

    @Inject
    SecurityContext securityContext;

    @Inject
    public ItineraryApi(ItineraryService itineraryService) {

        this.itineraryService = itineraryService;
    }


    @POST
    @Path("/create")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create a new itinerary",
            description = "Creates a new travel itinerary for the authenticated user. Requires authentication. The itinerary must include title, destination, start date, and descriptions."
    )
    @SecurityRequirement(name = "BearerAuth")
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
                    description = "Bad request - Invalid data provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"Invalid itinerary data\"}"
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
                            example = "{\"error\": \"User with email not found\"}"
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
            ) final ItineraryDto itineraryDto) {

        String email = securityContext.getCurrentUserEmail();
        itineraryService.createItineraryByEmail(itineraryDto, email);

        return Response.ok().build();
    }






    @GET
    @Path("/get")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get itineraries for authenticated user",
            description = "Retrieves all itineraries associated with the authenticated user. Requires authentication. Returns a list of itinerary details including title, destination, start date, and descriptions."
    )
    @SecurityRequirement(name = "BearerAuth")
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
                            "id": 1,
                            "title": "Family Trip to Norway",
                            "destination": "Norway",
                            "startDate": "2024-06-15",
                            "shortDescription": "Explore the fjords of southern Norway",
                            "detailedDescription": "A wonderful family trip to explore the beautiful fjords of southern Norway. We will visit Bergen, Stavanger, and the famous Geirangerfjord."
                          },
                          {
                            "id": 2,
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
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"Missing or invalid Authorization header\"}"
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
    public Response getItineraryByEmail() {
        String email = securityContext.getCurrentUserEmail();
        final List<ItineraryDto> itineraryDtos = itineraryService.getItinerariesByEmail(email);

        return Response.ok(itineraryDtos).build();
    }

    @POST
    @Path("/by-ids")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get itineraries by IDs",
        description = "Retrieves a list of itineraries by their IDs. Used by the recommendation service to fetch itinerary details. Requires authentication."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Itineraries retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ItineraryDto[].class),
                examples = @ExampleObject(
                    name = "Itineraries by IDs Example",
                    summary = "Example of returned itineraries",
                    value = """
                        [
                          {
                            "id": 1,
                            "title": "Family Trip to Norway",
                            "destination": "Norway",
                            "startDate": "2024-06-15",
                            "shortDescription": "Explore the fjords of southern Norway",
                            "detailedDescription": "A wonderful family trip to explore the beautiful fjords of southern Norway."
                          },
                          {
                            "id": 3,
                            "title": "Summer in Paris",
                            "destination": "France",
                            "startDate": "2025-07-01",
                            "shortDescription": "Romantic getaway in Paris",
                            "detailedDescription": "A week-long romantic trip exploring the city of lights."
                          }
                        ]
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
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"An error occurred while retrieving itineraries\"}"
            )
        )
    })
    public Response getItinerariesByIds(
        @RequestBody(
            description = "List of itinerary IDs",
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = @ExampleObject(
                    name = "ID List Example",
                    summary = "Example list of itinerary IDs",
                    value = "[1, 3, 5, 7, 9]"
                )
            )
        ) final List<Long> ids) {

        final List<ItineraryDto> itineraryDtos = itineraryService.getItinerariesByIds(ids);
        return Response.ok(itineraryDtos).build();
    }

    @POST
    @Path("/search")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Search itineraries",
        description = "Search for itineraries based on various criteria including user name, user email, title, destination, description, and start date range. All search parameters are optional - empty/null values will be ignored. Requires authentication."
    )
    @SecurityRequirement(name = "BearerAuth")
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
                            "id": 3,
                            "title": "Summer in Paris",
                            "destination": "France",
                            "startDate": "2025-07-01",
                            "shortDescription": "Romantic getaway in Paris",
                            "detailedDescription": "A week-long romantic trip exploring the city of lights, including visits to the Eiffel Tower, Louvre Museum, and Seine river cruises.",
                            "userName": "Alice Johnson"
                          },
                          {
                            "id": 5,
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
            responseCode = "401",
            description = "Unauthorized - Missing or invalid token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Missing or invalid Authorization header\"}"
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
