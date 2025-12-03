package de.htwg.travelwarnings.api.resource;

import de.htwg.travelwarnings.api.dto.ErrorResponse;
import de.htwg.travelwarnings.api.dto.TravelWarningDto;
import de.htwg.travelwarnings.api.mapper.TravelWarningMapper;
import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import de.htwg.travelwarnings.persistence.repository.TravelWarningRepository;
import de.htwg.travelwarnings.scheduler.TravelWarningScheduler;
import de.htwg.travelwarnings.service.TravelWarningFetcherService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for travel warnings
 * Implements User Story 1 & 3: Access to travel warnings with severity information
 */
@Path("/warnings/travel-warnings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Travel Warnings", description = "Access official travel warnings from Auswärtiges Amt")
public class TravelWarningResource {

    private static final Logger LOG = Logger.getLogger(TravelWarningResource.class);

    @Inject
    TravelWarningRepository warningRepository;

    @Inject
    TravelWarningMapper mapper;

    @Inject
    TravelWarningFetcherService fetcherService;

    @Inject
    TravelWarningScheduler scheduler;

    /**
     * Get all travel warnings
     */
    @GET
    @Operation(summary = "Get all travel warnings",
               description = "Returns all travel warnings stored in the system")
    public Response getAllWarnings(
            @QueryParam("activeOnly") @DefaultValue("false") boolean activeOnly) {

        LOG.infof("GET /warnings/travel-warnings - activeOnly: %s", activeOnly);

        List<TravelWarning> warnings = activeOnly
            ? warningRepository.findAllWithActiveWarnings()
            : warningRepository.listAll();

        List<TravelWarningDto> dtos = warnings.stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());

        LOG.infof("Returning %d travel warnings", dtos.size());
        return Response.ok(dtos).build();
    }

    /**
     * Get travel warning by country code (User Story 1)
     */
    @GET
    @Path("/country/{countryCode}")
    @Operation(summary = "Get travel warning by country code",
               description = "Returns travel warning for a specific country")
    public Response getWarningByCountryCode(@PathParam("countryCode") String countryCode) {
        LOG.infof("GET /warnings/travel-warnings/country/%s", countryCode);

        var result = warningRepository.findByCountryCode(countryCode.toUpperCase())
            .map(mapper::toDto)
            .map(dto -> {
                LOG.infof("Found travel warning for country: %s with severity: %s", countryCode, dto.getSeverity());
                return Response.ok(dto).build();
            })
            .orElseGet(() -> {
                LOG.warnf("No warning found for country code: %s", countryCode);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("No warning found for country code: " + countryCode)
                    .build();
            });

        return result;
    }

    /**
     * Get detailed travel warning with categorized content (User Story 3)
     */
    @GET
    @Path("/country/{countryCode}/detail")
    @Operation(summary = "Get detailed travel warning",
               description = "Returns detailed travel warning with categorized content")
    public Response getWarningDetailByCountryCode(@PathParam("countryCode") String countryCode) {
        LOG.infof("GET /warnings/travel-warnings/country/%s/detail", countryCode);

        var result = warningRepository.findByCountryCode(countryCode.toUpperCase())
            .map(mapper::toDetailDto)
            .map(dto -> {
                LOG.infof("Returning detailed warning for country: %s", countryCode);
                return Response.ok(dto).build();
            })
            .orElseGet(() -> {
                LOG.warnf("No detailed warning found for country code: %s", countryCode);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("No warning found for country code: " + countryCode)
                    .build();
            });

        return result;
    }

    /**
     * Get travel warning by content ID
     */
    @GET
    @Path("/{contentId}")
    @Operation(summary = "Get travel warning by content ID",
               description = "Returns travel warning by Auswärtiges Amt content ID")
    public Response getWarningByContentId(@PathParam("contentId") String contentId) {
        LOG.infof("GET /warnings/travel-warnings/%s", contentId);

        var result = warningRepository.findByContentId(contentId)
            .map(mapper::toDto)
            .map(dto -> {
                LOG.infof("Found warning with content ID: %s", contentId);
                return Response.ok(dto).build();
            })
            .orElseGet(() -> {
                LOG.warnf("No warning found with content ID: %s", contentId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.notFound(
                        "No warning found with content ID: " + contentId,
                        "/warnings/travel-warnings/" + contentId))
                    .build();
            });

        return result;
    }

    /**
     * Get warnings for multiple countries (batch)
     */
    @POST
    @Path("/batch")
    @Operation(summary = "Get warnings for multiple countries",
               description = "Returns travel warnings for a list of country codes")
    public Response getWarningsBatch(List<String> countryCodes) {
        LOG.infof("POST /warnings/travel-warnings/batch - Requesting warnings for %d countries: %s",
                  countryCodes.size(), countryCodes);

        List<String> upperCaseCodes = countryCodes.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());

        List<TravelWarning> warnings = warningRepository.findByCountryCodes(upperCaseCodes);

        List<TravelWarningDto> dtos = warnings.stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());

        LOG.infof("Returning %d warnings for batch request", dtos.size());
        return Response.ok(dtos).build();
    }

    /**
     * Manually trigger warning update (for testing/admin)
     */
    @POST
    @Path("/refresh")
    @Operation(summary = "Manually refresh travel warnings",
               description = "Triggers manual fetch of latest warnings from Auswärtiges Amt API")
    public Response refreshWarnings() {
        LOG.info("POST /warnings/travel-warnings/refresh - Manual refresh triggered via API");
        scheduler.triggerManualPoll();
        LOG.info("Travel warning refresh job triggered successfully");

        // Return proper JSON response
        return Response.accepted()
            .entity(new java.util.HashMap<String, Object>() {{
                put("message", "Travel warning refresh triggered");
                put("status", "accepted");
                put("timestamp", java.time.Instant.now().toString());
            }})
            .build();
    }
}

