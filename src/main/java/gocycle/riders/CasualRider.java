package gocycle.riders;

import gocycle.pricing.CasualPricing;
import gocycle.pricing.LongTripSurcharge;
import gocycle.pricing.PricingStrategy;

/** Pays per trip at the standard rate. */
public class CasualRider extends Rider {
    public CasualRider(String riderId, String name, String email) {
        super(riderId, name, email);
    }

    @Override
    public PricingStrategy getPricingStrategy() {
        return new LongTripSurcharge(new CasualPricing());
    }

    @Override
    public String getKind() {
        return "Casual";
    }
}
