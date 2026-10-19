/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Wraps every rider's pricing strategy: each Rider subclass returns its base pricing inside one of these.
 */
package gocycle.pricing;

import gocycle.common.Message;
import gocycle.common.Money;

import java.util.Objects;

/**
 * Decorator: wraps any pricing strategy and adds a flat surcharge once a trip runs past the
 * threshold, so the long-trip rule is written once for every rider kind. By default a trip longer than
 * 120 minutes pays an extra $5.00; a trip of exactly the threshold does not.
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

    /**
     * @param thresholdMinutes trips longer than this many minutes pay the surcharge
     * @param surcharge flat amount in dollars; not validated
     */
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
    public Message describe() {
        return new Message("fare.surcharge", inner.describe(), Money.format(surcharge), thresholdMinutes);
    }
}
