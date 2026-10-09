package com.frauddetect.detection;

import com.frauddetect.model.Transaction;

/**
 * RUBRIC: 1 - OOP: Polymorphism (FraudRule Implementation)
 * Flags transactions whose transfer amount exceeds the administrator-configured threshold.
 */
public class HighAmountRule extends AbstractFraudRule {

    private double threshold;

    public HighAmountRule(double threshold, double weight, boolean enabled) {
        super("High Amount Rule", weight, enabled);
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, UserHistory history) {
        if (!isEnabled()) {
            return RuleResult.passed();
        }

        double amount = transaction.getAmount();
        if (amount > threshold) {
            // Proportional risk score between 75 and 100 based on how far amount exceeds threshold
            double excessRatio = (amount - threshold) / threshold;
            int score = (int) Math.min(100, 75 + (excessRatio * 25));
            String reason = String.format("Transaction amount ₹%,.2f exceeds high-value threshold of ₹%,.2f.",
                    amount, threshold);
            return RuleResult.flagged(score, reason);
        }

        return RuleResult.passed();
    }
}
