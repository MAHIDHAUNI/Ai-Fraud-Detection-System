package com.frauddetect.concurrent;

import com.frauddetect.model.DetectionResult;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;

/**
 * RUBRIC: 3 - Multithreading & Synchronization
 * Thread-safe statistics accumulator maintaining real-time transaction metrics.
 *
 * VIVA EXPLANATION:
 * Synchronization Mechanism:
 * Multiple background worker threads (TransactionMonitor instances) process incoming transactions
 * concurrently from a shared BlockingQueue. When a transaction finishes analysis, worker threads
 * concurrently invoke 'recordTransaction()'.
 *
 * All mutator and accessor methods are marked 'synchronized', acquiring the intrinsic lock (monitor)
 * of the StatsCounter instance. This guarantees:
 * 1. Mutual Exclusion: Prevents race conditions and lost update anomalies when incrementing counters.
 * 2. Memory Visibility: Ensures that updates made by one thread are immediately visible to reader
 *    threads (e.g. AdminDashboard monitor refresh thread).
 */
public class StatsCounter {

    private int totalProcessed;
    private int approvedCount;
    private int flaggedCount;
    private int blockedCount;
    private double totalVolumeAmount;
    private double blockedFraudAmount;

    public StatsCounter() {
        reset();
    }

    /**
     * Synchronously records the outcome of a processed transaction.
     * Synchronized to prevent lost updates across concurrent worker threads.
     *
     * @param transaction the evaluated transaction
     * @param result      the detection outcome
     */
    public synchronized void recordTransaction(Transaction transaction, DetectionResult result) {
        if (transaction == null || result == null) {
            return;
        }

        totalProcessed++;
        totalVolumeAmount += transaction.getAmount();

        String status = result.getRecommendedStatus();
        if ("BLOCKED".equalsIgnoreCase(status) || result.getLevel() == RiskLevel.HIGH) {
            blockedCount++;
            blockedFraudAmount += transaction.getAmount();
        } else if ("FLAGGED".equalsIgnoreCase(status) || result.getLevel() == RiskLevel.MEDIUM) {
            flaggedCount++;
        } else {
            approvedCount++;
        }
    }

    /**
     * Synchronously captures an immutable snapshot of current metrics for GUI display and reports.
     *
     * @return immutable StatsSnapshot
     */
    public synchronized StatsSnapshot getSnapshot() {
        return new StatsSnapshot(
                totalProcessed,
                approvedCount,
                flaggedCount,
                blockedCount,
                totalVolumeAmount,
                blockedFraudAmount
        );
    }

    /**
     * Resets all internal counters to zero under thread-safe synchronization.
     */
    public synchronized void reset() {
        this.totalProcessed = 0;
        this.approvedCount = 0;
        this.flaggedCount = 0;
        this.blockedCount = 0;
        this.totalVolumeAmount = 0.0;
        this.blockedFraudAmount = 0.0;
    }

    public synchronized int getTotalProcessed() {
        return totalProcessed;
    }

    public synchronized int getApprovedCount() {
        return approvedCount;
    }

    public synchronized int getFlaggedCount() {
        return flaggedCount;
    }

    public synchronized int getBlockedCount() {
        return blockedCount;
    }

    public synchronized double getTotalVolumeAmount() {
        return totalVolumeAmount;
    }

    public synchronized double getBlockedFraudAmount() {
        return blockedFraudAmount;
    }

    /**
     * Immutable value object capturing a point-in-time snapshot of system metrics.
     */
    public static class StatsSnapshot {
        private final int totalProcessed;
        private final int approvedCount;
        private final int flaggedCount;
        private final int blockedCount;
        private final double totalVolumeAmount;
        private final double blockedFraudAmount;

        public StatsSnapshot(int totalProcessed, int approvedCount, int flaggedCount,
                             int blockedCount, double totalVolumeAmount, double blockedFraudAmount) {
            this.totalProcessed = totalProcessed;
            this.approvedCount = approvedCount;
            this.flaggedCount = flaggedCount;
            this.blockedCount = blockedCount;
            this.totalVolumeAmount = totalVolumeAmount;
            this.blockedFraudAmount = blockedFraudAmount;
        }

        public int getTotalProcessed() {
            return totalProcessed;
        }

        public int getApprovedCount() {
            return approvedCount;
        }

        public int getFlaggedCount() {
            return flaggedCount;
        }

        public int getBlockedCount() {
            return blockedCount;
        }

        public double getTotalVolumeAmount() {
            return totalVolumeAmount;
        }

        public double getBlockedFraudAmount() {
            return blockedFraudAmount;
        }

        public double getFraudPercentage() {
            if (totalProcessed == 0) return 0.0;
            return ((double) (flaggedCount + blockedCount) / totalProcessed) * 100.0;
        }

        @Override
        public String toString() {
            return String.format("StatsSnapshot[Total: %d, Approved: %d, Flagged: %d, Blocked: %d, Volume: ₹%,.2f, Prevented: ₹%,.2f, FraudRate: %.1f%%]",
                    totalProcessed, approvedCount, flaggedCount, blockedCount, totalVolumeAmount, blockedFraudAmount, getFraudPercentage());
        }
    }
}
