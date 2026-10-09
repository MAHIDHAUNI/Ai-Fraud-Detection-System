package com.frauddetect.detection;

import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.model.Transaction;

/**
 * RUBRIC: 1 - OOP: Polymorphism (FraudRule Implementation)
 * Statistical Anomaly Detection (AI Component):
 * Computes the Z-Score of the transaction amount compared to the customer's historical
 * spending mean and standard deviation: Z = (Amount - Mean) / StdDev.
 * Flags transactions where Z exceeds the threshold (e.g. Z > 3.0, representing >99.7% outlier in normal distribution).
 */
public class StatisticalAnomalyRule extends AbstractFraudRule {

    private double zScoreThreshold;
    private static final int MIN_SAMPLE_SIZE = 3;

    public StatisticalAnomalyRule(double zScoreThreshold, double weight, boolean enabled) {
        super("Statistical Anomaly Rule (Z-Score)", weight, enabled);
        this.zScoreThreshold = zScoreThreshold;
    }

    public double getzScoreThreshold() {
        return zScoreThreshold;
    }

    public void setzScoreThreshold(double zScoreThreshold) {
        this.zScoreThreshold = zScoreThreshold;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, UserHistory history) {
        if (!isEnabled() || history == null) {
            return RuleResult.passed();
        }

        TransactionDAO.AmountStats stats = history.getAmountStats();
        if (stats == null || stats.getCount() < MIN_SAMPLE_SIZE || stats.getStdDev() <= 0.001) {
            // Insufficient historical baseline for standard deviation modeling
            return RuleResult.passed();
        }

        double amount = transaction.getAmount();
        double mean = stats.getMean();
        double stdDev = stats.getStdDev();

        double zScore = (amount - mean) / stdDev;

        if (zScore > zScoreThreshold) {
            int score = (int) Math.min(100, 75 + ((zScore - zScoreThreshold) * 10));
            String reason = String.format("Statistical anomaly (Z-Score: %.2f > %.1f): Amount ₹%,.2f deviates significantly from user's historical average of ₹%,.2f (±₹%,.2f).",
                    zScore, zScoreThreshold, amount, mean, stdDev);
            return RuleResult.flagged(score, reason);
        }

        return RuleResult.passed();
    }
}
