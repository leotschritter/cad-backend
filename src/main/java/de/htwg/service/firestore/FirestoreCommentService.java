package de.htwg.service.firestore;

import com.google.cloud.firestore.*;
import de.htwg.api.itinerary.model.CommentDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class FirestoreCommentService implements CommentService {

    private static final String COLLECTION_NAME = "comments";
    private final Firestore firestore;
    private final String projectId;

    @ConfigProperty(name = "google.cloud.projectId")
    String configuredProjectId;

    @ConfigProperty(name = "google.firestore.use-emulator", defaultValue = "false")
    boolean useEmulator;

    @ConfigProperty(name = "google.firestore.emulator-host", defaultValue = "localhost:8081")
    String emulatorHost;

    @Inject
    public FirestoreCommentService() {
        this.projectId = configuredProjectId;

        if (useEmulator) {
            this.firestore = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setHost(emulatorHost)
                    .setCredentials(new FirestoreOptions.EmulatorCredentials())
                    .build()
                    .getService();
        } else {
            this.firestore = FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .build()
                    .getService();
        }
    }

    @Override
    public CommentDto addComment(String userEmail, Long itineraryId, String comment) {
        try {
            if (comment == null || comment.trim().isEmpty()) {
                throw new IllegalArgumentException("Comment text cannot be empty");
            }

            String commentId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            
            CommentData commentData = new CommentData(
                userEmail,
                itineraryId,
                comment,
                Date.from(now.toInstant(ZoneOffset.UTC))
            );

            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(commentId);
            docRef.set(commentData).get();

            return CommentDto.builder()
                    .id(commentId)
                    .userEmail(userEmail)
                    .itineraryId(itineraryId)
                    .comment(comment)
                    .createdAt(now)
                    .build();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to add comment", e);
        }
    }

    @Override
    public void deleteComment(String commentId, String userEmail) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(commentId);
            DocumentSnapshot doc = docRef.get().get();

            if (!doc.exists()) {
                throw new IllegalArgumentException("Comment not found");
            }

            CommentData commentData = doc.toObject(CommentData.class);
            if (commentData == null || !commentData.getUserEmail().equals(userEmail)) {
                throw new IllegalArgumentException("You can only delete your own comments");
            }

            docRef.delete().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to delete comment", e);
        }
    }

    @Override
    public List<CommentDto> getCommentsForItinerary(Long itineraryId) {
        try {
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("itineraryId", itineraryId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .get();

            List<CommentDto> comments = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                CommentData commentData = document.toObject(CommentData.class);
                if (commentData != null) {
                    comments.add(CommentDto.builder()
                            .id(document.getId())
                            .userEmail(commentData.getUserEmail())
                            .itineraryId(commentData.getItineraryId())
                            .comment(commentData.getComment())
                            .createdAt(LocalDateTime.ofInstant(
                                commentData.getCreatedAt().toInstant(),
                                ZoneOffset.UTC
                            ))
                            .build());
                }
            }
            return comments;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to retrieve comments for itinerary", e);
        }
    }

    @Override
    public List<CommentDto> getCommentsByUser(String userEmail) {
        try {
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userEmail", userEmail)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .get();

            List<CommentDto> comments = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                CommentData commentData = document.toObject(CommentData.class);
                if (commentData != null) {
                    comments.add(CommentDto.builder()
                            .id(document.getId())
                            .userEmail(commentData.getUserEmail())
                            .itineraryId(commentData.getItineraryId())
                            .comment(commentData.getComment())
                            .createdAt(LocalDateTime.ofInstant(
                                commentData.getCreatedAt().toInstant(),
                                ZoneOffset.UTC
                            ))
                            .build());
                }
            }
            return comments;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to retrieve comments by user", e);
        }
    }

    // Inner class for Firestore document structure
    private static class CommentData {
        private String userEmail;
        private Long itineraryId;
        private String comment;
        private Date createdAt;

        public CommentData() {
        }

        public CommentData(String userEmail, Long itineraryId, String comment, Date createdAt) {
            this.userEmail = userEmail;
            this.itineraryId = itineraryId;
            this.comment = comment;
            this.createdAt = createdAt;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public Long getItineraryId() {
            return itineraryId;
        }

        public void setItineraryId(Long itineraryId) {
            this.itineraryId = itineraryId;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }
}

