/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One of the three rider kinds; registered by GoCycleSystem and priced through CasualPricing.
 */
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
