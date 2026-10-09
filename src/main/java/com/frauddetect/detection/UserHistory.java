package com.frauddetect.detection;

import com.frauddetect.dao.TransactionDAO;
import com.frauddetect.model.Transaction;
import com.frauddetect.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates historical contextual data about a user required by fraud detection rules.
 * Includes past transactions, statistical metrics, and familiar locations.
 */
public class UserHistory {
    private final User user;
    private final List<Transaction> recentVelocityTransactions;
    private final List<Transaction> recentRepeatTransactions;
    private final TransactionDAO.AmountStats amountStats;
    private final Set<String> knownCountries;

    public UserHistory(User user,
                       List<Transaction> recentVelocityTransactions,
                       List<Transaction> recentRepeatTransactions,
                       TransactionDAO.AmountStats amountStats,
                       Set<String> knownCountries) {
        this.user = user;
        this.recentVelocityTransactions = recentVelocityTransactions != null ? new ArrayList<>(recentVelocityTransactions) : new ArrayList<>();
        this.recentRepeatTransactions = recentRepeatTransactions != null ? new ArrayList<>(recentRepeatTransactions) : new ArrayList<>();
        this.amountStats = amountStats != null ? amountStats : new TransactionDAO.AmountStats(0.0, 0.0, 0);
        this.knownCountries = knownCountries != null ? new HashSet<>(knownCountries) : new HashSet<>();
    }

    public User getUser() {
        return user;
    }

    public List<Transaction> getRecentVelocityTransactions() {
        return Collections.unmodifiableList(recentVelocityTransactions);
    }

    public List<Transaction> getRecentRepeatTransactions() {
        return Collections.unmodifiableList(recentRepeatTransactions);
    }

    public TransactionDAO.AmountStats getAmountStats() {
        return amountStats;
    }

    public Set<String> getKnownCountries() {
        return Collections.unmodifiableSet(knownCountries);
    }
}
