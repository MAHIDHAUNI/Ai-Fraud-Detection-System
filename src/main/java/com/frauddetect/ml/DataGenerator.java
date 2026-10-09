package com.frauddetect.ml;

import com.frauddetect.model.DomesticTransaction;
import com.frauddetect.model.InternationalTransaction;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates realistic synthetic labelled transaction data for training the
 * logistic regression model. Produces ~3000 transactions with ~8% fraud rate.
 *
 * Fraud patterns modelled:
 *   - High amounts (>50,000) at unusual hours (00:00-05:00)
 *   - International transactions to uncommon countries
 *   - Suspiciously round large amounts
 *   - High velocity bursts
 *   - Combinations of the above
 *
 * OOP Concept: Encapsulation — data generation logic is hidden; only the public
 * generate() method exposes the labelled dataset.
 */
public final class DataGenerator {

    /** Container holding a feature vector and its label */
    public static class LabelledSample {
        private final double[] features;
        private final double label; // 1.0 = fraud, 0.0 = legitimate

        public LabelledSample(double[] features, double label) {
            this.features = features;
            this.label = label;
        }

        public double[] getFeatures() { return features; }
        public double getLabel() { return label; }
    }

    private static final String[] DOMESTIC_CITIES = {
        "Mumbai", "Delhi", "Bengaluru", "Chennai", "Hyderabad",
        "Pune", "Kolkata", "Ahmedabad", "Jaipur", "Lucknow"
    };

    private static final String[] FOREIGN_COUNTRIES = {
        "UAE", "USA", "UK", "Singapore", "Nigeria",
        "Russia", "China", "Brazil", "South Africa", "Thailand"
    };

    private DataGenerator() {}

    public static List<LabelledSample> generateDataset(int count) {
        return generate(count, 42L);
    }

    public static List<LabelledSample> generateDataset() {
        return generate(3000, 42L);
    }

    public static List<LabelledSample> generate(long seed) {
        return generate(3000, seed);
    }

    /**
     * Generates labelled samples with ~8% fraud rate.
     *
     * @param totalSamples total count of samples to generate
     * @param seed         random seed for reproducibility
     * @return list of LabelledSample objects
     */
    public static List<LabelledSample> generate(int totalSamples, long seed) {
        Random rng = new Random(seed);
        List<LabelledSample> samples = new ArrayList<>();

        int fraudCount = (int) (totalSamples * 0.08); // ~8% fraud
        int legitimateCount = totalSamples - fraudCount;

        // Generate legitimate transactions
        for (int i = 0; i < legitimateCount; i++) {
            samples.add(generateLegitimate(rng));
        }

        // Generate fraudulent transactions
        for (int i = 0; i < fraudCount; i++) {
            samples.add(generateFraudulent(rng));
        }

        // Shuffle to mix fraud and legitimate
        java.util.Collections.shuffle(samples, rng);
        return samples;
    }

    /**
     * Generates a legitimate (non-fraud) transaction sample.
     * Normal patterns: moderate amounts, daytime hours, domestic, known countries.
     */
    private static LabelledSample generateLegitimate(Random rng) {
        // Amount: typically 100-25,000 with occasional higher (log-normal distribution)
        double amount = Math.exp(rng.nextGaussian() * 1.5 + 7.5); // median ~1800
        amount = Math.max(50, Math.min(amount, 45000));

        // Z-score: typically near 0 for legitimate transactions
        double zScore = rng.nextGaussian() * 0.8;

        // Hour: mostly business hours (8-20) with some evening
        int hour = 8 + rng.nextInt(12);
        if (rng.nextDouble() < 0.15) hour = 6 + rng.nextInt(17); // wider spread

        // Velocity: typically 0-2 recent transactions
        int velocity = rng.nextInt(3);

        // New country: rarely for legitimate
        boolean newCountry = rng.nextDouble() < 0.05;

        // International: ~15% of legitimate
        boolean international = rng.nextDouble() < 0.15;

        // Round amount: occasionally
        boolean roundAmount = (amount >= 10000 && amount % 1000 == 0);

        double[] features = new double[]{
            amount, zScore, hour, velocity,
            newCountry ? 1.0 : 0.0,
            international ? 1.0 : 0.0,
            roundAmount ? 1.0 : 0.0
        };

        return new LabelledSample(features, 0.0);
    }

    /**
     * Generates a fraudulent transaction sample.
     * Exhibits suspicious patterns: high amounts, night hours, new countries, etc.
     */
    private static LabelledSample generateFraudulent(Random rng) {
        int pattern = rng.nextInt(5);
        double amount, zScore;
        int hour, velocity;
        boolean newCountry, international;

        switch (pattern) {
            case 0: // High amount at night
                amount = 50000 + rng.nextDouble() * 200000;
                zScore = 3.0 + rng.nextDouble() * 5.0;
                hour = rng.nextInt(5); // 0-4 AM
                velocity = rng.nextInt(2);
                newCountry = rng.nextDouble() < 0.4;
                international = rng.nextDouble() < 0.6;
                break;
            case 1: // Velocity burst
                amount = 5000 + rng.nextDouble() * 30000;
                zScore = 1.5 + rng.nextDouble() * 2.0;
                hour = rng.nextInt(24);
                velocity = 5 + rng.nextInt(6); // 5-10 in 10 min
                newCountry = rng.nextDouble() < 0.3;
                international = rng.nextDouble() < 0.3;
                break;
            case 2: // International to unusual country
                amount = 20000 + rng.nextDouble() * 80000;
                zScore = 2.0 + rng.nextDouble() * 3.0;
                hour = 1 + rng.nextInt(5);
                velocity = rng.nextInt(3);
                newCountry = true;
                international = true;
                break;
            case 3: // Round suspicious amount
                amount = (10 + rng.nextInt(20)) * 5000.0; // 50,000 to 100,000 in steps
                zScore = 2.5 + rng.nextDouble() * 3.0;
                hour = rng.nextInt(6);
                velocity = rng.nextInt(4);
                newCountry = rng.nextDouble() < 0.5;
                international = rng.nextDouble() < 0.5;
                break;
            default: // Mixed signals
                amount = 30000 + rng.nextDouble() * 70000;
                zScore = 2.0 + rng.nextDouble() * 4.0;
                hour = rng.nextInt(24);
                velocity = 3 + rng.nextInt(4);
                newCountry = rng.nextDouble() < 0.6;
                international = rng.nextDouble() < 0.5;
                break;
        }

        boolean roundAmount = (amount >= 10000 && amount % 1000 == 0);

        double[] features = new double[]{
            amount, zScore, hour, velocity,
            newCountry ? 1.0 : 0.0,
            international ? 1.0 : 0.0,
            roundAmount ? 1.0 : 0.0
        };

        return new LabelledSample(features, 1.0);
    }
}
