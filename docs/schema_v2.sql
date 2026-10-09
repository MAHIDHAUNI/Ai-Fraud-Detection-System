-- ============================================================================
-- AI Fraud Detection System - Schema V2: ML Model Upgrade
-- Run this file AFTER schema.sql to add ML model support.
-- ============================================================================

-- Model weights table: stores learned parameters from logistic regression
CREATE TABLE IF NOT EXISTS model_weights (
  feature_name VARCHAR(50) PRIMARY KEY,
  weight DOUBLE NOT NULL DEFAULT 0.0,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Initialize with default (untrained) weights for the 7 features + bias
INSERT INTO model_weights (feature_name, weight) VALUES
  ('bias',              0.0),
  ('amount',            0.0),
  ('z_score',           0.0),
  ('hour_of_day',       0.0),
  ('txns_last_10min',   0.0),
  ('new_country_flag',  0.0),
  ('international_flag',0.0),
  ('round_amount_flag', 0.0)
ON DUPLICATE KEY UPDATE weight = weight;

-- Add confirmed_fraud column to alerts for feedback loop
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS confirmed_fraud BOOLEAN DEFAULT NULL;

-- Add ML_WEIGHT setting for hybrid scoring
INSERT INTO settings (setting_key, setting_value, description) VALUES
  ('ML_WEIGHT', '0.4', 'Weight of ML model in hybrid scoring (0.0 = rules only, 1.0 = ML only)')
ON DUPLICATE KEY UPDATE setting_value = setting_value;
