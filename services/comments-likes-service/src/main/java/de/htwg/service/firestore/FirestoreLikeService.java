package de.htwg.service.firestore;

import com.google.api.gax.rpc.UnauthenticatedException;
import com.google.cloud.firestore.*;
import de.htwg.api.like.model.LikeDto;
import de.htwg.api.like.model.LikeResponseDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class FirestoreLikeService implements LikeService {

    private static final Logger LOG = Logger.getLogger(FirestoreLikeService.class);
    private static final String COLLECTION_NAME = "likes";

    @Inject
    Firestore firestore;

    @Override
    public LikeDto addLike(String userEmail, Long itineraryId) {
        try {
            // Check if user already liked this itinerary
            if (hasUserLiked(userEmail, itineraryId)) {
                throw new IllegalArgumentException("User has already liked this itinerary");
            }

            String likeId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            LikeData likeData = new LikeData(
                    userEmail,
                    itineraryId,
                    Date.from(now.toInstant(ZoneOffset.UTC))
            );

            DocumentReference docRef = firestore
                    .collection(COLLECTION_NAME)
                    .document(likeId);

            docRef.set(likeData).get();

            return LikeDto.builder()
                    .id(likeId)
                    .userEmail(userEmail)
                    .itineraryId(itineraryId)
                    .createdAt(now)
                    .build();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnauthenticatedException) {
                LOG.error("AUTHENTICATION ERROR: Cannot add like - Google Cloud credentials not configured.", e);
                throw new IllegalStateException("Google Cloud authentication failed. Please configure credentials.", e);
            }
            LOG.error("Failed to add like for user " + userEmail + " on itinerary " + itineraryId, e);
            throw new RuntimeException("Failed to add like: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while adding like", e);
            throw new RuntimeException("Operation interrupted while adding like", e);
        }
    }

    @Override
    public void removeLike(String userEmail, Long itineraryId) {
        try {
            Query query = firestore
                    .collection(COLLECTION_NAME)
                    .whereEqualTo("userEmail", userEmail)
                    .whereEqualTo("itineraryId", itineraryId);

            QuerySnapshot querySnapshot = query.get().get();
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                document.getReference().delete().get();
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnauthenticatedException) {
                LOG.error("AUTHENTICATION ERROR: Cannot remove like - Google Cloud credentials not configured.", e);
                throw new IllegalStateException("Google Cloud authentication failed. Please configure credentials.", e);
            }
            LOG.error("Failed to remove like for user " + userEmail + " from itinerary " + itineraryId, e);
            throw new RuntimeException("Failed to remove like: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while removing like", e);
            throw new RuntimeException("Operation interrupted while removing like", e);
        }
    }

    @Override
    public LikeResponseDto getLikesForItinerary(Long itineraryId) {
        try {
            Query query = firestore
                    .collection(COLLECTION_NAME)
                    .whereEqualTo("itineraryId", itineraryId);

            QuerySnapshot querySnapshot = query.get().get();
            long likeCount = querySnapshot.size();

            return LikeResponseDto.builder()
                    .itineraryId(itineraryId)
                    .likeCount(likeCount)
                    .build();
        } catch (ExecutionException e) {
            // Check if the root cause is an authentication error
            Throwable cause = e.getCause();
            if (cause instanceof UnauthenticatedException) {
                LOG.error("AUTHENTICATION ERROR: Google Cloud credentials not configured properly. " +
                        "For Kubernetes deployment, you need to configure Workload Identity or provide service account credentials.", e);
                throw new IllegalStateException("Google Cloud authentication failed. " +
                        "The application cannot authenticate with Firestore. " +
                        "Please configure service account credentials or Workload Identity for your deployment.", e);
            }
            LOG.error("Failed to get likes for itinerary " + itineraryId, e);
            throw new RuntimeException("Failed to get likes for itinerary: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while getting likes for itinerary " + itineraryId, e);
            throw new RuntimeException("Operation interrupted while getting likes for itinerary", e);
        }
    }

    @Override
    public boolean hasUserLiked(String userEmail, Long itineraryId) {
        try {
            Query query = firestore
                    .collection(COLLECTION_NAME)
                    .whereEqualTo("userEmail", userEmail)
                    .whereEqualTo("itineraryId", itineraryId)
                    .limit(1);

            QuerySnapshot querySnapshot = query.get().get();
            return !querySnapshot.isEmpty();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnauthenticatedException) {
                LOG.error("AUTHENTICATION ERROR: Cannot check if user liked itinerary - Google Cloud credentials not configured.", e);
                throw new IllegalStateException("Google Cloud authentication failed. Please configure credentials.", e);
            }
            LOG.error("Failed to check if user " + userEmail + " liked itinerary " + itineraryId, e);
            throw new RuntimeException("Failed to check if user liked itinerary: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while checking if user liked itinerary", e);
            throw new RuntimeException("Operation interrupted while checking if user liked itinerary", e);
        }
    }

    @Override
    public List<LikeDto> getLikesByUser(String userEmail) {
        try {
            Query query = firestore
                    .collection(COLLECTION_NAME)
                    .whereEqualTo("userEmail", userEmail)
                    .orderBy("createdAt", Query.Direction.DESCENDING);

            QuerySnapshot querySnapshot = query.get().get();
            List<LikeDto> likes = new ArrayList<>();

            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                LikeData likeData = document.toObject(LikeData.class);
                likes.add(LikeDto.builder()
                        .id(document.getId())
                        .userEmail(likeData.getUserEmail())
                        .itineraryId(likeData.getItineraryId())
                        .createdAt(LocalDateTime.ofInstant(
                                likeData.getCreatedAt().toInstant(),
                                ZoneOffset.UTC
                        ))
                        .build());
            }

            return likes;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnauthenticatedException) {
                LOG.error("AUTHENTICATION ERROR: Cannot get likes by user - Google Cloud credentials not configured.", e);
                throw new IllegalStateException("Google Cloud authentication failed. Please configure credentials.", e);
            }
            LOG.error("Failed to get likes for user " + userEmail, e);
            throw new RuntimeException("Failed to get likes by user: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while getting likes for user " + userEmail, e);
            throw new RuntimeException("Operation interrupted while getting likes by user", e);
        }
    }

    // Inner class for Firestore document structure
    @Setter
    @Getter
    @NoArgsConstructor
    private static class LikeData {
        private String userEmail;
        private Long itineraryId;
        private Date createdAt;

        public LikeData(String userEmail, Long itineraryId, Date createdAt) {
            this.userEmail = userEmail;
            this.itineraryId = itineraryId;
            this.createdAt = createdAt;
        }
    }
}
