package de.htwg.service.firestore;

import com.google.cloud.firestore.*;
import de.htwg.api.like.model.LikeDto;
import de.htwg.api.like.model.LikeResponseDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class FirestoreLikeService implements LikeService {

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
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to add like", e);
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
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to remove like", e);
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
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to get likes for itinerary", e);
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
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to check if user liked itinerary", e);
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
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to get likes by user", e);
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
