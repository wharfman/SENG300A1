/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Pricing strategy used by Student while the student's verification is current.
 */
package gocycle.pricing;

/** Reduced per-minute rate for verified students: $1.25 + $0.25/min. */
public class StudentPricing extends BasePricing {
    public StudentPricing() {
        super(1.25, 0.25);
    }
}
