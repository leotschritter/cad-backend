package de.htwg.service;

import de.htwg.client.ItineraryServiceClient;
import de.htwg.dto.FeedItemDTO;
import de.htwg.dto.FeedResponseDTO;
import de.htwg.dto.ItineraryDTO;
import de.htwg.security.SecurityContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class RecommendationService {

    private static final Logger LOG = Logger.getLogger(RecommendationService.class);

    @Inject
    Driver neo4jDriver;

    @Inject
    @RestClient
    ItineraryServiceClient itineraryServiceClient;

    @Inject
    SecurityContext securityContext;

    @Inject
    de.htwg.filter.AuthorizationHeaderHolder authorizationHeaderHolder;
    public FeedResponseDTO getPersonalizedFeed(String travellerId, Integer page, Integer pageSize) {
        LOG.infof("Generating personalized feed for traveller: %s", travellerId);

        // Get itinerary IDs with recommendation metadata from graph
        List<Map<String, Object>> recommendations = getCollaborativeFilteringRecommendations(travellerId, page, pageSize);

        if (recommendations.size() < pageSize) {
            LOG.infof("Supplementing with location-based recommendations for traveller %s", travellerId);
            int remaining = pageSize - recommendations.size();
            List<Map<String, Object>> locationBased = getLocationBasedRecommendations(travellerId, page, remaining);
            recommendations.addAll(locationBased);
        }

        if (recommendations.isEmpty()) {
            LOG.infof("No personalization data for traveller %s, using popular feed", travellerId);
            recommendations = getPopularItineraries(page, pageSize);
        }

        // Extract itinerary IDs
        List<Long> itineraryIds = recommendations.stream()
                .map(rec -> (Long) rec.get("itineraryId"))
                .collect(Collectors.toList());

        // Fetch full itinerary details from itinerary service
        List<FeedItemDTO> feedItems = enrichWithItineraryDetails(itineraryIds, recommendations);

        long total = feedItems.size();
        return FeedResponseDTO.builder()
                .items(feedItems)
                .page(page)
                .pageSize(pageSize)
                .totalItems(total)
                .hasMore(feedItems.size() >= pageSize)
                .build();
    }

    /**
     * Fetch itinerary details from itinerary service.
     * Forwards the authentication header from the current request.
     * The returned list has the exact same structure as itineraries from the itinerary service.
     */
    private List<FeedItemDTO> enrichWithItineraryDetails(List<Long> itineraryIds,
                                                          List<Map<String, Object>> recommendations) {
        if (itineraryIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Get authorization header from the holder (stored by AuthenticationFilter)
            String authHeader = authorizationHeaderHolder.getAuthorizationHeader();

            if (authHeader == null || authHeader.isEmpty()) {
                LOG.warn("No authorization header available to forward to itinerary service");
                return new ArrayList<>();
            }

            // Fetch itineraries from itinerary service
            List<ItineraryDTO> itineraries = itineraryServiceClient.getItinerariesByIds(authHeader, itineraryIds);

            // Create a map for maintaining the order from recommendations
            Map<Long, ItineraryDTO> itineraryMap = itineraries.stream()
                    .collect(Collectors.toMap(ItineraryDTO::getId, i -> i));

            // Convert to FeedItemDTO maintaining the recommendation order
            return recommendations.stream()
                    .map(rec -> {
                        Long id = (Long) rec.get("itineraryId");
                        ItineraryDTO itinerary = itineraryMap.get(id);

                        if (itinerary == null) {
                            LOG.warnf("Itinerary %d not found in itinerary service", id);
                            return null;
                        }

                        // Convert to FeedItemDTO - exact same structure as ItineraryDTO
                        return FeedItemDTO.builder()
                                .id(itinerary.getId())
                                .title(itinerary.getTitle())
                                .destination(itinerary.getDestination())
                                .startDate(itinerary.getStartDate())
                                .shortDescription(itinerary.getShortDescription())
                                .detailedDescription(itinerary.getDetailedDescription())
                                .build();
                    })
                    .filter(item -> item != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            LOG.errorf(e, "Error fetching itinerary details from itinerary service");
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getCollaborativeFilteringRecommendations(String userId, Integer page, Integer pageSize) {
        LOG.debugf("Getting collaborative filtering recommendations for user: %s", userId);
        String cypher = """
            MATCH (u:User {id: $userId})-[:LIKES]->(i:Itinerary)<-[:LIKES]-(other:User)
            MATCH (other)-[:LIKES]->(recommendation:Itinerary)
            WHERE NOT (u)-[:LIKES]->(recommendation)
            WITH recommendation, COUNT(DISTINCT other) as commonUsers, 
                 SIZE((recommendation)<-[:LIKES]-()) as totalLikes
            RETURN recommendation.id as itineraryId,
                   totalLikes,
                   commonUsers,
                   (commonUsers * 2.0 + totalLikes * 0.5) as relevanceScore
            ORDER BY relevanceScore DESC
            SKIP $skip
            LIMIT $limit
            """;
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.readTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("userId", userId);
                params.put("skip", page * pageSize);
                params.put("limit", pageSize);
                return tx.run(cypher, params);
            });
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> item = new HashMap<>();
                item.put("itineraryId", record.get("itineraryId").asLong());
                item.put("likesCount", record.get("totalLikes").asInt(0));
                item.put("matchReason", String.format("Liked by %d users with similar taste",
                        record.get("commonUsers").asInt(0)));
                item.put("relevanceScore", record.get("relevanceScore").asDouble(0.0));
                results.add(item);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error getting collaborative filtering recommendations for user %s", userId);
        }
        return results;
    }
    private List<Map<String, Object>> getLocationBasedRecommendations(String userId, Integer page, Integer pageSize) {
        LOG.debugf("Getting location-based recommendations for user: %s", userId);
        String cypher = """
            MATCH (u:User {id: $userId})-[:VISITED]->(loc:Location)<-[:INCLUDES]-(i:Itinerary)
            WHERE NOT (u)-[:LIKES]->(i) AND NOT (u)-[:CREATED]->(i)
            WITH i, COUNT(DISTINCT loc) as commonLocations,
                 SIZE((i)<-[:LIKES]-()) as totalLikes
            OPTIONAL MATCH (i)-[:INCLUDES]->(location:Location)
            WITH i, commonLocations, totalLikes, 
                 COLLECT(DISTINCT location.name) as locations
            RETURN i.id as itineraryId,
                   totalLikes,
                   locations,
                   commonLocations,
                   (commonLocations * 3.0 + totalLikes * 0.3) as relevanceScore
            ORDER BY relevanceScore DESC
            SKIP $skip
            LIMIT $limit
            """;
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.readTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("userId", userId);
                params.put("skip", page * pageSize);
                params.put("limit", pageSize);
                return tx.run(cypher, params);
            });
            while (result.hasNext()) {
                Record record = result.next();
                List<String> locations = record.get("locations").asList(v -> v.asString());
                Map<String, Object> item = new HashMap<>();
                item.put("itineraryId", record.get("itineraryId").asLong());
                item.put("likesCount", record.get("totalLikes").asInt(0));
                item.put("matchReason", String.format("Includes %d location(s) you visited: %s",
                        record.get("commonLocations").asInt(0),
                        locations.stream().limit(2).collect(Collectors.joining(", "))));
                item.put("relevanceScore", record.get("relevanceScore").asDouble(0.0));
                results.add(item);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error getting location-based recommendations for user %s", userId);
        }
        return results;
    }
    public FeedResponseDTO getPopularFeed(Integer page, Integer pageSize) {
        LOG.info("Generating popular feed");

        List<Map<String, Object>> popularRecommendations = getPopularItineraries(page, pageSize);

        // Extract itinerary IDs
        List<Long> itineraryIds = popularRecommendations.stream()
                .map(rec -> (Long) rec.get("itineraryId"))
                .collect(Collectors.toList());

        // Fetch full itinerary details
        List<FeedItemDTO> feedItems = enrichWithItineraryDetails(itineraryIds, popularRecommendations);

        return FeedResponseDTO.builder()
                .items(feedItems)
                .page(page)
                .pageSize(pageSize)
                .totalItems((long) feedItems.size())
                .hasMore(feedItems.size() >= pageSize)
                .build();
    }

    private List<Map<String, Object>> getPopularItineraries(Integer page, Integer pageSize) {
        LOG.debugf("Getting popular itineraries, page: %d, pageSize: %d", page, pageSize);
        String cypher = """
            MATCH (i:Itinerary)
            WITH i, SIZE((i)<-[:LIKES]-()) as likesCount
            WHERE likesCount > 0
            RETURN i.id as itineraryId,
                   likesCount
            ORDER BY likesCount DESC
            SKIP $skip
            LIMIT $limit
            """;
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.readTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("skip", page * pageSize);
                params.put("limit", pageSize);
                return tx.run(cypher, params);
            });
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> item = new HashMap<>();
                item.put("itineraryId", record.get("itineraryId").asLong());
                item.put("likesCount", record.get("likesCount").asInt(0));
                item.put("matchReason", "Popular itinerary");
                item.put("relevanceScore", (double) record.get("likesCount").asInt(0));
                results.add(item);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error getting popular itineraries");
        }
        return results;
    }
}
