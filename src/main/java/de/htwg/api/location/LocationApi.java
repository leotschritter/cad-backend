package de.htwg.api.location;

import de.htwg.api.itinerary.model.LocationDto;
import de.htwg.api.location.service.LocationService;
import de.htwg.service.storage.ImageStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Path("/location")
@Tag(name = "Location Management", description = "Operations for managing locations within itineraries")
public class LocationApi {

    private final LocationService locationService;
    private final ImageStorageService imageStorageService;

    @Inject
    public LocationApi(LocationService locationService, ImageStorageService imageStorageService) {
        this.locationService = locationService;
        this.imageStorageService = imageStorageService;
    }

    @POST
    @Path("/itinerary/{itineraryId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Add location to itinerary",
        description = "Adds a new location to an existing itinerary. Upload images directly as multipart form data. Images will be stored in Google Cloud Storage."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Location added successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LocationDto.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - Invalid data provided",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Location name is required\"}"
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
    public Response addLocationToItinerary(
        @Parameter(
            description = "ID of the itinerary to add location to",
            required = true,
            example = "1"
        ) @PathParam("itineraryId") Long itineraryId,
        @FormParam("name") String name,
        @FormParam("description") String description,
        @FormParam("fromDate") LocalDate fromDate,
        @FormParam("toDate") LocalDate toDate,
        @FormParam("files") List<FileUpload> files) {

        if (itineraryId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        if (name == null || name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Location name is required\"}")
                    .build();
        }

        try {
            // Upload images first
            List<String> imageUrls = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                for (FileUpload file : files) {
                    if (file != null && file.fileName() != null) {
                        String fileName = "location-images/" + itineraryId + "/" + 
                                        System.currentTimeMillis() + "_" + file.fileName();
                        String imageUrl = imageStorageService.uploadImage(
                            new java.io.FileInputStream(file.uploadedFile().toFile()),
                            fileName,
                            file.contentType()
                        );
                        imageUrls.add(imageUrl);
                    }
                }
            }

            // Create location with uploaded image URLs
            LocationDto locationDto = LocationDto.builder()
                    .name(name)
                    .description(description)
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .imageUrls(imageUrls)
                    .build();

            LocationDto createdLocation = locationService.addLocationToItinerary(itineraryId, locationDto);
            return Response.ok(createdLocation).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while adding the location: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/itinerary/{itineraryId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get locations for itinerary",
        description = "Retrieves all locations for a specific itinerary."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Locations retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LocationDto[].class),
                examples = @ExampleObject(
                    name = "Locations List",
                    summary = "Example list of locations",
                    value = """
                        [
                          {
                            "id": 1,
                            "name": "Bergen",
                            "description": "Historic coastal city",
                            "fromDate": "2024-06-15",
                            "toDate": "2024-06-18",
                            "imageUrls": ["https://example.com/bergen.jpg"]
                          },
                          {
                            "id": 2,
                            "name": "Oslo",
                            "description": "Capital city",
                            "fromDate": "2024-06-19",
                            "toDate": "2024-06-22",
                            "imageUrls": []
                          }
                        ]
                        """
                )
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
    public Response getLocationsForItinerary(
        @Parameter(
            description = "ID of the itinerary to get locations for",
            required = true,
            example = "1"
        ) @PathParam("itineraryId") Long itineraryId) {

        if (itineraryId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Itinerary ID is required\"}")
                    .build();
        }

        try {
            List<LocationDto> locations = locationService.getLocationsForItinerary(itineraryId);
            return Response.ok(locations).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while retrieving locations\"}")
                    .build();
        }
    }

    @GET
    @Path("/{locationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get location by ID",
        description = "Retrieves a specific location by its ID."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Location retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LocationDto.class)
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Location not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Location not found\"}"
            )
        )
    })
    public Response getLocationById(
        @Parameter(
            description = "ID of the location to retrieve",
            required = true,
            example = "1"
        ) @PathParam("locationId") Long locationId) {

        if (locationId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Location ID is required\"}")
                    .build();
        }

        try {
            LocationDto location = locationService.getLocationById(locationId);
            return Response.ok(location).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while retrieving the location\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{locationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete location",
        description = "Deletes a location from an itinerary."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Location deleted successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"message\": \"Location deleted successfully\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Location not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Location not found\"}"
            )
        )
    })

    public Response deleteLocation(
        @Parameter(
            description = "ID of the location to delete",
            required = true,
            example = "1"
        ) @PathParam("locationId") Long locationId) {

        if (locationId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Location ID is required\"}")
                    .build();
        }

        try {
            locationService.deleteLocation(locationId);
            return Response.ok()
                    .entity("{\"message\": \"Location deleted successfully\"}")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while deleting the location\"}")
                    .build();
        }
    }

    @POST
    @Path("/{locationId}/images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Upload images to location",
        description = "Uploads one or more images to a location. Images will be stored in Google Cloud Storage."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Images uploaded successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                examples = @ExampleObject(
                    name = "Upload response",
                    value = """
                        {
                          "message": "3 images uploaded successfully",
                          "imageUrls": [
                            "https://storage.googleapis.com/bucket/location-images/1/image1.jpg",
                            "https://storage.googleapis.com/bucket/location-images/1/image2.jpg",
                            "https://storage.googleapis.com/bucket/location-images/1/image3.jpg"
                          ]
                        }
                        """
                )
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - No images provided",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"At least one image file is required\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Location not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Location not found\"}"
            )
        )
    })
    public Response uploadImages(
        @Parameter(
            description = "ID of the location to upload images to",
            required = true,
            example = "1"
        ) @PathParam("locationId") Long locationId,
        @Parameter(
            description = "Image files to upload (can be multiple)",
            required = true
        ) @FormParam("files") List<FileUpload> files) {

        if (locationId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Location ID is required\"}")
                    .build();
        }

        if (files == null || files.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"At least one image file is required\"}")
                    .build();
        }

        try {
            // Verify location exists
            locationService.getLocationById(locationId);

            // Upload images and collect URLs
            List<String> imageUrls = new ArrayList<>();
            for (FileUpload file : files) {
                if (file != null && file.fileName() != null) {
                    String fileName = "location-images/" + locationId + "/" + 
                                    System.currentTimeMillis() + "_" + file.fileName();
                    String imageUrl = imageStorageService.uploadImage(
                        new java.io.FileInputStream(file.uploadedFile().toFile()),
                        fileName,
                        file.contentType()
                    );
                    imageUrls.add(imageUrl);
                }
            }

            // Add image URLs to location
            locationService.addImagesToLocation(locationId, imageUrls);

            String response = String.format(
                "{\"message\": \"%d images uploaded successfully\", \"imageUrls\": %s}",
                imageUrls.size(),
                imageUrls.stream()
                    .map(url -> "\"" + url + "\"")
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"))
            );

            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while uploading images: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{locationId}/images")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete image from location",
        description = "Deletes a specific image from a location by its URL."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Image deleted successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"message\": \"Image deleted successfully\"}"
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = "Bad request - Image URL is required",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Image URL is required\"}"
            )
        ),
        @APIResponse(
            responseCode = "404",
            description = "Location not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                example = "{\"error\": \"Location not found\"}"
            )
        )
    })
    public Response deleteImage(
        @Parameter(
            description = "ID of the location to delete image from",
            required = true,
            example = "1"
        ) @PathParam("locationId") Long locationId,
        @Parameter(
            description = "URL of the image to delete",
            required = true,
            example = "https://storage.googleapis.com/bucket/location-images/1/image1.jpg"
        ) @QueryParam("imageUrl") String imageUrl) {

        if (locationId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Location ID is required\"}")
                    .build();
        }

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Image URL is required\"}")
                    .build();
        }

        try {
            locationService.removeImageFromLocation(locationId, imageUrl);
            
            // Optionally delete from storage (extract filename from URL)
            // imageStorageService.deleteImage(extractFileName(imageUrl));

            return Response.ok()
                    .entity("{\"message\": \"Image deleted successfully\"}")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An error occurred while deleting the image\"}")
                    .build();
        }
    }
}

