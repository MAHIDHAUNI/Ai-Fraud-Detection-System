package com.frauddetect.detection;

/**
 * Encapsulates the outcome of evaluating a single FraudRule against a transaction.
 */
public class RuleResult {
    private final boolean triggered;
    private final int score; // 0 to 100
    private final String reason;

    private RuleResult(boolean triggered, int score, String reason) {
        this.triggered = triggered;
        this.score = Math.max(0, Math.min(100, score));
        this.reason = reason;
    }

    public static RuleResult passed() {
        return new RuleResult(false, 0, null);
    }

    public static RuleResult flagged(int score, String reason) {
        return new RuleResult(true, score, reason);
    }

    public boolean isTriggered() {
        return triggered;
    }

    public int getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return triggered ? String.format("FLAGGED(score=%d, reason='%s')", score, reason) : "PASSED";
    }
}
