/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Shared base of the per-rider pricing strategies (CasualPricing, MemberPricing, StudentPricing).
 */
package gocycle.pricing;

import gocycle.common.Money;

/**
 * Template for every trip price: unlock fee + minutes x per-minute rate. The formula is fixed here;
 * subclasses only supply the two numbers, both in dollars.
 */
public abstract class BasePricing implements PricingStrategy {
    protected final double unlockFee;
    protected final double perMinuteRate;

    /** @throws IllegalArgumentException if either amount is negative */
    protected BasePricing(double unlockFee, double perMinuteRate) {
        if (unlockFee < 0 || perMinuteRate < 0) {
            throw new IllegalArgumentException("Prices must not be negative");
        }
        this.unlockFee = unlockFee;
        this.perMinuteRate = perMinuteRate;
    }

    /** @throws IllegalArgumentException if {@code minutes} is negative */
    @Override
    public final double calculate(long minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("minutes must not be negative");
        }
        return Money.round(unlockFee + minutes * perMinuteRate);
    }

    @Override
    public String describe() {
        return Money.format(unlockFee) + " unlock + " + Money.format(perMinuteRate) + "/min";
    }
}
