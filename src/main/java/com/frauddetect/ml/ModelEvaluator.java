package com.frauddetect.ml;

import java.util.List;

/**
 * Evaluates the logistic regression model's performance using standard
 * classification metrics: precision, recall, F1-score, and confusion matrix.
 * Performs a 70/30 train/test split for unbiased evaluation.
 *
 * OOP Concept: Encapsulation — evaluation metrics are encapsulated in the
 * inner EvaluationResult class; splitting logic is hidden.
 */
public final class ModelEvaluator {

    /** Threshold for converting probability to binary prediction */
    private static final double DECISION_THRESHOLD = 0.5;

    private ModelEvaluator() {}

    /**
     * Container for classification evaluation metrics.
     */
    public static class EvaluationResult {
        private final int truePositives;
        private final int trueNegatives;
        private final int falsePositives;
        private final int falseNegatives;
        private final double precision;
        private final double recall;
        private final double f1Score;
        private final double accuracy;

        public EvaluationResult(int tp, int tn, int fp, int fn) {
            this.truePositives = tp;
            this.trueNegatives = tn;
            this.falsePositives = fp;
            this.falseNegatives = fn;

            // Precision = TP / (TP + FP)
            this.precision = (tp + fp > 0) ? (double) tp / (tp + fp) : 0.0;

            // Recall = TP / (TP + FN)
            this.recall = (tp + fn > 0) ? (double) tp / (tp + fn) : 0.0;

            // F1 = 2 * (precision * recall) / (precision + recall)
            this.f1Score = (precision + recall > 0) ? 2.0 * precision * recall / (precision + recall) : 0.0;

            // Accuracy = (TP + TN) / total
            int total = tp + tn + fp + fn;
            this.accuracy = (total > 0) ? (double) (tp + tn) / total : 0.0;
        }

        public int getTruePositives()  { return truePositives; }
        public int getTrueNegatives()  { return trueNegatives; }
        public int getFalsePositives() { return falsePositives; }
        public int getFalseNegatives() { return falseNegatives; }
        public double getPrecision()   { return precision; }
        public double getRecall()      { return recall; }
        public double getF1Score()     { return f1Score; }
        public double getAccuracy()    { return accuracy; }

        /**
         * Returns a formatted confusion matrix and metrics summary.
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("╔═══════════════════════════════════════════╗\n");
            sb.append("║     ML MODEL EVALUATION METRICS           ║\n");
            sb.append("╠═══════════════════════════════════════════╣\n");
            sb.append(String.format("║  Accuracy:   %6.2f%%                      ║%n", accuracy * 100));
            sb.append(String.format("║  Precision:  %6.2f%%                      ║%n", precision * 100));
            sb.append(String.format("║  Recall:     %6.2f%%                      ║%n", recall * 100));
            sb.append(String.format("║  F1 Score:   %6.4f                      ║%n", f1Score));
            sb.append("╠═══════════════════════════════════════════╣\n");
            sb.append("║            CONFUSION MATRIX               ║\n");
            sb.append("║                 Predicted                  ║\n");
            sb.append("║              Legit    Fraud                ║\n");
            sb.append(String.format("║  Actual Legit  %4d    %4d  (TN / FP)     ║%n", trueNegatives, falsePositives));
            sb.append(String.format("║  Actual Fraud  %4d    %4d  (FN / TP)     ║%n", falseNegatives, truePositives));
            sb.append("╚═══════════════════════════════════════════╝\n");
            return sb.toString();
        }

        @Override
        public String toString() {
            return String.format("EvalResult[accuracy=%.2f%%, precision=%.2f%%, recall=%.2f%%, F1=%.4f, TP=%d, TN=%d, FP=%d, FN=%d]",
                    accuracy * 100, precision * 100, recall * 100, f1Score,
                    truePositives, trueNegatives, falsePositives, falseNegatives);
        }
    }

    /**
     * Trains the model on 70% of the data and evaluates on the remaining 30%.
     *
     * @param samples   the full labelled dataset
     * @param lr        learning rate for training
     * @param epochs    number of training epochs
     * @param seed      random seed for split reproducibility
     * @return EvaluationResult with confusion matrix and metrics
     */
    public static EvaluationResult trainAndEvaluate(List<DataGenerator.LabelledSample> samples,
                                                     double lr, int epochs, long seed) {
        // Shuffle samples for random split
        java.util.List<DataGenerator.LabelledSample> shuffled = new java.util.ArrayList<>(samples);
        java.util.Collections.shuffle(shuffled, new java.util.Random(seed));

        int splitIndex = (int) (shuffled.size() * 0.7);

        // Split into train and test sets
        double[][] trainX = new double[splitIndex][];
        double[] trainY = new double[splitIndex];
        for (int i = 0; i < splitIndex; i++) {
            trainX[i] = shuffled.get(i).getFeatures();
            trainY[i] = shuffled.get(i).getLabel();
        }

        int testSize = shuffled.size() - splitIndex;
        double[][] testX = new double[testSize][];
        double[] testY = new double[testSize];
        for (int i = 0; i < testSize; i++) {
            testX[i] = shuffled.get(splitIndex + i).getFeatures();
            testY[i] = shuffled.get(splitIndex + i).getLabel();
        }

        // Train the model
        LogisticRegressionModel model = new LogisticRegressionModel(FeatureExtractor.FEATURE_COUNT);
        model.train(trainX, trainY, lr, epochs);

        // Evaluate on test set
        return evaluate(model, testX, testY);
    }

    /**
     * Evaluates a trained model on a test dataset.
     *
     * @param model the trained logistic regression model
     * @param testX test feature vectors
     * @param testY test labels
     * @return EvaluationResult
     */
    public static EvaluationResult evaluate(LogisticRegressionModel model,
                                             double[][] testX, double[] testY) {
        int tp = 0, tn = 0, fp = 0, fn = 0;

        for (int i = 0; i < testX.length; i++) {
            double prob = model.predictProbability(testX[i]);
            boolean predicted = prob >= DECISION_THRESHOLD;
            boolean actual = testY[i] >= 0.5;

            if (predicted && actual)      tp++;
            else if (!predicted && !actual) tn++;
            else if (predicted)           fp++;
            else                          fn++;
        }

        return new EvaluationResult(tp, tn, fp, fn);
    }

    /**
     * Trains the model on the full dataset (no split) and returns both the
     * trained model and its evaluation metrics on the training data.
     *
     * @param samples all labelled samples
     * @param lr      learning rate
     * @param epochs  number of epochs
     * @return trained LogisticRegressionModel
     */
    public static LogisticRegressionModel trainFull(List<DataGenerator.LabelledSample> samples,
                                                     double lr, int epochs) {
        double[][] X = new double[samples.size()][];
        double[] y = new double[samples.size()];
        for (int i = 0; i < samples.size(); i++) {
            X[i] = samples.get(i).getFeatures();
            y[i] = samples.get(i).getLabel();
        }

        LogisticRegressionModel model = new LogisticRegressionModel(FeatureExtractor.FEATURE_COUNT);
        model.train(X, y, lr, epochs);
        return model;
    }
}
