/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Pricing strategy for CasualRider, and the fallback rate for lapsed members and unverified students.
 */
package gocycle.pricing;

/** Standard unlock fee and per-minute rate: $1.25 + $0.35/min. */
public class CasualPricing extends BasePricing {
    public CasualPricing() {
        super(1.25, 0.35);
    }
}
