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
@Path("/warnings/trips")
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
        LOG.infof("GET /warnings/trips/user/%s - Fetching all trips for user", email);

        List<UserTrip> trips = tripRepository.findByEmail(email);

        List<UserTripDto> dtos = trips.stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());

        LOG.infof("Returning %d trips for user: %s", dtos.size(), email);
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
        LOG.infof("GET /warnings/trips/%d - Fetching trip by ID", tripId);

        var result = tripRepository.findByIdOptional(tripId)
            .map(mapper::toDto)
            .map(dto -> {
                LOG.infof("Found trip ID %d: %s to %s", tripId, dto.getTripName(), dto.getCountryName());
                return Response.ok(dto).build();
            })
            .orElseGet(() -> {
                LOG.warnf("Trip not found with ID: %d", tripId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.notFound(
                        "Trip not found with ID: " + tripId,
                        "/warnings/trips/" + tripId))
                    .build();
            });

        return result;
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
        LOG.infof("POST /warnings/trips - Creating new trip for user: %s, destination: %s",
                  tripDto.getEmail(), tripDto.getCountryCode());

        if (tripDto.getEmail() == null || tripDto.getCountryCode() == null) {
            LOG.warnf("Bad request - Missing required fields: email=%s, countryCode=%s",
                     tripDto.getEmail(), tripDto.getCountryCode());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse.badRequest(
                    "email and countryCode are required",
                    "/warnings/trips"))
                .build();
        }

        UserTrip trip = mapper.toEntity(tripDto);
        // Ensure ID is null for new entities to avoid "Detached entity" error
        trip.setId(null);
        tripRepository.persist(trip);

        LOG.infof("✅ Trip created successfully - ID: %d, User: %s, Destination: %s (%s), Notifications: %s",
                 trip.getId(), trip.getEmail(), trip.getCountryName(), trip.getCountryCode(),
                 trip.getNotificationsEnabled());

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
        LOG.infof("🔍 Checking for active warnings for trip - Destination: %s (%s), User: %s",
                 trip.getCountryName(), trip.getCountryCode(), trip.getEmail());

        try {
            // Find active warnings for the country
            Optional<TravelWarning> activeWarningOpt = warningRepository.findByCountryCode(trip.getCountryCode());

            if (activeWarningOpt.isEmpty()) {
                LOG.infof("✅ No warnings found for country %s - User can travel safely, no notification needed",
                         trip.getCountryCode());
                return;
            }

            TravelWarning warning = activeWarningOpt.get();
            LOG.infof("⚠️ Warning exists for %s - Checking if notification should be sent...", trip.getCountryCode());

            // Check if there are any active warnings
            boolean hasActiveWarning = warning.getWarning() ||
                                      warning.getPartialWarning() ||
                                      warning.getSituationWarning() ||
                                      warning.getSituationPartWarning();

            LOG.debugf("Warning flags - Full: %s, Partial: %s, Situation: %s, SituationPart: %s",
                      warning.getWarning(), warning.getPartialWarning(),
                      warning.getSituationWarning(), warning.getSituationPartWarning());

            if (hasActiveWarning && trip.getNotificationsEnabled()) {
                LOG.infof("🚨 ACTIVE WARNING DETECTED - Country: %s, Severity: %s, Sending immediate notification to: %s",
                         trip.getCountryCode(), warning.getSeverity(), trip.getEmail());
                LOG.infof("📧 Initiating email alert dispatch via HTWG SMTP server...");

                // Send immediate alert
                boolean sent = alertDispatcher.sendAlert(warning, trip, trip.getEmail());

                if (sent) {
                    LOG.infof("✅ ✅ ✅ IMMEDIATE WARNING EMAIL SENT SUCCESSFULLY ✅ ✅ ✅");
                    LOG.infof("   → Recipient: %s", trip.getEmail());
                    LOG.infof("   → Destination: %s (%s)", trip.getCountryName(), trip.getCountryCode());
                    LOG.infof("   → Severity: %s", warning.getSeverity());
                    LOG.infof("   → Trip: %s", trip.getTripName());
                } else {
                    LOG.errorf("❌ ❌ ❌ FAILED TO SEND IMMEDIATE WARNING EMAIL ❌ ❌ ❌");
                    LOG.errorf("   → Recipient: %s", trip.getEmail());
                    LOG.errorf("   → Destination: %s", trip.getCountryName());
                    LOG.errorf("   → User may not be aware of travel warnings!");
                    LOG.errorf("   → Check SMTP server configuration and logs above for details");
                }
            } else if (!hasActiveWarning) {
                LOG.infof("✅ Warning exists but no active flags set for %s - User can travel safely",
                         trip.getCountryCode());
            } else {
                LOG.infof("🔕 Notifications disabled for this trip (ID: %d) - Skipping immediate alert", trip.getId());
            }

        } catch (Exception e) {
            // Don't fail trip creation if notification fails
            LOG.errorf(e, "❌ ERROR checking/sending active warning notification for trip to %s - %s",
                      trip.getCountryCode(), e.getMessage());
            LOG.errorf("Trip creation will continue, but user may not receive warning notification!");
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
        LOG.infof("PUT /warnings/trips/%d - Updating trip", tripId);

        return tripRepository.findByIdOptional(tripId)
            .map(trip -> {
                LOG.debugf("Found trip %d, applying updates", tripId);

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
                LOG.infof("✅ Trip %d updated successfully - Destination: %s", tripId, trip.getCountryName());
                return Response.ok(mapper.toDto(trip)).build();
            })
            .orElseGet(() -> {
                LOG.warnf("Trip not found for update: ID %d", tripId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.notFound(
                        "Trip not found with ID: " + tripId,
                        "/warnings/trips/" + tripId))
                    .build();
            });
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
        LOG.infof("DELETE /warnings/trips/%d - Deleting trip", tripId);

        boolean deleted = tripRepository.deleteById(tripId);

        if (deleted) {
            LOG.infof("✅ Trip %d deleted successfully - Alerts stopped", tripId);
            return Response.noContent().build();
        } else {
            LOG.warnf("Trip not found for deletion: ID %d", tripId);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse.notFound(
                    "Trip not found with ID: " + tripId,
                    "/warnings/trips/" + tripId))
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
        LOG.infof("GET /warnings/trips/user/%s/warnings - Fetching warnings affecting user's trips", email);

        List<WarningMatcherService.WarningMatch> matches = matcherService.findWarningsForUser(email);

        List<TravelWarningDto> warnings = matches.stream()
            .map(WarningMatcherService.WarningMatch::getWarning)
            .map(mapper::toDto)
            .collect(Collectors.toList());

        LOG.infof("Found %d active warnings affecting user %s's trips", warnings.size(), email);
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

        LOG.infof("PATCH /warnings/trips/%d/notifications - Setting notifications to: %s", tripId, enabled);

        return tripRepository.findByIdOptional(tripId)
            .map(trip -> {
                trip.setNotificationsEnabled(enabled);
                tripRepository.persist(trip);
                LOG.infof("✅ Notifications %s for trip %d (%s)",
                         enabled ? "enabled" : "disabled", tripId, trip.getTripName());
                return Response.ok(mapper.toDto(trip)).build();
            })
            .orElseGet(() -> {
                LOG.warnf("Trip not found for notification toggle: ID %d", tripId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.notFound(
                        "Trip not found with ID: " + tripId,
                        "/warnings/trips/" + tripId + "/notifications"))
                    .build();
            });
    }
}

