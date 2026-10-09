-- =====================================================================
-- AI-Powered Financial Fraud Detection & Transaction Monitoring System
-- Database Schema Definition (docs/schema.sql)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS fraud_detection_db;
USE fraud_detection_db;

-- ---------------------------------------------------------------------
-- 1. USERS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  salt VARCHAR(32) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100),
  role ENUM('CUSTOMER','ADMIN') NOT NULL,
  home_country VARCHAR(50) DEFAULT 'India',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2. TRANSACTIONS TABLE
-- Note: 'PENDING' is added as default status for transaction processing flow
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
  txn_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  txn_type ENUM('DOMESTIC','INTERNATIONAL') NOT NULL,
  receiver_account VARCHAR(30) NOT NULL,
  location VARCHAR(100) NOT NULL,
  country VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  txn_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  risk_score INT DEFAULT 0,
  risk_level ENUM('LOW','MEDIUM','HIGH') DEFAULT 'LOW',
  status ENUM('PENDING','APPROVED','FLAGGED','BLOCKED') DEFAULT 'PENDING',
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3. ALERTS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alerts (
  alert_id INT AUTO_INCREMENT PRIMARY KEY,
  txn_id INT NOT NULL,
  user_id INT NOT NULL,
  risk_level ENUM('MEDIUM','HIGH') NOT NULL,
  reasons TEXT NOT NULL,
  alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved BOOLEAN DEFAULT FALSE,
  admin_note VARCHAR(255),
  FOREIGN KEY (txn_id) REFERENCES transactions(txn_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 4. SETTINGS TABLE
-- Configurable detection rules and thresholds
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS settings (
  setting_key VARCHAR(50) PRIMARY KEY,
  setting_value VARCHAR(100) NOT NULL,
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- DEFAULT SETTINGS SEED DATA
-- ---------------------------------------------------------------------
INSERT INTO settings (setting_key, setting_value, description) VALUES
 ('HIGH_AMOUNT_THRESHOLD','50000','Amount above which a transaction is considered high'),
 ('VELOCITY_MAX_TXNS','5','Max transactions allowed in the velocity window'),
 ('VELOCITY_WINDOW_MIN','10','Velocity window in minutes'),
 ('ZSCORE_THRESHOLD','3.0','Z-score above which amount is anomalous'),
 ('MEDIUM_RISK_CUTOFF','40','Score for MEDIUM risk'),
 ('HIGH_RISK_CUTOFF','70','Score for HIGH risk'),
 ('RULE_HIGH_AMOUNT_ENABLED','true','Enable high amount rule'),
 ('RULE_VELOCITY_ENABLED','true','Enable velocity rule'),
 ('RULE_ANOMALY_ENABLED','true','Enable statistical anomaly rule'),
 ('RULE_TIME_ENABLED','true','Enable unusual time rule'),
 ('RULE_LOCATION_ENABLED','true','Enable new location rule'),
 ('RULE_ROUND_ENABLED','true','Enable round amount rule'),
 ('RULE_REPEAT_ENABLED','true','Enable rapid repeat rule')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
