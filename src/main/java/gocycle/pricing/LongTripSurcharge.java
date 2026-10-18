package gocycle.pricing;

import gocycle.common.Money;

import java.util.Objects;

/**
 * Decorator: wraps any pricing strategy and adds a flat surcharge once a trip runs past the
 * threshold, so the long-trip rule is written once for every rider kind.
 */
public class LongTripSurcharge implements PricingStrategy {
    public static final long DEFAULT_THRESHOLD_MINUTES = 120;
    public static final double DEFAULT_SURCHARGE = 5.00;

    private final PricingStrategy inner;
    private final long thresholdMinutes;
    private final double surcharge;

    public LongTripSurcharge(PricingStrategy inner) {
        this(inner, DEFAULT_THRESHOLD_MINUTES, DEFAULT_SURCHARGE);
    }

    public LongTripSurcharge(PricingStrategy inner, long thresholdMinutes, double surcharge) {
        this.inner = Objects.requireNonNull(inner);
        this.thresholdMinutes = thresholdMinutes;
        this.surcharge = surcharge;
    }

    @Override
    public double calculate(long minutes) {
        double base = inner.calculate(minutes);
        return minutes > thresholdMinutes ? Money.round(base + surcharge) : base;
    }

    @Override
    public String describe() {
        return inner.describe() + ", +" + Money.format(surcharge) + " over " + thresholdMinutes + " min";
    }
}
