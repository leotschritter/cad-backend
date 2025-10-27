package de.htwg.service.firestore;

import de.htwg.api.itinerary.model.LikeDto;
import de.htwg.api.itinerary.model.LikeResponseDto;

import java.util.List;

public interface LikeService {
    
    LikeDto addLike(String userEmail, Long itineraryId, String comment);
    
    void removeLike(String userEmail, Long itineraryId);
    
    LikeResponseDto getLikesForItinerary(Long itineraryId);
    
    boolean hasUserLiked(String userEmail, Long itineraryId);
    
    List<LikeDto> getLikesByUser(String userEmail);
}
