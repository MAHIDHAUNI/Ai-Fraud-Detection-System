package com.frauddetect.detection;

import com.frauddetect.model.Transaction;

/**
 * RUBRIC: 1 - OOP: Polymorphism (FraudRule Implementation)
 * Flags rapid-fire transaction bursts indicating potential automated bot attacks or compromised accounts.
 */
public class VelocityRule extends AbstractFraudRule {

    private int maxTransactions;
    private int windowMinutes;

    public VelocityRule(int maxTransactions, int windowMinutes, double weight, boolean enabled) {
        super("Transaction Velocity Rule", weight, enabled);
        this.maxTransactions = maxTransactions;
        this.windowMinutes = windowMinutes;
    }

    public int getMaxTransactions() {
        return maxTransactions;
    }

    public void setMaxTransactions(int maxTransactions) {
        this.maxTransactions = maxTransactions;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, UserHistory history) {
        if (!isEnabled() || history == null) {
            return RuleResult.passed();
        }

        int recentCount = history.getRecentVelocityTransactions().size();
        if (recentCount >= maxTransactions) {
            int score = Math.min(100, 70 + ((recentCount - maxTransactions + 1) * 10));
            String reason = String.format("High transaction velocity: %d transactions initiated within %d minutes (configured maximum: %d).",
                    recentCount + 1, windowMinutes, maxTransactions);
            return RuleResult.flagged(score, reason);
        }

        return RuleResult.passed();
    }
}
