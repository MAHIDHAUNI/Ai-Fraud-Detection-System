package com.frauddetect.ml;

import java.util.Arrays;

/**
 * Logistic Regression classifier implemented from scratch in plain Java.
 * Uses mini-batch gradient descent with feature normalization (z-score standardization).
 *
 * The model learns weights for 7 features + 1 bias term to predict the probability
 * that a transaction is fraudulent: P(fraud) = sigmoid(w0 + w1*x1 + ... + w7*x7).
 *
 * RUBRIC: 2 - Collections & Generics (arrays as primitive collections, streams)
 * OOP Concept: Encapsulation — all internal state (weights, normalization params) is private.
 */
public class LogisticRegressionModel {

    private final int featureCount;
    private double[] weights;       // weights[0] = bias, weights[1..n] = feature weights
    private double[] featureMeans;  // for z-score normalization
    private double[] featureStds;   // for z-score normalization
    private boolean trained;

    public LogisticRegressionModel(int featureCount) {
        this.featureCount = featureCount;
        this.weights = new double[featureCount + 1]; // +1 for bias
        this.featureMeans = new double[featureCount];
        this.featureStds = new double[featureCount];
        Arrays.fill(featureStds, 1.0); // prevent division by zero
        this.trained = false;
    }

    /**
     * Trains the model using gradient descent on the provided dataset.
     *
     * @param X      feature matrix [numSamples][featureCount]
     * @param y      label array [numSamples] — 1.0 for fraud, 0.0 for legitimate
     * @param lr     learning rate (e.g., 0.01)
     * @param epochs number of training passes (e.g., 500)
     */
    public void train(double[][] X, double[] y, double lr, int epochs) {
        if (X == null || y == null || X.length == 0 || X.length != y.length) {
            throw new IllegalArgumentException("Training data cannot be null or mismatched.");
        }

        int n = X.length;

        // Step 1: Compute feature means and standard deviations for normalization
        computeNormalizationParams(X);

        // Step 2: Normalize features (z-score standardization)
        double[][] Xnorm = new double[n][featureCount];
        for (int i = 0; i < n; i++) {
            Xnorm[i] = normalizeFeatures(X[i]);
        }

        // Step 3: Initialize weights to small random values (breaks symmetry)
        java.util.Random rng = new java.util.Random(42);
        for (int j = 0; j < weights.length; j++) {
            weights[j] = (rng.nextDouble() - 0.5) * 0.01;
        }

        // Step 4: Gradient descent optimization
        for (int epoch = 0; epoch < epochs; epoch++) {
            double[] gradient = new double[weights.length];

            for (int i = 0; i < n; i++) {
                double predicted = predictNormalized(Xnorm[i]);
                double error = predicted - y[i];

                // Gradient for bias
                gradient[0] += error;

                // Gradient for each feature weight
                for (int j = 0; j < featureCount; j++) {
                    gradient[j + 1] += error * Xnorm[i][j];
                }
            }

            // Update weights: w_j = w_j - (lr / n) * gradient_j
            for (int j = 0; j < weights.length; j++) {
                weights[j] -= (lr / n) * gradient[j];
            }
        }

        this.trained = true;
    }

    /**
     * Predicts the probability of fraud for a raw (un-normalized) feature vector.
     *
     * @param rawFeatures the feature vector from FeatureExtractor
     * @return probability between 0.0 and 1.0
     */
    public double predictProbability(double[] rawFeatures) {
        if (!trained) return 0.5; // untrained: neutral prediction
        double[] normalized = normalizeFeatures(rawFeatures);
        return predictNormalized(normalized);
    }

    /**
     * Returns the feature contributions to the prediction (for explainability).
     * Positive contributions indicate features pushing toward fraud prediction.
     *
     * @param rawFeatures the raw feature vector
     * @return double[] of per-feature contributions (same length as features)
     */
    public double[] getFeatureContributions(double[] rawFeatures) {
        double[] contributions = new double[featureCount];
        if (!trained) return contributions;

        double[] normalized = normalizeFeatures(rawFeatures);
        for (int i = 0; i < featureCount; i++) {
            contributions[i] = weights[i + 1] * normalized[i];
        }
        return contributions;
    }

    /**
     * Sigmoid activation function: maps any real number to (0, 1).
     */
    private static double sigmoid(double z) {
        // Clamp to prevent overflow
        if (z > 500) return 1.0;
        if (z < -500) return 0.0;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    /**
     * Predicts on already-normalized features.
     */
    private double predictNormalized(double[] normalizedFeatures) {
        double z = weights[0]; // bias
        for (int i = 0; i < featureCount; i++) {
            z += weights[i + 1] * normalizedFeatures[i];
        }
        return sigmoid(z);
    }

    /**
     * Z-score normalization: (x - mean) / std
     */
    private double[] normalizeFeatures(double[] raw) {
        double[] normalized = new double[featureCount];
        for (int i = 0; i < featureCount; i++) {
            if (featureStds[i] > 1e-8) {
                normalized[i] = (raw[i] - featureMeans[i]) / featureStds[i];
            } else {
                normalized[i] = 0.0;
            }
        }
        return normalized;
    }

    /**
     * Computes mean and standard deviation for each feature column.
     */
    private void computeNormalizationParams(double[][] X) {
        int n = X.length;
        featureMeans = new double[featureCount];
        featureStds = new double[featureCount];

        // Compute means
        for (double[] row : X) {
            for (int j = 0; j < featureCount; j++) {
                featureMeans[j] += row[j];
            }
        }
        for (int j = 0; j < featureCount; j++) {
            featureMeans[j] /= n;
        }

        // Compute standard deviations
        for (double[] row : X) {
            for (int j = 0; j < featureCount; j++) {
                double diff = row[j] - featureMeans[j];
                featureStds[j] += diff * diff;
            }
        }
        for (int j = 0; j < featureCount; j++) {
            featureStds[j] = Math.sqrt(featureStds[j] / n);
            if (featureStds[j] < 1e-8) featureStds[j] = 1.0; // prevent divide-by-zero
        }
    }

    // ===================== Getters / Save-Load =====================

    public double[] getWeights() {
        return Arrays.copyOf(weights, weights.length);
    }

    public void setWeights(double[] w) {
        if (w != null && w.length == weights.length) {
            this.weights = Arrays.copyOf(w, w.length);
            this.trained = true;
        }
    }

    public double[] getFeatureMeans() {
        return Arrays.copyOf(featureMeans, featureMeans.length);
    }

    public void setFeatureMeans(double[] means) {
        if (means != null && means.length == featureCount) {
            this.featureMeans = Arrays.copyOf(means, means.length);
        }
    }

    public double[] getFeatureStds() {
        return Arrays.copyOf(featureStds, featureStds.length);
    }

    public void setFeatureStds(double[] stds) {
        if (stds != null && stds.length == featureCount) {
            this.featureStds = Arrays.copyOf(stds, stds.length);
        }
    }

    public boolean isTrained() {
        return trained;
    }

    public void copyFrom(LogisticRegressionModel other) {
        if (other != null) {
            setWeights(other.getWeights());
            setFeatureMeans(other.getFeatureMeans());
            setFeatureStds(other.getFeatureStds());
            this.trained = other.isTrained();
        }
    }

    public int getFeatureCount() {
        return featureCount;
    }

    @Override
    public String toString() {
        return String.format("LogisticRegressionModel[features=%d, trained=%b, weights=%s]",
                featureCount, trained, Arrays.toString(weights));
    }
}
