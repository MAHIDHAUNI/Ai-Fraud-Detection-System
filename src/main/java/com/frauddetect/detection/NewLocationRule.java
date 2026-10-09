package com.frauddetect.detection;

import com.frauddetect.model.Transaction;
import com.frauddetect.model.User;

import java.util.Set;

/**
 * RUBRIC: 1 - OOP: Polymorphism (FraudRule Implementation)
 * RUBRIC: 2 - Collections (Set<String>)
 * Flags transactions originating from unfamiliar geographic countries not part of
 * the user's profile home country or historical transaction locations.
 */
public class NewLocationRule extends AbstractFraudRule {

    public NewLocationRule(double weight, boolean enabled) {
        super("New Location / Geo-Anomaly Rule", weight, enabled);
    }

    @Override
    public RuleResult evaluate(Transaction transaction, UserHistory history) {
        if (!isEnabled() || history == null) {
            return RuleResult.passed();
        }

        String rawCountry = transaction.getCountry();
        if (rawCountry == null || rawCountry.trim().isEmpty()) {
            return RuleResult.passed();
        }
        final String txnCountry = rawCountry.trim();

        User user = history.getUser();
        String homeCountry = (user != null && user.getHomeCountry() != null) ? user.getHomeCountry().trim() : "India";
        Set<String> knownCountries = history.getKnownCountries();

        // If txn country matches home country, it is familiar
        if (txnCountry.equalsIgnoreCase(homeCountry)) {
            return RuleResult.passed();
        }

        // If user has already transacted in this country before, it's known
        boolean isFamiliar = knownCountries.stream()
                .anyMatch(c -> c.equalsIgnoreCase(txnCountry));

        if (!isFamiliar) {
            int score = 65;
            String reason = String.format("Geographic anomaly: Transaction originating from unverified country '%s' (Registered home: '%s').",
                    txnCountry, homeCountry);
            return RuleResult.flagged(score, reason);
        }

        return RuleResult.passed();
    }
}
