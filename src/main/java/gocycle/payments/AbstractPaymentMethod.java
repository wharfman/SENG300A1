package gocycle.payments;

import gocycle.common.Money;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hold and charge bookkeeping shared by every payment kind. It stands in for a real payment
 * processor, so every charge is approved.
 */
public abstract class AbstractPaymentMethod implements PaymentMethod {
    private static final AtomicInteger NEXT_HOLD = new AtomicInteger(1);

    private final Map<String, Double> activeHolds = new LinkedHashMap<>();
    private double totalCharged;

    @Override
    public String placeHold(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Hold amount must be positive");
        }
        String ref = "H-" + NEXT_HOLD.getAndIncrement();
        activeHolds.put(ref, amount);
        return ref;
    }

    @Override
    public void releaseHold(String ref) {
        if (activeHolds.remove(ref) == null) {
            throw new IllegalArgumentException("No active hold " + ref);
        }
    }

    @Override
    public boolean charge(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Charge must not be negative");
        }
        totalCharged = Money.round(totalCharged + amount);
        return true;
    }

    public double getHeldAmount() {
        return activeHolds.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getTotalCharged() {
        return totalCharged;
    }

    protected static String lastFour(String number) {
        String digits = number.replaceAll("\\D", "");
        return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    }
}
