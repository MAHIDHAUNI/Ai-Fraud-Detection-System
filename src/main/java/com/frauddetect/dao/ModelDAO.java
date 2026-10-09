package com.frauddetect.dao;

import com.frauddetect.db.DBConnection;
import com.frauddetect.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RUBRIC: 4 - Classes for database operations (ModelDAO)
 * RUBRIC: 6 - Implement JDBC: PreparedStatement, ResultSet, try-with-resources
 * Data access class for persisting and loading logistic regression model weights.
 */
public class ModelDAO {

    private final DBConnection dbConnection;

    public ModelDAO(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public ModelDAO() throws DatabaseException {
        this.dbConnection = DBConnection.getInstance();
    }

    /**
     * Loads all model weights from the database as a feature_name -> weight map.
     *
     * @return LinkedHashMap preserving insertion order
     * @throws DatabaseException on database error
     */
    public Map<String, Double> loadWeights() throws DatabaseException {
        String sql = "SELECT feature_name, weight FROM model_weights ORDER BY feature_name";
        Map<String, Double> weights = new LinkedHashMap<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                weights.put(rs.getString("feature_name"), rs.getDouble("weight"));
            }
            return weights;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load model weights.", e);
        }
    }

    /**
     * Saves or updates a single model weight.
     *
     * @param featureName the feature identifier
     * @param weight      the learned weight value
     * @throws DatabaseException on database error
     */
    public void saveWeight(String featureName, double weight) throws DatabaseException {
        String sql = "INSERT INTO model_weights (feature_name, weight) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE weight = ?, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, featureName);
            ps.setDouble(2, weight);
            ps.setDouble(3, weight);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save model weight: " + featureName, e);
        }
    }

    /**
     * Saves the full weight vector (bias + 7 features) to the database.
     *
     * @param bias          the bias weight
     * @param featureNames  the feature name array (length 7)
     * @param weights       the feature weight array (length 7)
     * @throws DatabaseException on database error
     */
    public void saveAllWeights(double bias, String[] featureNames, double[] weights) throws DatabaseException {
        saveWeight("bias", bias);
        for (int i = 0; i < featureNames.length && i < weights.length; i++) {
            saveWeight(featureNames[i], weights[i]);
        }
    }

    /**
     * Saves normalization parameters alongside weights for model portability.
     *
     * @param featureNames feature name array
     * @param means        mean values for normalization
     * @param stds         standard deviation values for normalization
     * @throws DatabaseException on database error
     */
    public void saveNormalizationParams(String[] featureNames, double[] means, double[] stds) throws DatabaseException {
        for (int i = 0; i < featureNames.length; i++) {
            saveWeight(featureNames[i] + "_mean", means[i]);
            saveWeight(featureNames[i] + "_std", stds[i]);
        }
    }

    /**
     * Loads normalization parameters from the database.
     *
     * @param featureNames feature name array
     * @return double[2][n] — [0] = means, [1] = stds
     * @throws DatabaseException on database error
     */
    public double[][] loadNormalizationParams(String[] featureNames) throws DatabaseException {
        Map<String, Double> allWeights = loadWeights();
        double[] means = new double[featureNames.length];
        double[] stds = new double[featureNames.length];

        for (int i = 0; i < featureNames.length; i++) {
            means[i] = allWeights.getOrDefault(featureNames[i] + "_mean", 0.0);
            stds[i] = allWeights.getOrDefault(featureNames[i] + "_std", 1.0);
        }
        return new double[][]{means, stds};
    }

    /**
     * High-level loader: populates a LogisticRegressionModel with weights and normalization params from DB.
     * Returns true if loaded successfully and non-empty.
     */
    public boolean loadModel(com.frauddetect.ml.LogisticRegressionModel model) throws DatabaseException {
        if (model == null) return false;
        Map<String, Double> map = loadWeights();
        if (map.isEmpty() || !map.containsKey("bias")) return false;

        String[] names = com.frauddetect.ml.FeatureExtractor.FEATURE_NAMES;
        double[] weights = new double[names.length + 1];
        weights[0] = map.getOrDefault("bias", 0.0);
        for (int i = 0; i < names.length; i++) {
            weights[i + 1] = map.getOrDefault(names[i], 0.0);
        }
        model.setWeights(weights);

        double[][] norm = loadNormalizationParams(names);
        model.setFeatureMeans(norm[0]);
        model.setFeatureStds(norm[1]);
        return true;
    }

    /**
     * High-level saver: persists model weights and normalization params to DB.
     */
    public void saveModel(com.frauddetect.ml.LogisticRegressionModel model) throws DatabaseException {
        if (model == null || !model.isTrained()) return;
        double[] w = model.getWeights();
        String[] names = com.frauddetect.ml.FeatureExtractor.FEATURE_NAMES;
        saveWeight("bias", w[0]);
        for (int i = 0; i < names.length; i++) {
            saveWeight(names[i], w[i + 1]);
        }
        saveNormalizationParams(names, model.getFeatureMeans(), model.getFeatureStds());
    }
}
