package gocycle.fleet;

/** Charge level of an electric vehicle and the rule that it must be at least 20% to rent. */
public class Battery {
    public static final int MIN_RENT_PERCENT = 20;

    private int chargePercent;

    public Battery(int chargePercent) {
        this.chargePercent = clamp(chargePercent);
    }

    public boolean meetsRentThreshold() {
        return chargePercent >= MIN_RENT_PERCENT;
    }

    public int getChargePercent() {
        return chargePercent;
    }

    public void drain(int percent) {
        chargePercent = clamp(chargePercent - percent);
    }

    public void charge(int percent) {
        chargePercent = clamp(chargePercent + percent);
    }

    private static int clamp(int percent) {
        return Math.max(0, Math.min(100, percent));
    }
}
