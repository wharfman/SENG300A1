/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Pricing strategy used by Member while the membership is active.
 */
package gocycle.pricing;

/** Lower per-minute rate for members: $1.25 + $0.20/min. The monthly fee is not part of trip prices. */
public class MemberPricing extends BasePricing {
    public MemberPricing() {
        super(1.25, 0.20);
    }
}
