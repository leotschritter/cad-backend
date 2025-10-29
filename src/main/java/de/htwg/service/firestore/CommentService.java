package de.htwg.service.firestore;

import de.htwg.api.itinerary.model.CommentDto;

import java.util.List;

public interface CommentService {
    
    CommentDto addComment(String userEmail, Long itineraryId, String comment);
    
    void deleteComment(String commentId, String userEmail);
    
    List<CommentDto> getCommentsForItinerary(Long itineraryId);
    
    List<CommentDto> getCommentsByUser(String userEmail);
}

