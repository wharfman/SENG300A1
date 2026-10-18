package gocycle.pricing;

import gocycle.common.Money;

/**
 * Template for every trip price: unlock fee + minutes x per-minute rate. The formula is fixed here;
 * subclasses only supply the two numbers.
 */
public abstract class BasePricing implements PricingStrategy {
    protected final double unlockFee;
    protected final double perMinuteRate;

    protected BasePricing(double unlockFee, double perMinuteRate) {
        if (unlockFee < 0 || perMinuteRate < 0) {
            throw new IllegalArgumentException("Prices must not be negative");
        }
        this.unlockFee = unlockFee;
        this.perMinuteRate = perMinuteRate;
    }

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
