package com.frauddetect.detection;

/**
 * RUBRIC: 1 - OOP: Abstraction & Polymorphism
 * Abstract base class implementing the FraudRule interface.
 * Encapsulates common state such as rule activation flag and scoring weight.
 */
public abstract class AbstractFraudRule implements FraudRule {

    private final String name;
    private double weight;
    private boolean enabled;

    public AbstractFraudRule(String name, double weight, boolean enabled) {
        this.name = name;
        this.weight = weight;
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return String.format("%s[name='%s', weight=%.2f, enabled=%b]",
                getClass().getSimpleName(), name, weight, enabled);
    }
}
