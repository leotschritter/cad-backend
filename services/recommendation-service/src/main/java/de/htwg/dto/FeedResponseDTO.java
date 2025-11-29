package de.htwg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for feed response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponseDTO {
    
    private List<FeedItemDTO> items;
    private Integer totalItems;
}

