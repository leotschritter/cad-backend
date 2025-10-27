package de.htwg.service.firestore;

import com.google.cloud.firestore.*;
import de.htwg.api.itinerary.model.LikeDto;
import de.htwg.api.itinerary.model.LikeResponseDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class FirestoreLikeService implements LikeService {

    @Inject
    @ConfigProperty(name = "google.firestore.use-emulator", defaultValue = "false")
    boolean useEmulator;

    @Inject
    @ConfigProperty(name = "google.firestore.emulator-host", defaultValue = "localhost:8080")
    String emulatorHost;

    @Inject
    @ConfigProperty(name = "google.cloud.projectId")
    String projectId;

    private Firestore firestore;
    private static final String COLLECTION_NAME = "likes";

    private Firestore getFirestore() {
        if (firestore == null) {
            FirestoreOptions.Builder optionsBuilder = FirestoreOptions.newBuilder()
                    .setProjectId(projectId);

            if (useEmulator) {
                optionsBuilder.setHost(emulatorHost);
            }

            firestore = optionsBuilder.build().getService();
        }
        return firestore;
    }

    @Override
    public LikeDto addLike(String userEmail, Long itineraryId, String comment) {
        try {
            // Check if user already liked this itinerary
            if (hasUserLiked(userEmail, itineraryId)) {
                throw new IllegalArgumentException("User has already liked this itinerary");
            }

            String likeId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            LikeDto likeDto = LikeDto.builder()
                    .id(likeId)
                    .userEmail(userEmail)
                    .itineraryId(itineraryId)
                    .comment(comment)
                    .createdAt(now)
                    .build();

            DocumentReference docRef = getFirestore()
                    .collection(COLLECTION_NAME)
                    .document(likeId);

            docRef.set(likeDto).get();

            return likeDto;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to add like", e);
        }
    }

    @Override
    public void removeLike(String userEmail, Long itineraryId) {
        try {
            Query query = getFirestore()
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
            Query query = getFirestore()
                    .collection(COLLECTION_NAME)
                    .whereEqualTo("itineraryId", itineraryId)
                    .orderBy("createdAt", Query.Direction.ASCENDING);

            QuerySnapshot querySnapshot = query.get().get();
            List<LikeDto> likes = new ArrayList<>();

            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                LikeDto like = document.toObject(LikeDto.class);
                likes.add(like);
            }

            return LikeResponseDto.builder()
                    .itineraryId(itineraryId)
                    .likeCount(likes.size())
                    .likes(likes)
                    .build();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to get likes for itinerary", e);
        }
    }

    @Override
    public boolean hasUserLiked(String userEmail, Long itineraryId) {
        try {
            Query query = getFirestore()
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
            Query query = getFirestore()
                    .collection(COLLECTION_NAME)
                    .whereEqualTo("userEmail", userEmail)
                    .orderBy("createdAt", Query.Direction.DESCENDING);

            QuerySnapshot querySnapshot = query.get().get();
            List<LikeDto> likes = new ArrayList<>();

            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                LikeDto like = document.toObject(LikeDto.class);
                likes.add(like);
            }

            return likes;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to get likes by user", e);
        }
    }
}
