package com.frauddetect.detection;

import com.frauddetect.model.Transaction;

/**
 * RUBRIC: 1 - OOP: Interfaces & Polymorphism
 * Contract defining standard behavior for all modular fraud detection rules.
 * Enables runtime polymorphic evaluation within the hybrid risk-scoring engine.
 */
public interface FraudRule {

    /**
     * Returns the human-readable identifier of the rule.
     */
    String getName();

    /**
     * Checks if this rule is currently activated by admin settings.
     */
    boolean isEnabled();

    /**
     * Returns the relative importance weight of this rule in total risk computation.
     */
    double getWeight();

    /**
     * Evaluates a transaction in the context of the user's historical profile.
     *
     * @param transaction the candidate transaction
     * @param history contextual historical data for the user
     * @return RuleResult indicating whether risk was detected, score (0-100), and reason
     */
    RuleResult evaluate(Transaction transaction, UserHistory history);
}
