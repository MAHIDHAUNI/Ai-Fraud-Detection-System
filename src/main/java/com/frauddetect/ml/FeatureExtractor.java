package com.frauddetect.ml;

import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.detection.UserHistory;
import com.frauddetect.model.Transaction;

/**
 * Extracts a fixed-size numeric feature vector from a Transaction + UserHistory
 * for input into the logistic regression model.
 *
 * Feature vector (7 features):
 *   [0] amount           — raw transaction amount (normalized later by model)
 *   [1] zScore           — z-score of amount vs user's historical mean/stddev
 *   [2] hourOfDay        — hour of day (0-23) when the transaction occurs
 *   [3] txnsLast10Min    — count of user's transactions in the last 10 minutes (velocity)
 *   [4] newCountryFlag   — 1.0 if transaction country is new for this user, 0.0 otherwise
 *   [5] internationalFlag— 1.0 if transaction is international, 0.0 if domestic
 *   [6] roundAmountFlag  — 1.0 if amount is a suspiciously round large number, 0.0 otherwise
 */
public final class FeatureExtractor {

    /** Number of features in the extracted vector */
    public static final int FEATURE_COUNT = 7;

    /** Feature names matching model_weights table */
    public static final String[] FEATURE_NAMES = {
        "amount", "z_score", "hour_of_day", "txns_last_10min",
        "new_country_flag", "international_flag", "round_amount_flag"
    };

    private FeatureExtractor() {}

    public static double[] extractFeatures(Transaction txn, UserHistory history) {
        return extract(txn, history);
    }

    /**
     * Extracts a double[] feature vector from a transaction and user history.
     *
     * @param txn     the transaction to featurize
     * @param history the user's historical context (may be null for synthetic data)
     * @return double[7] feature vector
     */
    public static double[] extract(Transaction txn, UserHistory history) {
        double[] features = new double[FEATURE_COUNT];

        // Feature 0: Raw transaction amount
        features[0] = txn.getAmount();

        // Feature 1: Z-score (statistical anomaly measure)
        features[1] = computeZScore(txn, history);

        // Feature 2: Hour of day (0-23)
        features[2] = extractHourOfDay(txn);

        // Feature 3: Transaction velocity (count in last 10 min)
        features[3] = (history != null) ? history.getRecentVelocityTransactions().size() : 0;

        // Feature 4: New country flag (1.0 if country not in user's history)
        features[4] = computeNewCountryFlag(txn, history);

        // Feature 5: International flag (1.0 if cross-border)
        features[5] = "INTERNATIONAL".equalsIgnoreCase(txn.getTxnType()) ? 1.0 : 0.0;

        // Feature 6: Round amount flag (1.0 if suspiciously round and >= 10,000)
        features[6] = computeRoundAmountFlag(txn);

        return features;
    }

    /**
     * Extracts features from a transaction without user history (for synthetic data).
     *
     * @param txn    the transaction
     * @param zScore precomputed z-score
     * @param velocityCount precomputed velocity count
     * @param isNewCountry whether the country is new
     * @return double[7] feature vector
     */
    public static double[] extractWithPrecomputed(Transaction txn, double zScore,
                                                   int velocityCount, boolean isNewCountry) {
        double[] features = new double[FEATURE_COUNT];
        features[0] = txn.getAmount();
        features[1] = zScore;
        features[2] = extractHourOfDay(txn);
        features[3] = velocityCount;
        features[4] = isNewCountry ? 1.0 : 0.0;
        features[5] = "INTERNATIONAL".equalsIgnoreCase(txn.getTxnType()) ? 1.0 : 0.0;
        features[6] = computeRoundAmountFlag(txn);
        return features;
    }

    private static double computeZScore(Transaction txn, UserHistory history) {
        if (history == null) return 0.0;
        TransactionDAO.AmountStats stats = history.getAmountStats();
        if (stats.getCount() < 2 || stats.getStdDev() < 0.01) {
            return 0.0;
        }
        return (txn.getAmount() - stats.getMean()) / stats.getStdDev();
    }

    private static int extractHourOfDay(Transaction txn) {
        if (txn.getTxnTime() != null) {
            return txn.getTxnTime().toLocalDateTime().getHour();
        }
        return java.time.LocalTime.now().getHour();
    }

    private static double computeNewCountryFlag(Transaction txn, UserHistory history) {
        if (history == null || history.getKnownCountries().isEmpty()) return 0.0;
        String country = txn.getCountry();
        if (country == null || country.isBlank()) return 0.0;
        return history.getKnownCountries().contains(country.trim()) ? 0.0 : 1.0;
    }

    private static double computeRoundAmountFlag(Transaction txn) {
        double amount = txn.getAmount();
        if (amount < 10_000) return 0.0;
        return (amount % 1000 == 0) ? 1.0 : 0.0;
    }
}
