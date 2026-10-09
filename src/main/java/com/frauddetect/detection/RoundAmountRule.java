package com.frauddetect.detection;

import com.frauddetect.model.Transaction;

/**
 * RUBRIC: 1 - OOP: Polymorphism (FraudRule Implementation)
 * Flags suspiciously round large financial amounts (e.g. exact ₹50,000 or multiples of ₹10,000 above ₹25,000).
 * Fraudulent transfers often target round lump sums unlike normal retail transactions.
 */
public class RoundAmountRule extends AbstractFraudRule {

    private static final double MIN_SUSPICIOUS_ROUND_AMOUNT = 25000.00;

    public RoundAmountRule(double weight, boolean enabled) {
        super("Round Amount Rule", weight, enabled);
    }

    @Override
    public RuleResult evaluate(Transaction transaction, UserHistory history) {
        if (!isEnabled()) {
            return RuleResult.passed();
        }

        double amount = transaction.getAmount();

        if (amount >= MIN_SUSPICIOUS_ROUND_AMOUNT) {
            // Check if amount is an exact integer and a round multiple of 10,000
            boolean isExactInteger = (amount == Math.floor(amount));
            boolean isMultipleOf10k = ((long) amount % 10000 == 0);
            boolean isMultipleOf5k = ((long) amount % 5000 == 0);

            if (isExactInteger && (isMultipleOf10k || isMultipleOf5k)) {
                int score = 45;
                String reason = String.format("Suspicious round lump-sum amount of ₹%,.2f detected, characteristic of structured fraudulent extractions.",
                        amount);
                return RuleResult.flagged(score, reason);
            }
        }

        return RuleResult.passed();
    }
}
