package gocycle.pricing;

/** Lower per-minute rate for members. */
public class MemberPricing extends BasePricing {
    public MemberPricing() {
        super(1.25, 0.20);
    }
}
