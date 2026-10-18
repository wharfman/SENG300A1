package gocycle.pricing;

/** Calculates what a trip costs from its length. Each rider kind supplies its own implementation. */
public interface PricingStrategy {
    double calculate(long minutes);

    /** One-line summary shown to riders, e.g. "$1.25 unlock + $0.35/min". */
    String describe();
}
