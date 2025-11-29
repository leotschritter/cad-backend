package de.htwg.travelwarnings.persistence.entity;

/**
 * Enum representing the severity levels of travel warnings.
 * Used for User Story 1: clearly indicate alert severity.
 */
public enum WarningSeverity {
    NONE("No Warning", 0),
    MINOR("Minor", 1),
    MODERATE("Moderate", 2),
    SEVERE("Severe", 3),
    CRITICAL("Critical", 4);

    private final String displayName;
    private final int level;

    WarningSeverity(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }
}

