package de.htwg.travelwarnings.api.dto;

import de.htwg.travelwarnings.service.WarningContentService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for detailed travel warning with categorized content
 * User Story 3: Access comprehensive safety information organized into clear categories
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelWarningDetailDto {
    private TravelWarningDto warning;
    private String content;
    private ContentCategories categories;
    private String officialLink;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentCategories {
        private String security;
        private String natureAndClimate;
        private String travelInfo;
        private String documentsAndCustoms;
        private String health;
        private String others;

        public static ContentCategories from(WarningContentService.WarningCategories categories) {
            return new ContentCategories(
                categories.security,
                categories.natureAndClimate,
                categories.travelInfo,
                categories.documentsAndCustoms,
                categories.health,
                categories.others
            );
        }
    }
}

