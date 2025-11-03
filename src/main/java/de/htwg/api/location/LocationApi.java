package de.htwg.api.location;

import de.htwg.api.itinerary.model.LocationDto;
import de.htwg.api.location.model.AccommodationDto;
import de.htwg.api.location.model.LocationImageUploadResponseDto;
import de.htwg.api.location.model.MessageResponseDto;
import de.htwg.api.location.model.TransportDto;
import de.htwg.api.location.service.LocationService;
import de.htwg.service.storage.ImageStorageService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    @Transactional
    @Operation(
        summary = "Add location to itinerary",
        description = "Adds a new location to an existing itinerary. Upload images directly as multipart form data (optional). Images will be stored in Google Cloud Storage."
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
        @Parameter(
            description = "Image files to upload (optional - can be omitted)"
        ) @FormParam("files") List<FileUpload> files,
        @Parameter(
            description = "Transport type (optional)"
        ) @FormParam("transportType") String transportType,
        @Parameter(
            description = "Transport duration in minutes (optional)"
        ) @FormParam("transportDuration") Long transportDuration,
        @Parameter(
            description = "Transport distance in kilometers (optional)"
        ) @FormParam("transportDistance") Long transportDistance,
        @Parameter(
            description = "Accommodation name (optional)"
        ) @FormParam("accommodationName") String accommodationName,
        @Parameter(
            description = "Accommodation price per night (optional)"
        ) @FormParam("accommodationPricePerNight") Double accommodationPricePerNight,
        @Parameter(
            description = "Accommodation rating (optional)"
        ) @FormParam("accommodationRating") Float accommodationRating,
        @Parameter(
            description = "Accommodation notes (optional)"
        ) @FormParam("accommodationNotes") String accommodationNotes,
        @Parameter(
            description = "Accommodation image URL (optional)"
        ) @FormParam("accommodationImageUrl") String accommodationImageUrl,
        @Parameter(
            description = "Booking page URL (optional)"
        ) @FormParam("bookingPageUrl") String bookingPageUrl) {

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
            List<String> imageFileNames = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                for (FileUpload file : files) {
                    if (file != null && file.fileName() != null) {
                        String fileName = "location-images/" + itineraryId + "/" + 
                                        System.currentTimeMillis() + "_" + file.fileName();
                        String uploadedFileName = imageStorageService.uploadImage(
                            new java.io.FileInputStream(file.uploadedFile().toFile()),
                            fileName,
                            file.contentType()
                        );
                        imageFileNames.add(uploadedFileName);
                    }
                }
            }

            // Create location with uploaded image filenames (stored in DB)
            LocationDto locationDto = LocationDto.builder()
                    .name(name)
                    .description(description)
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .imageUrls(imageFileNames)
                    .build();

            LocationDto createdLocation = locationService.addLocationToItinerary(itineraryId, locationDto);

            // Add transport if provided
            if (!(transportType == null && transportDuration == null && transportDistance == null)) {
                TransportDto transportDto = TransportDto.builder()
                        .transportType(transportType)
                        .duration(transportDuration)
                        .distance(transportDistance)
                        .build();
                locationService.addTransportToLocation(createdLocation.id(), transportDto);
            }

            // Add accommodation if provided
            if (!(accommodationName == null && accommodationPricePerNight == null && accommodationRating == null && accommodationNotes == null && accommodationImageUrl == null && bookingPageUrl == null)) {
                AccommodationDto accommodationDto = AccommodationDto.builder()
                        .name(accommodationName)
                        .pricePerNight(accommodationPricePerNight)
                        .rating(accommodationRating)
                        .notes(accommodationNotes)
                        .accommodationImageUrl(accommodationImageUrl)
                        .bookingPageUrl(bookingPageUrl)
                        .build();
                locationService.addAccommodationToLocation(createdLocation.id(), accommodationDto);
            }

            // Convert filenames to signed URLs for the response
            List<String> signedUrls = createdLocation.imageUrls() != null
                    ? createdLocation.imageUrls().stream()
                        .map(imageStorageService::getImageUrl)
                        .toList()
                    : List.of();

            LocationDto responseLocation = LocationDto.builder()
                    .id(createdLocation.id())
                    .name(createdLocation.name())
                    .description(createdLocation.description())
                    .fromDate(createdLocation.fromDate())
                    .toDate(createdLocation.toDate())
                    .imageUrls(signedUrls)
                    .build();

            return Response.ok(responseLocation).build();
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
        description = "Retrieves all locations for a specific itinerary, including associated transport and accommodation information."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Locations retrieved successfully with transport and accommodation details",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = LocationDto[].class),
                examples = @ExampleObject(
                    name = "Locations List",
                    summary = "Example list of locations with transport and accommodation",
                    value = """
                        [
                          {
                            "id": 1,
                            "name": "Bergen",
                            "description": "Historic coastal city",
                            "fromDate": "2024-06-15",
                            "toDate": "2024-06-18",
                            "imageUrls": ["https://example.com/bergen.jpg"],
                            "transportDto": {
                              "id": 1,
                              "transportType": "Flight",
                              "duration": 120,
                              "distance": 450
                            },
                            "accommodationDto": {
                              "id": 1,
                              "name": "Bergen Boutique Hotel",
                              "pricePerNight": 150.0,
                              "rating": 4.5,
                              "notes": "Great location near the harbor",
                              "accommodationImageUrl": "https://example.com/hotel.jpg",
                              "bookingPageUrl": "https://example.com/hotel.html"
                            }
                          },
                          {
                            "id": 2,
                            "name": "Oslo",
                            "description": "Capital city",
                            "fromDate": "2024-06-19",
                            "toDate": "2024-06-22",
                            "imageUrls": [],
                            "transportDto": null,
                            "accommodationDto": null
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

            // Convert filenames to signed URLs for each location
            List<LocationDto> locationsWithSignedUrls = locations.stream()
                    .map(location -> {
                        List<String> signedUrls = location.imageUrls() != null
                            ? location.imageUrls().stream()
                                .map(imageStorageService::getImageUrl)
                                .toList()
                            : List.of();

                        AccommodationDto accommodationDto = locationService.getAccommodationByLocationId(location.id());
                        TransportDto transportDto = locationService.getTransportByLocationId(location.id());

                        return LocationDto.builder()
                                .id(location.id())
                                .name(location.name())
                                .description(location.description())
                                .fromDate(location.fromDate())
                                .toDate(location.toDate())
                                .imageUrls(signedUrls)
                                .accommodationDto(accommodationDto)
                                .transportDto(transportDto)
                                .build();
                    })
                    .toList();

            return Response.ok(locationsWithSignedUrls).build();
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

            // Convert filenames to signed URLs
            List<String> signedUrls = location.imageUrls() != null
                ? location.imageUrls().stream()
                    .map(imageStorageService::getImageUrl)
                    .toList()
                : List.of();

            LocationDto locationWithSignedUrls = LocationDto.builder()
                    .id(location.id())
                    .name(location.name())
                    .description(location.description())
                    .fromDate(location.fromDate())
                    .toDate(location.toDate())
                    .imageUrls(signedUrls)
                    .build();

            return Response.ok(locationWithSignedUrls).build();
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
                schema = @Schema(implementation = MessageResponseDto.class),
                examples = @ExampleObject(
                    name = "Delete Success",
                    summary = "Example of successful deletion",
                    value = """
                        {
                          "message": "Location deleted successfully"
                        }
                        """
                )
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
            MessageResponseDto response = new MessageResponseDto("Location deleted successfully");
            return Response.ok(response).build();
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
                schema = @Schema(implementation = LocationImageUploadResponseDto.class),
                examples = @ExampleObject(
                    name = "Upload response",
                    summary = "Example of successful image upload",
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

            // Upload images and collect filenames
            List<String> imageFileNames = new ArrayList<>();
            for (FileUpload file : files) {
                if (file != null && file.fileName() != null) {
                    String fileName = "location-images/" + locationId + "/" + 
                                    System.currentTimeMillis() + "_" + file.fileName();
                    String uploadedFileName = imageStorageService.uploadImage(
                        new java.io.FileInputStream(file.uploadedFile().toFile()),
                        fileName,
                        file.contentType()
                    );
                    imageFileNames.add(uploadedFileName);
                }
            }

            // Add image filenames to location (stored in DB)
            locationService.addImagesToLocation(locationId, imageFileNames);

            // Convert filenames to signed URLs for the response
            List<String> signedUrls = imageFileNames.stream()
                    .map(imageStorageService::getImageUrl)
                    .toList();

            LocationImageUploadResponseDto response = new LocationImageUploadResponseDto(
                signedUrls.size() + " images uploaded successfully",
                signedUrls
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
                schema = @Schema(implementation = MessageResponseDto.class),
                examples = @ExampleObject(
                    name = "Delete Success",
                    summary = "Example of successful image deletion",
                    value = """
                        {
                          "message": "Image deleted successfully"
                        }
                        """
                )
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
            // imageUrl is actually a filename, not a URL
            imageStorageService.deleteImage(imageUrl);

            // Remove filename from location record
            locationService.removeImageFromLocation(locationId, imageUrl);

            MessageResponseDto response = new MessageResponseDto("Image deleted successfully");
            return Response.ok(response).build();
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

