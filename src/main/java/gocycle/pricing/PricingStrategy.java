/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Strategy interface of the pricing package. Each Rider supplies one, the Trip keeps it from the moment
 * it starts, and TripService uses it to work out the final charge.
 */
package gocycle.pricing;

/**
 * Calculates what a trip costs from its length. Each rider kind supplies its own implementation
 * (Strategy pattern); {@link LongTripSurcharge} decorates it with the long-trip rule.
 */
public interface PricingStrategy {
    /**
     * @param minutes billed trip length in whole minutes (trips round partial minutes up)
     * @return the price in dollars, rounded to the cent
     */
    double calculate(long minutes);

    /** One-line summary shown to riders, e.g. "$1.25 unlock + $0.35/min". */
    String describe();
}
