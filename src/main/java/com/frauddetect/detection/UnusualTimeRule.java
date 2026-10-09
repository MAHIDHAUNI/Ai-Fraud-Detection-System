package com.frauddetect.detection;

import com.frauddetect.model.Transaction;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * RUBRIC: 1 - OOP: Polymorphism (FraudRule Implementation)
 * Flags transactions executed during unusual nocturnal hours (00:00 to 05:00),
 * when account takeover and automated unauthorized drains frequently occur.
 */
public class UnusualTimeRule extends AbstractFraudRule {

    private int startHour; // 0 (midnight)
    private int endHour;   // 5 (5:00 AM)

    public UnusualTimeRule(double weight, boolean enabled) {
        super("Unusual Time Rule", weight, enabled);
        this.startHour = 0;
        this.endHour = 5;
    }

    public UnusualTimeRule(int startHour, int endHour, double weight, boolean enabled) {
        super("Unusual Time Rule", weight, enabled);
        this.startHour = startHour;
        this.endHour = endHour;
    }

    @Override
    public RuleResult evaluate(Transaction transaction, UserHistory history) {
        if (!isEnabled()) {
            return RuleResult.passed();
        }

        Timestamp txnTime = transaction.getTxnTime();
        LocalDateTime dt = (txnTime != null) ? txnTime.toLocalDateTime() : LocalDateTime.now();
        int hour = dt.getHour();
        int minute = dt.getMinute();

        // Check if executed between startHour (inclusive) and endHour (exclusive)
        if (hour >= startHour && hour < endHour) {
            int score = 55;
            String reason = String.format("Transaction conducted during high-risk night hours (%02d:%02d), outside standard customer operating windows.",
                    hour, minute);
            return RuleResult.flagged(score, reason);
        }

        return RuleResult.passed();
    }
}
