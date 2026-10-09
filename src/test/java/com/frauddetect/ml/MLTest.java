package com.frauddetect.ml;

import com.frauddetect.detection.FraudDetectionEngine;
import com.frauddetect.model.DetectionResult;
import com.frauddetect.model.DomesticTransaction;
import com.frauddetect.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MLTest {

    private LogisticRegressionModel model;

    @BeforeEach
    public void setUp() {
        model = new LogisticRegressionModel(FeatureExtractor.FEATURE_COUNT);
    }

    @Test
    @DisplayName("LogisticRegressionModel: Train and Predict on synthetic dataset")
    public void testModelTrainingAndPrediction() {
        List<DataGenerator.LabelledSample> dataset = DataGenerator.generateDataset(1000);
        assertEquals(1000, dataset.size(), "Dataset size should be 1000");

        ModelEvaluator.EvaluationResult metrics = ModelEvaluator.trainAndEvaluate(dataset, 0.05, 300, 42L);
        assertNotNull(metrics);
        assertTrue(metrics.getAccuracy() > 0.80, "Accuracy should be above 80%");
        assertTrue(metrics.getPrecision() >= 0.0, "Precision should be non-negative");
        assertTrue(metrics.getRecall() >= 0.0, "Recall should be non-negative");
        assertTrue(metrics.getF1Score() >= 0.0, "F1 score should be non-negative");
    }

    @Test
    @DisplayName("FeatureExtractor: Extract 7-feature vector")
    public void testFeatureExtractor() {
        Transaction txn = new DomesticTransaction(101, 25000.0, "DEBIT", "Amazon India", "ACC1001", "Mumbai");
        double[] features = FeatureExtractor.extractFeatures(txn, null);

        assertNotNull(features);
        assertEquals(7, features.length, "Feature vector length should be 7");
        assertEquals(25000.0, features[0], 0.01, "Feature 0 should be amount");
        assertEquals(0.0, features[5], 0.01, "Feature 5 should be 0 for domestic transaction");
    }

    @Test
    @DisplayName("DataGenerator: Generate realistic synthetic fraud distribution")
    public void testDataGeneratorDistribution() {
        List<DataGenerator.LabelledSample> samples = DataGenerator.generateDataset(2000);
        long fraudCount = samples.stream().filter(s -> s.getLabel() >= 0.5).count();
        double fraudRate = (double) fraudCount / samples.size();

        assertTrue(fraudRate >= 0.05 && fraudRate <= 0.15,
                String.format("Fraud rate (%.2f%%) should be approx 8%% (between 5%% and 15%%)", fraudRate * 100));
    }

    @Test
    @DisplayName("FraudDetectionEngine: Hybrid score calculation with ML_WEIGHT")
    public void testHybridScoring() {
        FraudDetectionEngine engine = new FraudDetectionEngine(null, null, null, null);
        assertTrue(engine.getModel().isTrained(), "Engine should auto-train model if empty");

        Transaction txn = new DomesticTransaction(101, 75000.0, "DEBIT", "Jewellery Shop", "ACC1001", "Delhi");

        // Analyze with ML_WEIGHT = 0.40
        engine.setMlWeight(0.40);
        DetectionResult result1 = engine.analyze(txn);

        // Analyze with ML_WEIGHT = 0.0 (Rules only)
        engine.setMlWeight(0.0);
        DetectionResult result2 = engine.analyze(txn);

        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.getScore() >= 0 && result1.getScore() <= 100);
        assertTrue(result2.getScore() >= 0 && result2.getScore() <= 100);
    }
}
