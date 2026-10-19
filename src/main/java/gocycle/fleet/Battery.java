/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Part of the fleet model: owned by each ElectricVehicle, drained while it is ridden and charged while
 * it is docked by the FleetSimulator clock.
 */
package gocycle.fleet;

/**
 * Charge level of an electric vehicle and the rule that it must be at least 20% to rent. The level is
 * always kept within 0-100%; draining or charging past either end stops at the limit.
 */
public class Battery {
    public static final int MIN_RENT_PERCENT = 20;

    private int chargePercent;

    /** Out-of-range starting values are clamped to 0-100%. */
    public Battery(int chargePercent) {
        this.chargePercent = clamp(chargePercent);
    }

    public boolean meetsRentThreshold() {
        return chargePercent >= MIN_RENT_PERCENT;
    }

    public int getChargePercent() {
        return chargePercent;
    }

    /** Removes {@code percent} percentage points of charge, stopping at 0%. */
    public void drain(int percent) {
        chargePercent = clamp(chargePercent - percent);
    }

    /** Adds {@code percent} percentage points of charge, stopping at 100%. */
    public void charge(int percent) {
        chargePercent = clamp(chargePercent + percent);
    }

    private static int clamp(int percent) {
        return Math.max(0, Math.min(100, percent));
    }
}
