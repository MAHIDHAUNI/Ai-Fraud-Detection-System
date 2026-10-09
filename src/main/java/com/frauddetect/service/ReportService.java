package com.frauddetect.service;

import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.dao.UserDAO;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;
import com.frauddetect.model.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RUBRIC: 2 - Collections & Generics (groupingBy, Streams, Comparators, Maps, Lists)
 * Service generating analytical summaries, risk breakdowns, top riskiest user rankings,
 * 7-day daily trends, and CSV file exports for auditing.
 */
public class ReportService {

    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO;
    private final AlertDAO alertDAO;

    public ReportService(TransactionDAO transactionDAO, UserDAO userDAO, AlertDAO alertDAO) {
        this.transactionDAO = transactionDAO;
        this.userDAO = userDAO;
        this.alertDAO = alertDAO;
    }

    /**
     * DTO capturing high-level fraud monitoring overview metrics.
     */
    public static class SystemSummary {
        private final int totalTransactions;
        private final int approvedCount;
        private final int flaggedCount;
        private final int blockedCount;
        private final double totalVolume;
        private final double preventedFraudVolume;

        public SystemSummary(int totalTransactions, int approvedCount, int flaggedCount,
                             int blockedCount, double totalVolume, double preventedFraudVolume) {
            this.totalTransactions = totalTransactions;
            this.approvedCount = approvedCount;
            this.flaggedCount = flaggedCount;
            this.blockedCount = blockedCount;
            this.totalVolume = totalVolume;
            this.preventedFraudVolume = preventedFraudVolume;
        }

        public int getTotalTransactions() { return totalTransactions; }
        public int getApprovedCount() { return approvedCount; }
        public int getFlaggedCount() { return flaggedCount; }
        public int getBlockedCount() { return blockedCount; }
        public double getTotalVolume() { return totalVolume; }
        public double getPreventedFraudVolume() { return preventedFraudVolume; }

        public double getFraudPercentage() {
            if (totalTransactions == 0) return 0.0;
            return ((double) (flaggedCount + blockedCount) / totalTransactions) * 100.0;
        }

        @Override
        public String toString() {
            return String.format("Summary[Total: %d, Approved: %d, Flagged: %d, Blocked: %d, Volume: ₹%,.2f, Prevented: ₹%,.2f, FraudRate: %.2f%%]",
                    totalTransactions, approvedCount, flaggedCount, blockedCount, totalVolume, preventedFraudVolume, getFraudPercentage());
        }
    }

    /**
     * DTO representing a high-risk customer profile ranking.
     */
    public static class RiskyUserSummary {
        private final int userId;
        private final String username;
        private final String fullName;
        private final int totalTransactions;
        private final int suspiciousCount;
        private final int blockedCount;
        private final int totalRiskScore;
        private final double totalAmount;

        public RiskyUserSummary(int userId, String username, String fullName, int totalTransactions,
                                int suspiciousCount, int blockedCount, int totalRiskScore, double totalAmount) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.totalTransactions = totalTransactions;
            this.suspiciousCount = suspiciousCount;
            this.blockedCount = blockedCount;
            this.totalRiskScore = totalRiskScore;
            this.totalAmount = totalAmount;
        }

        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public int getTotalTransactions() { return totalTransactions; }
        public int getSuspiciousCount() { return suspiciousCount; }
        public int getBlockedCount() { return blockedCount; }
        public int getTotalRiskScore() { return totalRiskScore; }
        public double getTotalAmount() { return totalAmount; }

        public double getAverageRiskScore() {
            return totalTransactions > 0 ? ((double) totalRiskScore / totalTransactions) : 0.0;
        }

        @Override
        public String toString() {
            return String.format("RiskyUser[ID=%d, User='%s' (%s), Txns=%d, Suspicious=%d, Blocked=%d, AvgScore=%.1f]",
                    userId, username, fullName, totalTransactions, suspiciousCount, blockedCount, getAverageRiskScore());
        }
    }

    /**
     * DTO representing a single day's transaction activity and risk metrics.
     */
    public static class DailyTrend {
        private final String dateStr;
        private int totalCount;
        private int approvedCount;
        private int flaggedCount;
        private int blockedCount;
        private double totalVolume;
        private int sumScore;

        public DailyTrend(String dateStr) {
            this.dateStr = dateStr;
        }

        public void addTransaction(Transaction t) {
            totalCount++;
            totalVolume += t.getAmount();
            sumScore += t.getRiskScore();

            String status = t.getStatus();
            if ("BLOCKED".equalsIgnoreCase(status) || t.getRiskLevel() == RiskLevel.HIGH) {
                blockedCount++;
            } else if ("FLAGGED".equalsIgnoreCase(status) || t.getRiskLevel() == RiskLevel.MEDIUM) {
                flaggedCount++;
            } else {
                approvedCount++;
            }
        }

        public String getDateStr() { return dateStr; }
        public int getTotalCount() { return totalCount; }
        public int getApprovedCount() { return approvedCount; }
        public int getFlaggedCount() { return flaggedCount; }
        public int getBlockedCount() { return blockedCount; }
        public double getTotalVolume() { return totalVolume; }
        public double getAverageRiskScore() { return totalCount > 0 ? ((double) sumScore / totalCount) : 0.0; }

        @Override
        public String toString() {
            return String.format("Day[%s: Total=%d, Approved=%d, Flagged=%d, Blocked=%d, Volume=₹%,.2f, AvgScore=%.1f]",
                    dateStr, totalCount, approvedCount, flaggedCount, blockedCount, totalVolume, getAverageRiskScore());
        }
    }

    /**
     * Computes the overall system summary statistics.
     */
    public SystemSummary getSummaryMetrics() throws DatabaseException {
        List<Transaction> transactions = transactionDAO.findAll();

        int total = transactions.size();
        int approved = 0;
        int flagged = 0;
        int blocked = 0;
        double volume = 0.0;
        double preventedVolume = 0.0;

        for (Transaction t : transactions) {
            volume += t.getAmount();
            String status = t.getStatus();
            if ("BLOCKED".equalsIgnoreCase(status) || t.getRiskLevel() == RiskLevel.HIGH) {
                blocked++;
                preventedVolume += t.getAmount();
            } else if ("FLAGGED".equalsIgnoreCase(status) || t.getRiskLevel() == RiskLevel.MEDIUM) {
                flagged++;
            } else {
                approved++;
            }
        }

        return new SystemSummary(total, approved, flagged, blocked, volume, preventedVolume);
    }

    /**
     * RUBRIC: 2 - Collections & Streams
     * Computes transaction counts grouped by RiskLevel using Collectors.groupingBy.
     *
     * @return Map<RiskLevel, Long>
     */
    public Map<RiskLevel, Long> getTransactionsPerRiskLevel() throws DatabaseException {
        List<Transaction> transactions = transactionDAO.findAll();

        Map<RiskLevel, Long> rawMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getRiskLevel,
                        Collectors.counting()
                ));

        // Ensure all risk levels are present in the EnumMap
        Map<RiskLevel, Long> resultMap = new EnumMap<>(RiskLevel.class);
        for (RiskLevel level : RiskLevel.values()) {
            resultMap.put(level, rawMap.getOrDefault(level, 0L));
        }
        return resultMap;
    }

    /**
     * RUBRIC: 2 - Collections & Streams (groupingBy, Custom Comparator, limit)
     * Calculates the top N highest risk customers based on flagged/blocked occurrences and total risk score.
     */
    public List<RiskyUserSummary> getTopRiskiestUsers(int limit) throws DatabaseException {
        List<Transaction> transactions = transactionDAO.findAll();
        Map<Integer, User> userCache = userDAO.findAll().stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        // Group transactions by userId
        Map<Integer, List<Transaction>> userTxnMap = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getUserId));

        List<RiskyUserSummary> riskyUsers = new ArrayList<>();

        for (Map.Entry<Integer, List<Transaction>> entry : userTxnMap.entrySet()) {
            int userId = entry.getKey();
            List<Transaction> txns = entry.getValue();

            User user = userCache.get(userId);
            String username = (user != null) ? user.getUsername() : "User #" + userId;
            String fullName = (user != null) ? user.getFullName() : "Unknown User";

            int totalTxns = txns.size();
            int suspicious = 0;
            int blocked = 0;
            int totalScore = 0;
            double totalAmount = 0.0;

            for (Transaction t : txns) {
                totalScore += t.getRiskScore();
                totalAmount += t.getAmount();
                if (t.getRiskLevel() == RiskLevel.HIGH || "BLOCKED".equalsIgnoreCase(t.getStatus())) {
                    blocked++;
                    suspicious++;
                } else if (t.getRiskLevel() == RiskLevel.MEDIUM || "FLAGGED".equalsIgnoreCase(t.getStatus())) {
                    suspicious++;
                }
            }

            riskyUsers.add(new RiskyUserSummary(userId, username, fullName, totalTxns,
                    suspicious, blocked, totalScore, totalAmount));
        }

        // Sort descending by: (1) blocked count, (2) suspicious count, (3) total risk score
        return riskyUsers.stream()
                .sorted(Comparator.comparingInt(RiskyUserSummary::getBlockedCount)
                        .thenComparingInt(RiskyUserSummary::getSuspiciousCount)
                        .thenComparingInt(RiskyUserSummary::getTotalRiskScore)
                        .reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * RUBRIC: 2 - Collections (LinkedHashMap for chronological ordering)
     * Builds the daily transaction volume and risk trend for the last 7 calendar days.
     */
    public Map<String, DailyTrend> getDailyTrendLast7Days() throws DatabaseException {
        List<Transaction> transactions = transactionDAO.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, DailyTrend> trendMap = new LinkedHashMap<>();

        // Initialize 7 days in chronological sequence (from 6 days ago up to today)
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            String dateKey = today.minusDays(i).format(formatter);
            trendMap.put(dateKey, new DailyTrend(dateKey));
        }

        for (Transaction t : transactions) {
            Timestamp ts = t.getTxnTime();
            if (ts != null) {
                String dateKey = ts.toLocalDateTime().toLocalDate().format(formatter);
                if (trendMap.containsKey(dateKey)) {
                    trendMap.get(dateKey).addTransaction(t);
                }
            }
        }

        return trendMap;
    }

    /**
     * Exports an audit report containing system summary, risk distribution, and full transaction logs to a CSV file.
     *
     * @param targetFile the destination CSV file
     * @throws IOException       on file writing errors
     * @throws DatabaseException on database query errors
     */
    public void exportReportToCSV(File targetFile) throws IOException, DatabaseException {
        if (targetFile == null) {
            throw new IllegalArgumentException("Target CSV file cannot be null.");
        }

        SystemSummary summary = getSummaryMetrics();
        Map<RiskLevel, Long> riskMap = getTransactionsPerRiskLevel();
        List<Transaction> transactions = transactionDAO.findAll();
        List<RiskyUserSummary> topRiskyUsers = getTopRiskiestUsers(5);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            // Section 1: Executive Summary
            writer.write("=== AI FRAUD DETECTION & TRANSACTION MONITORING AUDIT REPORT ===");
            writer.newLine();
            writer.write("Generated On:," + java.time.LocalDateTime.now());
            writer.newLine();
            writer.newLine();

            writer.write("--- SYSTEM METRICS SUMMARY ---");
            writer.newLine();
            writer.write("Metric,Value");
            writer.newLine();
            writer.write(String.format("Total Transactions Processed,%d%n", summary.getTotalTransactions()));
            writer.write(String.format("Approved Transactions,%d%n", summary.getApprovedCount()));
            writer.write(String.format("Flagged Transactions (Medium Risk),%d%n", summary.getFlaggedCount()));
            writer.write(String.format("Blocked Transactions (High Risk),%d%n", summary.getBlockedCount()));
            writer.write(String.format("Total Transaction Volume (INR),₹%,.2f%n", summary.getTotalVolume()));
            writer.write(String.format("Total Prevented Fraud Amount (INR),₹%,.2f%n", summary.getPreventedFraudVolume()));
            writer.write(String.format("System Fraud Detection Rate,%.2f%%%n", summary.getFraudPercentage()));
            writer.newLine();

            // Section 2: Risk Level Distribution
            writer.write("--- RISK LEVEL DISTRIBUTION ---");
            writer.newLine();
            writer.write("Risk Level,Count,Percentage");
            writer.newLine();
            for (Map.Entry<RiskLevel, Long> entry : riskMap.entrySet()) {
                double pct = summary.getTotalTransactions() > 0 ?
                        ((double) entry.getValue() / summary.getTotalTransactions()) * 100.0 : 0.0;
                writer.write(String.format("%s,%d,%.2f%%%n", entry.getKey(), entry.getValue(), pct));
            }
            writer.newLine();

            // Section 3: Top Riskiest Accounts
            writer.write("--- TOP 5 HIGHEST RISK ACCOUNTS ---");
            writer.newLine();
            writer.write("User ID,Username,Full Name,Total Txns,Flagged,Blocked,Avg Risk Score,Total Volume");
            writer.newLine();
            for (RiskyUserSummary r : topRiskyUsers) {
                writer.write(String.format("%d,%s,%s,%d,%d,%d,%.1f,₹%,.2f%n",
                        r.getUserId(), r.getUsername(), r.getFullName(), r.getTotalTransactions(),
                        r.getSuspiciousCount(), r.getBlockedCount(), r.getAverageRiskScore(), r.getTotalAmount()));
            }
            writer.newLine();

            // Section 4: Full Transaction Audit Log
            writer.write("--- TRANSACTION AUDIT LOGS ---");
            writer.newLine();
            writer.write("Txn ID,User ID,Amount (INR),Type,Category,Receiver Account,Location,Country,Timestamp,Risk Score,Risk Level,Status,Description");
            writer.newLine();

            for (Transaction t : transactions) {
                writer.write(String.format("%d,%d,%.2f,%s,\"%s\",%s,\"%s\",\"%s\",%s,%d,%s,%s,\"%s\"%n",
                        t.getTxnId(),
                        t.getUserId(),
                        t.getAmount(),
                        t.getTxnType(),
                        t.getCategory(),
                        t.getReceiverAccount(),
                        escapeCsv(t.getLocation()),
                        escapeCsv(t.getCountry()),
                        t.getTxnTime() != null ? t.getTxnTime().toString() : "N/A",
                        t.getRiskScore(),
                        t.getRiskLevel(),
                        t.getStatus(),
                        escapeCsv(t.getDescription())
                ));
            }
        }
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}
