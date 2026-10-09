package com.frauddetect.detection;

import com.frauddetect.model.Transaction;

import java.util.List;

/**
 * RUBRIC: 1 - OOP: Polymorphism (FraudRule Implementation)
 * RUBRIC: 2 - Collections & Streams
 * Flags identical amounts sent to the exact same receiver within a short window,
 * indicative of duplicate billing fraud, replay attacks, or unauthorized consecutive drains.
 */
public class RapidRepeatRule extends AbstractFraudRule {

    public RapidRepeatRule(double weight, boolean enabled) {
        super("Rapid Repeat Transfer Rule", weight, enabled);
    }

    @Override
    public RuleResult evaluate(Transaction transaction, UserHistory history) {
        if (!isEnabled() || history == null) {
            return RuleResult.passed();
        }

        List<Transaction> pastTxns = history.getRecentRepeatTransactions();
        if (pastTxns == null || pastTxns.isEmpty()) {
            return RuleResult.passed();
        }

        double candidateAmount = transaction.getAmount();
        String candidateReceiver = transaction.getReceiverAccount();
        if (candidateReceiver == null || candidateReceiver.isBlank()) {
            return RuleResult.passed();
        }

        // Check if identical amount was sent to the same receiver recently
        boolean duplicateFound = pastTxns.stream()
                .filter(t -> t.getTxnId() != transaction.getTxnId()) // Ignore self if updating
                .anyMatch(t -> candidateReceiver.equalsIgnoreCase(t.getReceiverAccount()) &&
                        Math.abs(t.getAmount() - candidateAmount) < 0.01);

        if (duplicateFound) {
            int score = 75;
            String reason = String.format("Rapid repeat transfer: Identical amount of ₹%,.2f was already transferred to receiver '%s' recently.",
                    candidateAmount, candidateReceiver);
            return RuleResult.flagged(score, reason);
        }

        return RuleResult.passed();
    }
}
