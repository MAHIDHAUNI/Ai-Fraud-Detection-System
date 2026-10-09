package com.frauddetect.service;

import com.frauddetect.concurrent.StatsCounter;
import com.frauddetect.concurrent.TransactionMonitor;
import com.frauddetect.dao.AlertDAO;
import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.detection.FraudDetectionEngine;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.exception.InvalidTransactionException;
import com.frauddetect.model.DetectionResult;
import com.frauddetect.model.Transaction;
import com.frauddetect.util.Result;
import com.frauddetect.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * RUBRIC: 3 - Multithreading & Concurrency (ExecutorService, BlockingQueue, Producer-Consumer)
 * Service managing transaction submission, asynchronous queue dispatching,
 * worker thread lifecycle, and transaction retrieval.
 */
public class TransactionService {

    private static final int DEFAULT_WORKER_THREADS = 3;

    private final TransactionDAO transactionDAO;
    private final AlertDAO alertDAO;
    private final FraudDetectionEngine engine;
    private final StatsCounter statsCounter;

    // Concurrency components: Thread-safe BlockingQueue and ExecutorService
    private final BlockingQueue<Transaction> processingQueue;
    private final ExecutorService threadPool;
    private final List<TransactionMonitor> workerMonitors;

    public TransactionService(TransactionDAO transactionDAO,
                              AlertDAO alertDAO,
                              FraudDetectionEngine engine,
                              StatsCounter statsCounter,
                              BiConsumer<Transaction, DetectionResult> callback) {
        this.transactionDAO = transactionDAO;
        this.alertDAO = alertDAO;
        this.engine = engine;
        this.statsCounter = (statsCounter != null) ? statsCounter : new StatsCounter();

        this.processingQueue = new LinkedBlockingQueue<>();
        this.threadPool = Executors.newFixedThreadPool(DEFAULT_WORKER_THREADS);
        this.workerMonitors = new ArrayList<>();

        // Start 3 concurrent worker threads
        for (int i = 1; i <= DEFAULT_WORKER_THREADS; i++) {
            TransactionMonitor monitor = new TransactionMonitor(
                    "Worker-" + i,
                    processingQueue,
                    this.engine,
                    this.transactionDAO,
                    this.alertDAO,
                    this.statsCounter,
                    callback
            );
            workerMonitors.add(monitor);
            threadPool.submit(monitor);
        }
    }

    public TransactionService(TransactionDAO transactionDAO,
                              AlertDAO alertDAO,
                              FraudDetectionEngine engine) {
        this(transactionDAO, alertDAO, engine, new StatsCounter(), null);
    }

    /**
     * Producer Method: Validates transaction, inserts it initially as PENDING into the DB,
     * and queues it for asynchronous multithreaded analysis.
     *
     * @param transaction candidate transaction to submit
     * @return Result<Transaction> indicating queued status or validation error
     * @throws InvalidTransactionException if transaction input is invalid
     * @throws DatabaseException           if initial database persistence fails
     */
    public Result<Transaction> submit(Transaction transaction) throws InvalidTransactionException, DatabaseException {
        // 1. Validate transaction business rules (amount, account, location, country)
        Validator.validateTransaction(transaction);

        // 2. Set default initial status
        transaction.setStatus("PENDING");

        // 3. Persist initial transaction in MySQL to generate primary key ID
        Transaction savedTxn = transactionDAO.save(transaction);

        // 4. Producer: Place transaction onto the thread-safe BlockingQueue
        boolean queued = processingQueue.offer(savedTxn);
        if (!queued) {
            return Result.error("Transaction processing queue is currently saturated. Please retry.");
        }

        return Result.ok(savedTxn, "Transaction submitted successfully. Background analysis in progress.");
    }

    /**
     * Synchronous processing method: Evaluates transaction immediately on the calling thread.
     * Useful for direct console testing, unit tests, and synchronous submission flows.
     */
    public DetectionResult processSynchronous(Transaction transaction) throws InvalidTransactionException, DatabaseException {
        Validator.validateTransaction(transaction);
        transaction.setStatus("PENDING");

        Transaction savedTxn = transactionDAO.save(transaction);
        DetectionResult result = engine.analyze(savedTxn);

        // Update risk score and final status
        transactionDAO.updateRisk(savedTxn.getTxnId(), result.getScore(), result.getLevel(), result.getRecommendedStatus());

        // Create alert if suspicious
        if (result.isSuspicious()) {
            alertDAO.save(new com.frauddetect.model.Alert(
                    savedTxn.getTxnId(),
                    savedTxn.getUserId(),
                    result.getLevel(),
                    result.getFormattedReasons()
            ));
        }

        statsCounter.recordTransaction(savedTxn, result);
        return result;
    }

    public List<Transaction> getUserTransactions(int userId) throws DatabaseException {
        return transactionDAO.findByUser(userId);
    }

    public List<Transaction> getAllTransactions() throws DatabaseException {
        return transactionDAO.findAll();
    }

    public StatsCounter getStatsCounter() {
        return statsCounter;
    }

    public int getQueueDepth() {
        return processingQueue.size();
    }

    /**
     * Cleanly terminates background worker threads and shuts down the thread pool.
     */
    public void shutdown() {
        for (TransactionMonitor monitor : workerMonitors) {
            monitor.stop();
        }
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
