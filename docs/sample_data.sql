-- =====================================================================
-- AI-Powered Financial Fraud Detection & Transaction Monitoring System
-- Sample Data: Transactions (docs/sample_data.sql)
--
-- NOTE: As per project guidelines, User accounts are seeded securely
-- via the one-time database seeding utility in Main using PasswordUtil.
-- This file contains ~30 realistic sample transactions for demonstration.
-- =====================================================================

USE fraud_detection_db;

SET FOREIGN_KEY_CHECKS = 0;

-- Clean existing sample transactions to avoid duplication on re-run
DELETE FROM alerts;
DELETE FROM transactions;

-- ---------------------------------------------------------------------
-- TRANSACTIONS DATA (30 realistic transactions)
-- Customer IDs: 2 (john_doe), 3 (priya_sharma), 4 (rahul_verma)
-- ---------------------------------------------------------------------

-- User 2 (john_doe): Regular daily activity (Domestic, Low risk, Approved)
INSERT INTO transactions (txn_id, user_id, amount, txn_type, receiver_account, location, country, description, txn_time, risk_score, risk_level, status) VALUES
(1, 2, 450.00, 'DOMESTIC', '987654321001', 'Mumbai', 'India', 'Coffee and bakery', DATE_SUB(NOW(), INTERVAL 6 DAY), 5, 'LOW', 'APPROVED'),
(2, 2, 2200.00, 'DOMESTIC', '987654321002', 'Mumbai', 'India', 'Grocery supermarket', DATE_SUB(NOW(), INTERVAL 5 DAY), 8, 'LOW', 'APPROVED'),
(3, 2, 12500.00, 'DOMESTIC', '987654321003', 'Mumbai', 'India', 'Monthly electricity & water utilities', DATE_SUB(NOW(), INTERVAL 5 DAY), 12, 'LOW', 'APPROVED'),
(4, 2, 3500.00, 'DOMESTIC', '987654321004', 'Mumbai', 'India', 'Fuel station', DATE_SUB(NOW(), INTERVAL 4 DAY), 5, 'LOW', 'APPROVED'),
(5, 2, 1800.00, 'DOMESTIC', '987654321005', 'Pune', 'India', 'Weekend dining', DATE_SUB(NOW(), INTERVAL 3 DAY), 10, 'LOW', 'APPROVED'),
(6, 2, 25000.00, 'DOMESTIC', '987654321006', 'Mumbai', 'India', 'Apartment rent transfer', DATE_SUB(NOW(), INTERVAL 2 DAY), 15, 'LOW', 'APPROVED'),
(7, 2, 950.00, 'DOMESTIC', '987654321007', 'Mumbai', 'India', 'Pharmacy medicines', DATE_SUB(NOW(), INTERVAL 1 DAY), 5, 'LOW', 'APPROVED');

-- User 2 (john_doe): Suspicious transactions (High Amount, Unusual Time, Round Amount, New Location)
INSERT INTO transactions (txn_id, user_id, amount, txn_type, receiver_account, location, country, description, txn_time, risk_score, risk_level, status) VALUES
(8, 2, 95000.00, 'INTERNATIONAL', '112233445566', 'Dubai', 'UAE', 'High value electronics import', '2026-10-08 03:15:00', 92, 'HIGH', 'BLOCKED'),
(9, 2, 50000.00, 'DOMESTIC', '987654321008', 'Mumbai', 'India', 'Suspicious round amount transfer', DATE_SUB(NOW(), INTERVAL 18 HOUR), 48, 'MEDIUM', 'FLAGGED'),
(10, 2, 15000.00, 'DOMESTIC', '987654321009', 'Mumbai', 'India', 'Consulting fee installment 1', DATE_SUB(NOW(), INTERVAL 12 HOUR), 15, 'LOW', 'APPROVED'),
(11, 2, 15000.00, 'DOMESTIC', '987654321009', 'Mumbai', 'India', 'Consulting fee installment 2 (rapid repeat)', DATE_SUB(NOW(), INTERVAL 11 HOUR), 62, 'MEDIUM', 'FLAGGED');

-- User 3 (priya_sharma): Regular transactions
INSERT INTO transactions (txn_id, user_id, amount, txn_type, receiver_account, location, country, description, txn_time, risk_score, risk_level, status) VALUES
(12, 3, 1200.00, 'DOMESTIC', '887766554401', 'Bengaluru', 'India', 'Bookstore order', DATE_SUB(NOW(), INTERVAL 7 DAY), 5, 'LOW', 'APPROVED'),
(13, 3, 4300.00, 'DOMESTIC', '887766554402', 'Bengaluru', 'India', 'Departmental store', DATE_SUB(NOW(), INTERVAL 6 DAY), 8, 'LOW', 'APPROVED'),
(14, 3, 6200.00, 'DOMESTIC', '887766554403', 'Bengaluru', 'India', 'Flight ticket Bengaluru-Delhi', DATE_SUB(NOW(), INTERVAL 5 DAY), 12, 'LOW', 'APPROVED'),
(15, 3, 2800.00, 'DOMESTIC', '887766554404', 'Delhi', 'India', 'Hotel dining', DATE_SUB(NOW(), INTERVAL 4 DAY), 10, 'LOW', 'APPROVED'),
(16, 3, 850.00, 'DOMESTIC', '887766554405', 'Bengaluru', 'India', 'Cab rides payment', DATE_SUB(NOW(), INTERVAL 3 DAY), 5, 'LOW', 'APPROVED'),
(17, 3, 7500.00, 'DOMESTIC', '887766554406', 'Bengaluru', 'India', 'Online apparel purchase', DATE_SUB(NOW(), INTERVAL 2 DAY), 10, 'LOW', 'APPROVED');

-- User 3 (priya_sharma): Suspicious burst (Velocity burst within minutes & Statistical Anomaly)
INSERT INTO transactions (txn_id, user_id, amount, txn_type, receiver_account, location, country, description, txn_time, risk_score, risk_level, status) VALUES
(18, 3, 8000.00, 'DOMESTIC', '887766554410', 'Bengaluru', 'India', 'Rapid transfer 1', DATE_SUB(NOW(), INTERVAL 50 MINUTE), 15, 'LOW', 'APPROVED'),
(19, 3, 8000.00, 'DOMESTIC', '887766554411', 'Bengaluru', 'India', 'Rapid transfer 2', DATE_SUB(NOW(), INTERVAL 48 MINUTE), 25, 'LOW', 'APPROVED'),
(20, 3, 9000.00, 'DOMESTIC', '887766554412', 'Bengaluru', 'India', 'Rapid transfer 3', DATE_SUB(NOW(), INTERVAL 46 MINUTE), 35, 'LOW', 'APPROVED'),
(21, 3, 8500.00, 'DOMESTIC', '887766554413', 'Bengaluru', 'India', 'Rapid transfer 4', DATE_SUB(NOW(), INTERVAL 44 MINUTE), 55, 'MEDIUM', 'FLAGGED'),
(22, 3, 9500.00, 'DOMESTIC', '887766554414', 'Bengaluru', 'India', 'Rapid transfer 5 (velocity exceeded)', DATE_SUB(NOW(), INTERVAL 42 MINUTE), 78, 'HIGH', 'FLAGGED'),
(23, 3, 185000.00, 'DOMESTIC', '887766554415', 'Bengaluru', 'India', 'Massive anomalous investment', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 88, 'HIGH', 'BLOCKED');

-- User 4 (rahul_verma): Regular transactions
INSERT INTO transactions (txn_id, user_id, amount, txn_type, receiver_account, location, country, description, txn_time, risk_score, risk_level, status) VALUES
(24, 4, 3200.00, 'DOMESTIC', '776655443301', 'Hyderabad', 'India', 'Annual gym membership', DATE_SUB(NOW(), INTERVAL 8 DAY), 5, 'LOW', 'APPROVED'),
(25, 4, 1500.00, 'DOMESTIC', '776655443302', 'Hyderabad', 'India', 'Books & stationery', DATE_SUB(NOW(), INTERVAL 7 DAY), 5, 'LOW', 'APPROVED'),
(26, 4, 12000.00, 'DOMESTIC', '776655443303', 'Hyderabad', 'India', 'Electronic gadget repair', DATE_SUB(NOW(), INTERVAL 4 DAY), 10, 'LOW', 'APPROVED'),
(27, 4, 6500.00, 'INTERNATIONAL', '776655443304', 'San Francisco', 'USA', 'Cloud developer subscription', DATE_SUB(NOW(), INTERVAL 3 DAY), 22, 'LOW', 'APPROVED'),
(28, 4, 4200.00, 'DOMESTIC', '776655443305', 'Hyderabad', 'India', 'Health checkup lab tests', DATE_SUB(NOW(), INTERVAL 2 DAY), 5, 'LOW', 'APPROVED');

-- User 4 (rahul_verma): Suspicious international transactions
INSERT INTO transactions (txn_id, user_id, amount, txn_type, receiver_account, location, country, description, txn_time, risk_score, risk_level, status) VALUES
(29, 4, 82000.00, 'INTERNATIONAL', '334455667788', 'Moscow', 'Russia', 'Unusual location luxury goods', '2026-10-08 02:40:00', 85, 'HIGH', 'BLOCKED'),
(30, 4, 1000.00, 'DOMESTIC', '776655443306', 'Hyderabad', 'India', 'Mobile recharge', NOW(), 0, 'LOW', 'PENDING');

-- ---------------------------------------------------------------------
-- ALERTS FOR HIGH/MEDIUM RISK TRANSACTIONS (Sample initial alerts)
-- ---------------------------------------------------------------------
INSERT INTO alerts (alert_id, txn_id, user_id, risk_level, reasons, alert_time, resolved, admin_note) VALUES
(1, 8, 2, 'HIGH', 'Amount ₹95,000 exceeds threshold ₹50,000; Unusual transaction hour (03:15); New foreign country (UAE); International multiplier 1.2 applied.', '2026-10-08 03:15:05', FALSE, NULL),
(2, 9, 2, 'MEDIUM', 'Suspicious round amount ₹50,000 matches threshold.', DATE_SUB(NOW(), INTERVAL 18 HOUR), FALSE, NULL),
(3, 11, 2, 'MEDIUM', 'Rapid repeated transfer of identical amount ₹15,000 to receiver 987654321009 within short time window.', DATE_SUB(NOW(), INTERVAL 11 HOUR), FALSE, NULL),
(4, 22, 3, 'HIGH', 'Velocity limit exceeded: 5 transactions within 10 minutes.', DATE_SUB(NOW(), INTERVAL 42 MINUTE), FALSE, NULL),
(5, 23, 3, 'HIGH', 'Statistical anomaly: Amount ₹185,000 deviates significantly from historical user average (Z-Score > 3.0); Amount exceeds threshold ₹50,000.', DATE_SUB(NOW(), INTERVAL 30 MINUTE), FALSE, NULL),
(6, 29, 4, 'HIGH', 'High international amount ₹82,000; Unusual night hour (02:40); Unfamiliar country location (Russia).', '2026-10-08 02:40:05', FALSE, NULL);

SET FOREIGN_KEY_CHECKS = 1;
