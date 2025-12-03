package de.htwg.travelwarnings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwg.travelwarnings.client.AuswaertigesAmtClient;
import de.htwg.travelwarnings.client.dto.Reisewarnung;
import de.htwg.travelwarnings.client.dto.ReisewarnungZusammenfassung;
import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import de.htwg.travelwarnings.persistence.repository.TravelWarningRepository;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for fetching travel warnings from Auswärtiges Amt API.
 * Implements the "Warning Fetcher" component from the bounded context diagram.
 */
@ApplicationScoped
public class TravelWarningFetcherService {

    private static final Logger LOG = Logger.getLogger(TravelWarningFetcherService.class);

    @Inject
    @RestClient
    AuswaertigesAmtClient auswaertigesAmtClient;

    @Inject
    TravelWarningRepository travelWarningRepository;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Fetch and update all travel warnings from the external API
     */
    public int fetchAndUpdateAllWarnings() {
        LOG.info("🌍 Starting to fetch travel warnings from Auswärtiges Amt API");
        LOG.info("📡 Calling API endpoint: GET /travelwarning");

        try {
            JsonNode response = auswaertigesAmtClient.getTravelWarnings();

            LOG.info("✅ Received response from Auswärtiges Amt API");
            LOG.debugf("Response size: %d bytes", response != null ? response.toString().length() : 0);

            if (response == null || !response.has("response")) {
                LOG.error("❌ Received empty or invalid response from Auswärtiges Amt API");
                LOG.error("Response was null: " + (response == null));
                if (response != null) {
                    LOG.errorf("Response structure: %s", response.toPrettyString());
                }
                return 0;
            }

            LOG.info("✅ Response structure validated - contains 'response' node");

            JsonNode responseNode = response.get("response");
            LOG.debug("Extracting contentList from response node");

            JsonNode contentListNode = responseNode.get("contentList");

            if (contentListNode == null || !contentListNode.isArray()) {
                LOG.error("❌ No content list found in Auswärtiges Amt response");
                LOG.errorf("Response node keys: %s", responseNode.fieldNames());
                return 0;
            }

            LOG.infof("✅ Found contentList in response - Type: %s", contentListNode.getNodeType());

            List<String> contentIds = new ArrayList<>();
            contentListNode.forEach(node -> contentIds.add(node.asText()));

            LOG.infof("📋 Extracted %d content IDs from Auswärtiges Amt response", contentIds.size());
            LOG.debugf("Content IDs: %s", contentIds);

            int updatedCount = 0;
            int processedCount = 0;

            LOG.info("🔄 Starting to process individual warnings from Auswärtiges Amt response...");

            for (String contentId : contentIds) {
                processedCount++;
                try {
                    LOG.debugf("Processing warning %d/%d - Content ID: %s", processedCount, contentIds.size(), contentId);

                    // Parse the summary data from the response
                    JsonNode warningNode = responseNode.get(contentId);
                    if (warningNode != null) {
                        LOG.debugf("Found warning node for %s in response", contentId);
                        ReisewarnungZusammenfassung summaryDto = objectMapper.treeToValue(warningNode, ReisewarnungZusammenfassung.class);
                        LOG.debugf("Parsed summary DTO for %s - Country: %s", contentId, summaryDto.getCountryName());

                        // Process in separate transaction to avoid session corruption
                        boolean processed = processWarning(contentId, summaryDto);
                        if (processed) {
                            updatedCount++;
                            LOG.debugf("✅ Successfully processed warning for %s", summaryDto.getCountryName());
                        }
                    } else {
                        LOG.warnf("⚠️ Warning node not found in response for contentId: %s", contentId);
                    }
                } catch (Exception e) {
                    LOG.errorf(e, "❌ Error processing warning with contentId: %s - %s", contentId, e.getMessage());
                }
            }

            LOG.infof("✅ ✅ ✅ AUSWÄRTIGES AMT SYNC COMPLETED ✅ ✅ ✅");
            LOG.infof("   → Total warnings in response: %d", contentIds.size());
            LOG.infof("   → Successfully updated: %d", updatedCount);
            LOG.infof("   → Skipped (no changes): %d", contentIds.size() - updatedCount);
            return updatedCount;

        } catch (Exception e) {
            LOG.errorf(e, "❌ ❌ ❌ CRITICAL ERROR fetching travel warnings from Auswärtiges Amt API ❌ ❌ ❌");
            LOG.errorf("Exception type: %s", e.getClass().getSimpleName());
            LOG.errorf("Error message: %s", e.getMessage());
            LOG.error("This means travel warnings cannot be updated - users may not receive latest safety information!");
            return 0;
        }
    }

    /**
     * Process a single warning in its own transaction
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public boolean processWarning(String contentId, ReisewarnungZusammenfassung summaryDto) {
        try {
            // Validate required fields from summary
            if (!isValidWarningData(summaryDto, contentId)) {
                LOG.warnf("Skipping warning %s - missing required fields", contentId);
                return false;
            }

            Optional<TravelWarning> existingOpt = travelWarningRepository.findByContentId(contentId);

            if (existingOpt.isPresent()) {
                // Entity exists - check if needs update
                TravelWarning existing = existingOpt.get();
                if (existing.getLastModified() < summaryDto.getLastModified()) {
                    // Update existing entity in-place (it's already managed by Hibernate)
                    updateWarningFromDetail(existing, contentId, summaryDto);
                    LOG.debugf("Updated warning for %s (contentId: %s)",
                              existing.getCountryName(), contentId);
                    return true;
                }
                return false; // No update needed
            } else {
                // Create new entity
                TravelWarning warning = fetchDetailedWarning(contentId, summaryDto);
                if (warning != null && isValidEntity(warning)) {
                    travelWarningRepository.persist(warning);
                    LOG.debugf("Created warning for %s (contentId: %s)",
                              warning.getCountryName(), contentId);
                    return true;
                }
                LOG.warnf("Skipping warning %s - invalid entity after fetch", contentId);
                return false;
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error in transaction for contentId: %s", contentId);
            // Transaction will be rolled back, but other warnings can continue
            return false;
        }
    }

    /**
     * Validate that warning DTO has required fields
     */
    private boolean isValidWarningData(ReisewarnungZusammenfassung dto, String contentId) {
        if (dto == null) return false;
        if (contentId == null || contentId.trim().isEmpty()) return false;
        if (dto.getCountryCode() == null || dto.getCountryCode().trim().isEmpty()) return false;
        if (dto.getCountryName() == null || dto.getCountryName().trim().isEmpty()) return false;
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) return false;
        if (dto.getLastModified() == null) return false;
        if (dto.getEffective() == null) return false;
        return true;
    }

    /**
     * Validate that entity has all required fields before persisting
     */
    private boolean isValidEntity(TravelWarning warning) {
        if (warning == null) return false;
        if (warning.getContentId() == null || warning.getContentId().trim().isEmpty()) return false;
        if (warning.getCountryCode() == null || warning.getCountryCode().trim().isEmpty()) return false;
        if (warning.getCountryName() == null || warning.getCountryName().trim().isEmpty()) return false;
        if (warning.getTitle() == null || warning.getTitle().trim().isEmpty()) return false;
        if (warning.getLastModified() == null) return false;
        if (warning.getEffective() == null) return false;
        if (warning.getWarning() == null) return false;
        if (warning.getPartialWarning() == null) return false;
        if (warning.getSituationWarning() == null) return false;
        if (warning.getSituationPartWarning() == null) return false;
        return true;
    }

    /**
     * Update an existing warning entity with new data from API
     */
    private void updateWarningFromDetail(TravelWarning existing, String contentId, ReisewarnungZusammenfassung summaryDto) {
        try {
            LOG.debugf("📡 Fetching detailed warning from Auswärtiges Amt - Content ID: %s", contentId);
            JsonNode detailResponse = auswaertigesAmtClient.getTravelWarningDetail(contentId);

            LOG.debugf("✅ Received detail response for %s", contentId);

            if (detailResponse != null && detailResponse.has("response")) {
                LOG.debugf("Detail response validated for %s", contentId);
                JsonNode responseNode = detailResponse.get("response");
                JsonNode warningNode = responseNode.get(contentId);

                if (warningNode != null) {
                    LOG.debugf("Parsing detail DTO for %s", contentId);
                    Reisewarnung detailDto = objectMapper.treeToValue(warningNode, Reisewarnung.class);
                    LOG.debugf("✅ Successfully parsed detail data for %s - Country: %s", contentId, detailDto.getCountryName());

                    updateEntityFromDetailDto(existing, detailDto);
                    LOG.debugf("Entity updated with detailed content for %s", contentId);
                    return;
                } else {
                    LOG.warnf("⚠️ Warning node not found in detail response for %s", contentId);
                }
            } else {
                LOG.warnf("⚠️ Invalid detail response structure for %s", contentId);
            }

            // Fallback to summary data if detail fetch fails
            LOG.debugf("Using summary data for %s (detail fetch incomplete)", contentId);
            updateEntityFromSummaryDto(existing, summaryDto);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Error fetching detail from Auswärtiges Amt for contentId: %s - %s", contentId, e.getMessage());
            LOG.debugf("Falling back to summary data for %s", contentId);
            updateEntityFromSummaryDto(existing, summaryDto);
        }
    }

    /**
     * Fetch detailed warning information including content (for new entities)
     */
    private TravelWarning fetchDetailedWarning(String contentId, ReisewarnungZusammenfassung summaryDto) {
        try {
            LOG.debugf("📡 Fetching detailed warning from Auswärtiges Amt for new entity - Content ID: %s", contentId);
            JsonNode detailResponse = auswaertigesAmtClient.getTravelWarningDetail(contentId);

            LOG.debugf("✅ Received detail response for new warning: %s", contentId);

            if (detailResponse == null || !detailResponse.has("response")) {
                LOG.warnf("⚠️ No valid detail response from Auswärtiges Amt for contentId: %s - using summary data", contentId);
                return mapSummaryToWarning(contentId, summaryDto, null);
            }

            LOG.debugf("Detail response structure validated for %s", contentId);
            JsonNode responseNode = detailResponse.get("response");
            JsonNode warningNode = responseNode.get(contentId);

            if (warningNode != null) {
                LOG.debugf("Parsing detailed warning DTO for %s", contentId);
                Reisewarnung detailDto = objectMapper.treeToValue(warningNode, Reisewarnung.class);
                LOG.debugf("✅ Successfully parsed detailed warning for %s - Country: %s", contentId, detailDto.getCountryName());

                TravelWarning warning = mapDetailToWarning(contentId, detailDto);
                LOG.debugf("Created new TravelWarning entity from detail data for %s", contentId);
                return warning;
            }

            LOG.warnf("⚠️ Warning node not found in detail response for %s - using summary data", contentId);
            return mapSummaryToWarning(contentId, summaryDto, null);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Error fetching detail from Auswärtiges Amt for contentId: %s - %s", contentId, e.getMessage());
            LOG.debugf("Falling back to summary data for new entity: %s", contentId);
            return mapSummaryToWarning(contentId, summaryDto, null);
        }
    }

    /**
     * Update existing entity from detailed DTO
     */
    private void updateEntityFromDetailDto(TravelWarning warning, Reisewarnung dto) {
        warning.setLastModified(dto.getLastModified());
        warning.setEffective(dto.getEffective());
        warning.setTitle(dto.getTitle());
        warning.setCountryCode(dto.getCountryCode());
        warning.setIso3CountryCode(dto.getIso3CountryCode());
        warning.setCountryName(dto.getCountryName());
        warning.setWarning(dto.getWarning() != null ? dto.getWarning() : false);
        warning.setPartialWarning(dto.getPartialWarning() != null ? dto.getPartialWarning() : false);
        warning.setSituationWarning(dto.getSituationWarning() != null ? dto.getSituationWarning() : false);
        warning.setSituationPartWarning(dto.getSituationPartWarning() != null ? dto.getSituationPartWarning() : false);
        warning.setContent(dto.getContent());
        warning.setFetchedAt(Instant.now());
    }

    /**
     * Update existing entity from summary DTO
     */
    private void updateEntityFromSummaryDto(TravelWarning warning, ReisewarnungZusammenfassung dto) {
        warning.setLastModified(dto.getLastModified());
        warning.setEffective(dto.getEffective());
        warning.setTitle(dto.getTitle());
        warning.setCountryCode(dto.getCountryCode());
        // Note: iso3CountryCode is only available in detailed response, not in summary
        warning.setCountryName(dto.getCountryName());
        warning.setWarning(dto.getWarning() != null ? dto.getWarning() : false);
        warning.setPartialWarning(dto.getPartialWarning() != null ? dto.getPartialWarning() : false);
        warning.setSituationWarning(dto.getSituationWarning() != null ? dto.getSituationWarning() : false);
        warning.setSituationPartWarning(dto.getSituationPartWarning() != null ? dto.getSituationPartWarning() : false);
        warning.setFetchedAt(Instant.now());
    }

    /**
     * Map detailed DTO to entity
     */
    private TravelWarning mapDetailToWarning(String contentId, Reisewarnung dto) {
        TravelWarning warning = new TravelWarning();
        warning.setContentId(contentId);
        warning.setLastModified(dto.getLastModified());
        warning.setEffective(dto.getEffective());
        warning.setTitle(dto.getTitle());
        warning.setCountryCode(dto.getCountryCode());
        warning.setIso3CountryCode(dto.getIso3CountryCode());
        warning.setCountryName(dto.getCountryName());
        warning.setWarning(dto.getWarning() != null ? dto.getWarning() : false);
        warning.setPartialWarning(dto.getPartialWarning() != null ? dto.getPartialWarning() : false);
        warning.setSituationWarning(dto.getSituationWarning() != null ? dto.getSituationWarning() : false);
        warning.setSituationPartWarning(dto.getSituationPartWarning() != null ? dto.getSituationPartWarning() : false);
        warning.setContent(dto.getContent());
        warning.setFetchedAt(Instant.now());
        return warning;
    }

    /**
     * Map summary DTO to entity (when detailed content is not available)
     */
    private TravelWarning mapSummaryToWarning(String contentId, ReisewarnungZusammenfassung dto, String content) {
        TravelWarning warning = new TravelWarning();
        warning.setContentId(contentId);
        warning.setLastModified(dto.getLastModified());
        warning.setEffective(dto.getEffective());
        warning.setTitle(dto.getTitle());
        warning.setCountryCode(dto.getCountryCode());
        // Note: iso3CountryCode is only available in detailed response, not in summary
        warning.setCountryName(dto.getCountryName());
        warning.setWarning(dto.getWarning() != null ? dto.getWarning() : false);
        warning.setPartialWarning(dto.getPartialWarning() != null ? dto.getPartialWarning() : false);
        warning.setSituationWarning(dto.getSituationWarning() != null ? dto.getSituationWarning() : false);
        warning.setSituationPartWarning(dto.getSituationPartWarning() != null ? dto.getSituationPartWarning() : false);
        warning.setContent(content);
        warning.setFetchedAt(Instant.now());
        return warning;
    }

    /**
     * Get travel warning by country code (cached)
     */
    @CacheResult(cacheName = "travel-warnings")
    public Optional<TravelWarning> getWarningByCountryCode(String countryCode) {
        return travelWarningRepository.findByCountryCode(countryCode);
    }
}

