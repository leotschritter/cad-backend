package de.htwg.travelwarnings.service;

import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for processing and extracting structured information from travel warning content.
 * Implements User Story 2: Clear, concise summary and User Story 3: Organized information by category.
 */
@ApplicationScoped
public class WarningContentService {

    private static final Logger LOG = Logger.getLogger(WarningContentService.class);

    /**
     * Generate a concise alert summary - User Story 2
     */
    public String generateAlertSummary(TravelWarning warning) {
        StringBuilder html = new StringBuilder();

        html.append("<div class='section'>")
            .append("<div class='section-title'>⚡ What's New:</div>")
            .append("<div class='info-box'>");

        // Extract key changes from content or provide generic summary
        String summary = extractSummary(warning);
        html.append(summary);

        html.append("</div></div>");

        return html.toString();
    }

    /**
     * Extract or generate summary from warning content
     */
    private String extractSummary(TravelWarning warning) {
        StringBuilder summary = new StringBuilder();

        // Check what type of warnings are active
        if (Boolean.TRUE.equals(warning.getWarning())) {
            summary.append("<p><strong>🚨 A full travel warning has been issued for ")
                   .append(warning.getCountryName())
                   .append(".</strong> Travelers are advised against all travel to this destination.</p>");
        } else if (Boolean.TRUE.equals(warning.getPartialWarning())) {
            summary.append("<p><strong>⚠️ A partial travel warning is active for certain regions in ")
                   .append(warning.getCountryName())
                   .append(".</strong> Some areas may be unsafe for travel.</p>");
        } else if (Boolean.TRUE.equals(warning.getSituationWarning())) {
            summary.append("<p><strong>⚠️ A situation-specific travel warning is in effect for ")
                   .append(warning.getCountryName())
                   .append(".</strong> Special precautions are required.</p>");
        }

        summary.append("<p><strong>Severity Level:</strong> ")
               .append(warning.getSeverity().getDisplayName())
               .append("</p>");

        return summary.toString();
    }

    /**
     * Categorize warning content - User Story 3
     * Extracts and organizes information into categories:
     * - Security
     * - Nature & Climate
     * - General Travel Information
     * - Documents & Customs
     * - Health
     */
    public WarningCategories categorizeContent(TravelWarning warning) {
        WarningCategories categories = new WarningCategories();

        if (warning.getContent() == null || warning.getContent().isEmpty()) {
            return categories;
        }

        String content = warning.getContent();

        // Extract security information
        categories.security = extractSection(content,
            "Sicherheit", "Terrorismus", "Kriminalität", "Innenpolitische Lage", "Konflikte");

        // Extract nature & climate information
        categories.natureAndClimate = extractSection(content,
            "Natur und Klima", "Naturkatastrophen", "Erdbeben", "Vulkan", "Überschwemmung", "Hurrikane");

        // Extract travel information
        categories.travelInfo = extractSection(content,
            "Reiseinfos", "Infrastruktur", "Verkehr", "Führerschein", "Kommunikation");

        // Extract document & customs information
        categories.documentsAndCustoms = extractSection(content,
            "Einreise und Zoll", "Visum", "Reisepass", "Zollvorschriften", "Einfuhr");

        // Extract health information
        categories.health = extractSection(content,
            "Gesundheit", "Medizinische Hinweise", "Impfschutz", "HIV/AIDS", "Medizinische Versorgung");

        return categories;
    }

    /**
     * Extract content section based on keywords
     */
    private String extractSection(String content, String... keywords) {
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile(
                "<h[23][^>]*>" + keyword + ".*?</h[23]>(.*?)(?=<h[23]|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }
        }
        return "";
    }

    /**
     * Clean HTML tags for plain text extraction
     */
    private String cleanHtml(String html) {
        if (html == null) return "";
        // Remove HTML tags but preserve structure
        return html.replaceAll("<[^>]+>", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    /**
     * Value object for categorized warning content - User Story 3
     */
    public static class WarningCategories {
        public String security = "";
        public String natureAndClimate = "";
        public String travelInfo = "";
        public String documentsAndCustoms = "";
        public String health = "";
        public String others = "";

        public boolean isEmpty() {
            return security.isEmpty() &&
                   natureAndClimate.isEmpty() &&
                   travelInfo.isEmpty() &&
                   documentsAndCustoms.isEmpty() &&
                   health.isEmpty() &&
                   others.isEmpty();
        }
    }
}

