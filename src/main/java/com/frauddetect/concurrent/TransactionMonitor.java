package com.frauddetect.concurrent;

import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.detection.FraudDetectionEngine;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Alert;
import com.frauddetect.model.DetectionResult;
import com.frauddetect.model.Transaction;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * RUBRIC: 3 - Multithreading & Synchronization
 * Background Consumer Worker Thread implementing Runnable.
 * Continuously polls incoming transactions from a shared thread-safe BlockingQueue,
 * executes fraud risk analysis via FraudDetectionEngine, persists results and alerts
 * into MySQL through DAOs, and updates synchronized statistics.
 *
 * VIVA EXPLANATION:
 * Producer-Consumer Architecture:
 * 1. Decoupling: The GUI/Service (Producer) places transactions on a thread-safe
 *    BlockingQueue (LinkedBlockingQueue) without waiting for complex scoring queries.
 * 2. Asynchronous Execution: Multiple TransactionMonitor threads (Consumers) dequeue
 *    and evaluate transactions concurrently in the background, keeping the UI responsive.
 * 3. Thread Coordination: The BlockingQueue handles wait/notify coordination internally,
 *    blocking worker threads when the queue is empty and waking them when new work arrives.
 */
public class TransactionMonitor implements Runnable {

    private final String workerName;
    private final BlockingQueue<Transaction> queue;
    private final FraudDetectionEngine engine;
    private final TransactionDAO transactionDAO;
    private final AlertDAO alertDAO;
    private final StatsCounter statsCounter;
    private final BiConsumer<Transaction, DetectionResult> resultCallback;

    private volatile boolean running = true;

    public TransactionMonitor(String workerName,
                              BlockingQueue<Transaction> queue,
                              FraudDetectionEngine engine,
                              TransactionDAO transactionDAO,
                              AlertDAO alertDAO,
                              StatsCounter statsCounter,
                              BiConsumer<Transaction, DetectionResult> resultCallback) {
        this.workerName = workerName;
        this.queue = queue;
        this.engine = engine;
        this.transactionDAO = transactionDAO;
        this.alertDAO = alertDAO;
        this.statsCounter = statsCounter;
        this.resultCallback = resultCallback;
    }

    public TransactionMonitor(String workerName,
                              BlockingQueue<Transaction> queue,
                              FraudDetectionEngine engine,
                              TransactionDAO transactionDAO,
                              AlertDAO alertDAO,
                              StatsCounter statsCounter) {
        this(workerName, queue, engine, transactionDAO, alertDAO, statsCounter, null);
    }

    @Override
    public void run() {
        System.out.printf("[%s] Worker thread started. Listening for incoming transactions...%n", workerName);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Poll with timeout to allow checking running flag periodically
                Transaction txn = queue.poll(500, TimeUnit.MILLISECONDS);
                if (txn == null) {
                    continue;
                }

                // Process transaction through detection engine
                processTransaction(txn);

            } catch (InterruptedException e) {
                // Restore interrupted status and break out gracefully
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.printf("[%s] Error processing transaction: %s%n", workerName, e.getMessage());
            }
        }

        System.out.printf("[%s] Worker thread stopped gracefully.%n", workerName);
    }

    /**
     * Executes the risk scoring, database persistence, alert generation, and metric aggregation.
     */
    public void processTransaction(Transaction txn) {
        try {
            // 1. Evaluate transaction risk using hybrid engine
            DetectionResult result = engine.analyze(txn);

            // 2. Update transaction outcome in database
            if (transactionDAO != null && txn.getTxnId() > 0) {
                transactionDAO.updateRisk(
                        txn.getTxnId(),
                        result.getScore(),
                        result.getLevel(),
                        result.getRecommendedStatus()
                );
            }

            // 3. If risk is MEDIUM or HIGH, generate and persist a security alert
            if (result.isSuspicious() && alertDAO != null && txn.getTxnId() > 0) {
                Alert alert = new Alert(
                        txn.getTxnId(),
                        txn.getUserId(),
                        result.getLevel(),
                        result.getFormattedReasons()
                );
                alertDAO.save(alert);
            }

            // 4. Update synchronized shared statistics counter
            if (statsCounter != null) {
                statsCounter.recordTransaction(txn, result);
            }

            // 5. Invoke optional UI or service callback
            if (resultCallback != null) {
                resultCallback.accept(txn, result);
            }

        } catch (DatabaseException e) {
            System.err.printf("[%s] Database error during transaction #%d analysis: %s%n",
                    workerName, txn.getTxnId(), e.getMessage());
        }
    }

    /**
     * Signals the worker to stop processing new items.
     */
    public void stop() {
        this.running = false;
    }
}
