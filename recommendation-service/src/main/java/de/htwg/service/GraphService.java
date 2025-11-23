package de.htwg.service;
import de.htwg.dto.ItineraryEventDTO;
import de.htwg.dto.LikeActionDTO;
import de.htwg.dto.LocationVisitDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@ApplicationScoped
public class GraphService {
    private static final Logger LOG = Logger.getLogger(GraphService.class);
    @Inject
    Driver neo4jDriver;
    public void recordLike(LikeActionDTO likeAction) {
        LOG.infof("Recording like: User %s likes Itinerary %d", likeAction.getUserId(), likeAction.getItineraryId());
        String cypher = """
            MERGE (u:User {id: $userId})
            MERGE (i:Itinerary {id: $itineraryId})
            MERGE (u)-[r:LIKES {timestamp: $timestamp}]->(i)
            RETURN r
            """;
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("userId", likeAction.getUserId());
                params.put("itineraryId", likeAction.getItineraryId());
                params.put("timestamp", LocalDateTime.now().toString());
                return tx.run(cypher, params).consume();
            });
            LOG.infof("Successfully recorded like from user %s", likeAction.getUserId());
        } catch (Exception e) {
            LOG.errorf(e, "Error recording like for user %s", likeAction.getUserId());
            throw new RuntimeException("Failed to record like", e);
        }
    }
    public void removeLike(LikeActionDTO likeAction) {
        LOG.infof("Removing like: User %s unlikes Itinerary %d", likeAction.getUserId(), likeAction.getItineraryId());
        String cypher = """
            MATCH (u:User {id: $userId})-[r:LIKES]->(i:Itinerary {id: $itineraryId})
            DELETE r
            """;
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("userId", likeAction.getUserId());
                params.put("itineraryId", likeAction.getItineraryId());
                return tx.run(cypher, params).consume();
            });
            LOG.infof("Successfully removed like from user %s", likeAction.getUserId());
        } catch (Exception e) {
            LOG.errorf(e, "Error removing like for user %s", likeAction.getUserId());
            throw new RuntimeException("Failed to remove like", e);
        }
    }
    public void recordItinerary(ItineraryEventDTO itineraryEvent) {
        LOG.infof("Recording itinerary: %d by user %s", itineraryEvent.getItineraryId(), itineraryEvent.getUserId());
        String cypher = """
            MERGE (u:User {id: $userId})
            MERGE (i:Itinerary {id: $itineraryId})
            ON CREATE SET i.title = $title, i.description = $description, i.createdAt = $timestamp
            ON MATCH SET i.title = $title, i.description = $description, i.likesCount = $likesCount
            MERGE (u)-[:CREATED]->(i)
            WITH i
            UNWIND $locations AS locationName
            MERGE (l:Location {name: locationName})
            MERGE (i)-[:INCLUDES]->(l)
            """;
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("userId", itineraryEvent.getUserId());
                params.put("itineraryId", itineraryEvent.getItineraryId());
                params.put("title", itineraryEvent.getTitle());
                params.put("description", itineraryEvent.getDescription());
                params.put("locations", itineraryEvent.getLocationNames());
                params.put("likesCount", itineraryEvent.getLikesCount() != null ? itineraryEvent.getLikesCount() : 0);
                params.put("timestamp", LocalDateTime.now().toString());
                return tx.run(cypher, params).consume();
            });
            LOG.infof("Successfully recorded itinerary %d", itineraryEvent.getItineraryId());
        } catch (Exception e) {
            LOG.errorf(e, "Error recording itinerary %d", itineraryEvent.getItineraryId());
            throw new RuntimeException("Failed to record itinerary", e);
        }
    }
    public void recordLocationVisits(LocationVisitDTO locationVisit) {
        LOG.infof("Recording location visits for user %s", locationVisit.getUserId());
        String cypher = """
            MERGE (u:User {id: $userId})
            WITH u
            UNWIND $locations AS locationName
            MERGE (l:Location {name: locationName})
            MERGE (u)-[v:VISITED]->(l)
            ON CREATE SET v.timestamp = $timestamp
            """;
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("userId", locationVisit.getUserId());
                params.put("locations", locationVisit.getLocationNames());
                params.put("timestamp", LocalDateTime.now().toString());
                return tx.run(cypher, params).consume();
            });
            LOG.infof("Successfully recorded location visits for user %s", locationVisit.getUserId());
        } catch (Exception e) {
            LOG.errorf(e, "Error recording location visits for user %s", locationVisit.getUserId());
            throw new RuntimeException("Failed to record location visits", e);
        }
    }
}
