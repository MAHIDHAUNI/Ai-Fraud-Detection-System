package com.frauddetect.model;

/**
 * RUBRIC: 1 - OOP: Enums & Type Safety
 * Represents the computed risk severity of a financial transaction.
 */
public enum RiskLevel {
    LOW("Low Risk", "#28a745"),
    MEDIUM("Medium Risk", "#ffc107"),
    HIGH("High Risk", "#dc3545");

    private final String displayName;
    private final String colorCode;

    RiskLevel(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    /**
     * Categorizes a numeric score (0-100) into a RiskLevel based on configurable thresholds.
     *
     * @param score the calculated risk score (0 to 100)
     * @param mediumCutoff the threshold above which risk is MEDIUM
     * @param highCutoff the threshold above which risk is HIGH
     * @return the corresponding RiskLevel
     */
    public static RiskLevel fromScore(int score, int mediumCutoff, int highCutoff) {
        if (score >= highCutoff) {
            return HIGH;
        } else if (score >= mediumCutoff) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }

    @Override
    public String toString() {
        return name();
    }
}
