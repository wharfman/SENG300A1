package gocycle.pricing;

/** Reduced per-minute rate for verified students. */
public class StudentPricing extends BasePricing {
    public StudentPricing() {
        super(1.25, 0.25);
    }
}
