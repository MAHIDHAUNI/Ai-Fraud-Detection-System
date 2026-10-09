package com.frauddetect.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RUBRIC: 2 - Collections & Generics (List<String> reasons)
 * Encapsulates the output of the fraud detection analysis for a transaction.
 * Contains the numeric risk score (0-100), calculated RiskLevel, and explainable reasons list.
 */
public class DetectionResult {
    private int score;
    private RiskLevel level;
    private final List<String> reasons;
    private String recommendedStatus; // 'APPROVED', 'FLAGGED', 'BLOCKED'

    public DetectionResult() {
        this.score = 0;
        this.level = RiskLevel.LOW;
        this.reasons = new ArrayList<>();
        this.recommendedStatus = "APPROVED";
    }

    public DetectionResult(int score, RiskLevel level, List<String> reasons) {
        this.score = score;
        this.level = (level != null) ? level : RiskLevel.LOW;
        this.reasons = (reasons != null) ? new ArrayList<>(reasons) : new ArrayList<>();
        this.recommendedStatus = deriveRecommendedStatus(this.level);
    }

    public DetectionResult(int score, RiskLevel level, List<String> reasons, String recommendedStatus) {
        this.score = score;
        this.level = (level != null) ? level : RiskLevel.LOW;
        this.reasons = (reasons != null) ? new ArrayList<>(reasons) : new ArrayList<>();
        this.recommendedStatus = (recommendedStatus != null) ? recommendedStatus : deriveRecommendedStatus(this.level);
    }

    private static String deriveRecommendedStatus(RiskLevel level) {
        if (level == RiskLevel.HIGH) {
            return "BLOCKED";
        } else if (level == RiskLevel.MEDIUM) {
            return "FLAGGED";
        } else {
            return "APPROVED";
        }
    }

    public void addReason(String reason) {
        if (reason != null && !reason.isBlank()) {
            this.reasons.add(reason);
        }
    }

    public boolean isSuspicious() {
        return level == RiskLevel.MEDIUM || level == RiskLevel.HIGH;
    }

    public String getFormattedReasons() {
        if (reasons.isEmpty()) {
            return "No suspicious indicators triggered.";
        }
        return String.join("; ", reasons);
    }

    // Getters and Setters
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public void setLevel(RiskLevel level) {
        this.level = level;
        this.recommendedStatus = deriveRecommendedStatus(level);
    }

    public List<String> getReasons() {
        return Collections.unmodifiableList(reasons);
    }

    public String getRecommendedStatus() {
        return recommendedStatus;
    }

    public void setRecommendedStatus(String recommendedStatus) {
        this.recommendedStatus = recommendedStatus;
    }

    @Override
    public String toString() {
        return String.format("DetectionResult[score=%d, level=%s, status=%s, reasons=%s]",
                score, level, recommendedStatus, reasons);
    }
}
