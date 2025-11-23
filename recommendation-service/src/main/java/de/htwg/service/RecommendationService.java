package de.htwg.service;
import de.htwg.dto.FeedItemDTO;
import de.htwg.dto.FeedResponseDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    public FeedResponseDTO getPersonalizedFeed(String travellerId, Integer page, Integer pageSize) {
        LOG.infof("Generating personalized feed for traveller: %s", travellerId);
        List<FeedItemDTO> recommendations = getCollaborativeFilteringRecommendations(travellerId, page, pageSize);
        if (recommendations.size() < pageSize) {
            LOG.infof("Supplementing with location-based recommendations for traveller %s", travellerId);
            int remaining = pageSize - recommendations.size();
            List<FeedItemDTO> locationBased = getLocationBasedRecommendations(travellerId, page, remaining);
            recommendations.addAll(locationBased);
        }
        if (recommendations.isEmpty()) {
            LOG.infof("No personalization data for traveller %s, using popular feed", travellerId);
            recommendations = getPopularItineraries(page, pageSize);
        }
        long total = recommendations.size();
        return FeedResponseDTO.builder()
                .items(recommendations)
                .page(page)
                .pageSize(pageSize)
                .totalItems(total)
                .hasMore(recommendations.size() >= pageSize)
                .build();
    }
    public FeedResponseDTO getPopularFeed(Integer page, Integer pageSize) {
        LOG.info("Generating popular feed");
        List<FeedItemDTO> popularItineraries = getPopularItineraries(page, pageSize);
        return FeedResponseDTO.builder()
                .items(popularItineraries)
                .page(page)
                .pageSize(pageSize)
                .totalItems((long) popularItineraries.size())
                .hasMore(popularItineraries.size() >= pageSize)
                .build();
    }
    private List<FeedItemDTO> getCollaborativeFilteringRecommendations(String userId, Integer page, Integer pageSize) {
        LOG.debugf("Getting collaborative filtering recommendations for user: %s", userId);
        String cypher = """
            MATCH (u:User {id: $userId})-[:LIKES]->(i:Itinerary)<-[:LIKES]-(other:User)
            MATCH (other)-[:LIKES]->(recommendation:Itinerary)
            WHERE NOT (u)-[:LIKES]->(recommendation)
            WITH recommendation, COUNT(DISTINCT other) as commonUsers, 
                 SIZE((recommendation)<-[:LIKES]-()) as totalLikes
            MATCH (creator:User)-[:CREATED]->(recommendation)
            OPTIONAL MATCH (recommendation)-[:INCLUDES]->(loc:Location)
            WITH recommendation, commonUsers, totalLikes, creator, COLLECT(DISTINCT loc.name) as locations
            RETURN recommendation.id as itineraryId,
                   recommendation.title as title,
                   recommendation.description as description,
                   creator.id as creatorId,
                   totalLikes,
                   locations,
                   commonUsers,
                   (commonUsers * 2.0 + totalLikes * 0.5) as relevanceScore
            ORDER BY relevanceScore DESC
            SKIP $skip
            LIMIT $limit
            """;
        List<FeedItemDTO> results = new ArrayList<>();
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
                FeedItemDTO item = FeedItemDTO.builder()
                        .itineraryId(record.get("itineraryId").asLong())
                        .title(record.get("title").asString("Untitled"))
                        .description(record.get("description").asString(""))
                        .travellerName(record.get("creatorId").asString("Unknown"))
                        .likesCount(record.get("totalLikes").asInt(0))
                        .destinations(record.get("locations").asList(v -> v.asString()))
                        .matchReason(String.format("Liked by %d users with similar taste", 
                                record.get("commonUsers").asInt(0)))
                        .relevanceScore(record.get("relevanceScore").asDouble(0.0))
                        .build();
                results.add(item);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error getting collaborative filtering recommendations for user %s", userId);
        }
        return results;
    }
    private List<FeedItemDTO> getLocationBasedRecommendations(String userId, Integer page, Integer pageSize) {
        LOG.debugf("Getting location-based recommendations for user: %s", userId);
        String cypher = """
            MATCH (u:User {id: $userId})-[:VISITED]->(loc:Location)<-[:INCLUDES]-(i:Itinerary)
            WHERE NOT (u)-[:LIKES]->(i) AND NOT (u)-[:CREATED]->(i)
            WITH i, COUNT(DISTINCT loc) as commonLocations,
                 SIZE((i)<-[:LIKES]-()) as totalLikes
            MATCH (creator:User)-[:CREATED]->(i)
            OPTIONAL MATCH (i)-[:INCLUDES]->(location:Location)
            WITH i, commonLocations, totalLikes, creator, 
                 COLLECT(DISTINCT location.name) as locations
            RETURN i.id as itineraryId,
                   i.title as title,
                   i.description as description,
                   creator.id as creatorId,
                   totalLikes,
                   locations,
                   commonLocations,
                   (commonLocations * 3.0 + totalLikes * 0.3) as relevanceScore
            ORDER BY relevanceScore DESC
            SKIP $skip
            LIMIT $limit
            """;
        List<FeedItemDTO> results = new ArrayList<>();
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
                FeedItemDTO item = FeedItemDTO.builder()
                        .itineraryId(record.get("itineraryId").asLong())
                        .title(record.get("title").asString("Untitled"))
                        .description(record.get("description").asString(""))
                        .travellerName(record.get("creatorId").asString("Unknown"))
                        .likesCount(record.get("totalLikes").asInt(0))
                        .destinations(locations)
                        .matchReason(String.format("Includes %d location(s) you visited: %s", 
                                record.get("commonLocations").asInt(0),
                                locations.stream().limit(2).collect(Collectors.joining(", "))))
                        .relevanceScore(record.get("relevanceScore").asDouble(0.0))
                        .build();
                results.add(item);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error getting location-based recommendations for user %s", userId);
        }
        return results;
    }
    private List<FeedItemDTO> getPopularItineraries(Integer page, Integer pageSize) {
        LOG.debugf("Getting popular itineraries, page: %d, pageSize: %d", page, pageSize);
        String cypher = """
            MATCH (i:Itinerary)
            WITH i, SIZE((i)<-[:LIKES]-()) as likesCount
            MATCH (creator:User)-[:CREATED]->(i)
            OPTIONAL MATCH (i)-[:INCLUDES]->(loc:Location)
            WITH i, likesCount, creator, COLLECT(DISTINCT loc.name) as locations
            WHERE likesCount > 0
            RETURN i.id as itineraryId,
                   i.title as title,
                   i.description as description,
                   creator.id as creatorId,
                   likesCount,
                   locations
            ORDER BY likesCount DESC
            SKIP $skip
            LIMIT $limit
            """;
        List<FeedItemDTO> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.readTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("skip", page * pageSize);
                params.put("limit", pageSize);
                return tx.run(cypher, params);
            });
            while (result.hasNext()) {
                Record record = result.next();
                FeedItemDTO item = FeedItemDTO.builder()
                        .itineraryId(record.get("itineraryId").asLong())
                        .title(record.get("title").asString("Untitled"))
                        .description(record.get("description").asString(""))
                        .travellerName(record.get("creatorId").asString("Unknown"))
                        .likesCount(record.get("likesCount").asInt(0))
                        .destinations(record.get("locations").asList(v -> v.asString()))
                        .matchReason("Popular itinerary")
                        .relevanceScore((double) record.get("likesCount").asInt(0))
                        .build();
                results.add(item);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error getting popular itineraries");
        }
        return results;
    }
}
