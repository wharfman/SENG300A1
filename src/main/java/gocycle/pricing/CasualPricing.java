package gocycle.pricing;

/** Standard unlock fee and per-minute rate. */
public class CasualPricing extends BasePricing {
    public CasualPricing() {
        super(1.25, 0.35);
    }
}
