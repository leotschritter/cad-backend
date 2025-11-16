package de.htwg.travelwarnings.api.resource;

import de.htwg.travelwarnings.api.dto.ErrorResponse;
import de.htwg.travelwarnings.api.dto.TravelWarningDto;
import de.htwg.travelwarnings.api.dto.UserTripDto;
import de.htwg.travelwarnings.api.mapper.TravelWarningMapper;
import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import de.htwg.travelwarnings.persistence.entity.UserTrip;
import de.htwg.travelwarnings.persistence.repository.TravelWarningRepository;
import de.htwg.travelwarnings.persistence.repository.UserTripRepository;
import de.htwg.travelwarnings.service.AlertDispatcherService;
import de.htwg.travelwarnings.service.WarningMatcherService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API for managing user trips
 * Supports User Story 1: Manage trips to receive relevant alerts
 */
@Path("/api/v1/trips")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Trips", description = "Manage user trip itineraries for travel warning notifications")
public class UserTripResource {

    private static final Logger LOG = Logger.getLogger(UserTripResource.class);

    @Inject
    UserTripRepository tripRepository;

    @Inject
    TravelWarningRepository warningRepository;

    @Inject
    TravelWarningMapper mapper;

    @Inject
    WarningMatcherService matcherService;

    @Inject
    AlertDispatcherService alertDispatcher;

    /**
     * Get all trips for a user
     */
    @GET
    @Path("/user/{email}")
    @Operation(summary = "Get all trips for a user",
               description = "Returns all trips associated with a user email")
    public Response getUserTrips(@PathParam("email") String email) {
        List<UserTrip> trips = tripRepository.findByEmail(email);

        List<UserTripDto> dtos = trips.stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }

    /**
     * Get a specific trip
     */
    @GET
    @Path("/{tripId}")
    @Operation(summary = "Get trip by ID",
               description = "Returns a specific trip by its ID")
    public Response getTripById(@PathParam("tripId") Long tripId) {
        return tripRepository.findByIdOptional(tripId)
            .map(mapper::toDto)
            .map(dto -> Response.ok(dto).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse.notFound(
                    "Trip not found with ID: " + tripId,
                    "/api/v1/trips/" + tripId))
                .build());
    }

    /**
     * Create a new trip
     */
    @POST
    @Transactional
    @Operation(summary = "Create a new trip",
               description = "Creates a new trip for receiving travel warnings. " +
                           "If active warnings exist for the destination, an immediate notification is sent.")
    public Response createTrip(UserTripDto tripDto) {
        if (tripDto.getEmail() == null || tripDto.getCountryCode() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse.badRequest(
                    "email and countryCode are required",
                    "/api/v1/trips"))
                .build();
        }

        UserTrip trip = mapper.toEntity(tripDto);
        // Ensure ID is null for new entities to avoid "Detached entity" error
        trip.setId(null);
        tripRepository.persist(trip);

        LOG.infof("Created trip for user %s to %s", trip.getEmail(), trip.getCountryName());

        // Check for existing active warnings for the destination country
        checkAndNotifyActiveWarnings(trip);

        return Response.status(Response.Status.CREATED)
            .entity(mapper.toDto(trip))
            .build();
    }

    /**
     * Check if there are active warnings for the trip's destination and send immediate notification
     */
    private void checkAndNotifyActiveWarnings(UserTrip trip) {
        try {
            // Find active warnings for the country
            Optional<TravelWarning> activeWarningOpt = warningRepository.findByCountryCode(trip.getCountryCode());

            if (activeWarningOpt.isEmpty()) {
                LOG.debugf("No warnings found for country %s, no immediate notification needed", trip.getCountryCode());
                return;
            }

            TravelWarning warning = activeWarningOpt.get();

            // Check if there are any active warnings
            boolean hasActiveWarning = warning.getWarning() ||
                                      warning.getPartialWarning() ||
                                      warning.getSituationWarning() ||
                                      warning.getSituationPartWarning();

            if (hasActiveWarning && trip.getNotificationsEnabled()) {
                LOG.infof("🚨 Active warning detected for %s! Sending immediate notification to %s",
                         trip.getCountryCode(), trip.getEmail());

                // Send immediate alert
                boolean sent = alertDispatcher.sendAlert(warning, trip, trip.getEmail());

                if (sent) {
                    LOG.infof("✅ Immediate warning notification sent successfully to %s for trip to %s",
                             trip.getEmail(), trip.getCountryName());
                } else {
                    LOG.warnf("⚠️ Failed to send immediate warning notification to %s", trip.getEmail());
                }
            } else if (!hasActiveWarning) {
                LOG.debugf("✅ No active warnings for %s, user can travel safely", trip.getCountryCode());
            } else {
                LOG.debugf("Notifications disabled for trip, skipping immediate alert");
            }

        } catch (Exception e) {
            // Don't fail trip creation if notification fails
            LOG.errorf(e, "Error checking/sending active warning notification for trip to %s", trip.getCountryCode());
        }
    }

    /**
     * Update an existing trip
     */
    @PUT
    @Path("/{tripId}")
    @Transactional
    @Operation(summary = "Update a trip",
               description = "Updates an existing trip")
    public Response updateTrip(@PathParam("tripId") Long tripId, UserTripDto tripDto) {
        return tripRepository.findByIdOptional(tripId)
            .map(trip -> {
                if (tripDto.getCountryCode() != null) {
                    trip.setCountryCode(tripDto.getCountryCode());
                }
                if (tripDto.getCountryName() != null) {
                    trip.setCountryName(tripDto.getCountryName());
                }
                if (tripDto.getStartDate() != null) {
                    trip.setStartDate(tripDto.getStartDate());
                }
                if (tripDto.getEndDate() != null) {
                    trip.setEndDate(tripDto.getEndDate());
                }
                if (tripDto.getTripName() != null) {
                    trip.setTripName(tripDto.getTripName());
                }
                if (tripDto.getNotificationsEnabled() != null) {
                    trip.setNotificationsEnabled(tripDto.getNotificationsEnabled());
                }

                tripRepository.persist(trip);
                return Response.ok(mapper.toDto(trip)).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse.notFound(
                    "Trip not found with ID: " + tripId,
                    "/api/v1/trips/" + tripId))
                .build());
    }

    /**
     * Delete a trip
     */
    @DELETE
    @Path("/{tripId}")
    @Transactional
    @Operation(summary = "Delete a trip",
               description = "Deletes a trip and stops alerts for it")
    public Response deleteTrip(@PathParam("tripId") Long tripId) {
        boolean deleted = tripRepository.deleteById(tripId);

        if (deleted) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse.notFound(
                    "Trip not found with ID: " + tripId,
                    "/api/v1/trips/" + tripId))
                .build();
        }
    }

    /**
     * Get active warnings for a user's trips (User Story 1)
     */
    @GET
    @Path("/user/{email}/warnings")
    @Operation(summary = "Get warnings for user's trips",
               description = "Returns all active warnings that affect the user's trips")
    public Response getWarningsForUser(@PathParam("email") String email) {
        List<WarningMatcherService.WarningMatch> matches = matcherService.findWarningsForUser(email);

        List<TravelWarningDto> warnings = matches.stream()
            .map(WarningMatcherService.WarningMatch::getWarning)
            .map(mapper::toDto)
            .collect(Collectors.toList());

        return Response.ok(warnings).build();
    }

    /**
     * Toggle notifications for a trip
     */
    @PATCH
    @Path("/{tripId}/notifications")
    @Transactional
    @Operation(summary = "Toggle notifications",
               description = "Enable or disable notifications for a trip")
    public Response toggleNotifications(
            @PathParam("tripId") Long tripId,
            @QueryParam("enabled") @DefaultValue("true") boolean enabled) {

        return tripRepository.findByIdOptional(tripId)
            .map(trip -> {
                trip.setNotificationsEnabled(enabled);
                tripRepository.persist(trip);
                return Response.ok(mapper.toDto(trip)).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse.notFound(
                    "Trip not found with ID: " + tripId,
                    "/api/v1/trips/" + tripId + "/notifications"))
                .build());
    }
}

