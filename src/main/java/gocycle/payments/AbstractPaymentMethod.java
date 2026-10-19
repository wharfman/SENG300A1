/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Shared base of the concrete payment kinds in the payments package ({@link CreditCardPayment},
 * {@link TransitCardPayment}, {@link PhoneWalletPayment}); subclasses add only their type and label.
 */
package gocycle.payments;

import gocycle.common.Money;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hold and charge bookkeeping shared by every payment kind. It stands in for a real payment
 * processor, so every charge is approved. Amounts are in dollars.
 */
public abstract class AbstractPaymentMethod implements PaymentMethod {
    // Static so hold references are unique across all payment methods, not just within one.
    private static final AtomicInteger NEXT_HOLD = new AtomicInteger(1);

    private final Map<String, Double> activeHolds = new LinkedHashMap<>();
    private double totalCharged;

    /** @throws IllegalArgumentException if {@code amount} isn't positive */
    @Override
    public String placeHold(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Hold amount must be positive");
        }
        String ref = "H-" + NEXT_HOLD.getAndIncrement();
        activeHolds.put(ref, amount);
        return ref;
    }

    /** @throws IllegalArgumentException if {@code ref} isn't an active hold on this method */
    @Override
    public void releaseHold(String ref) {
        if (activeHolds.remove(ref) == null) {
            throw new IllegalArgumentException("No active hold " + ref);
        }
    }

    /**
     * Always approves; the running total is kept rounded to the cent.
     *
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    @Override
    public boolean charge(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Charge must not be negative");
        }
        totalCharged = Money.round(totalCharged + amount);
        return true;
    }

    /** Sum of the holds not yet released. */
    public double getHeldAmount() {
        return activeHolds.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getTotalCharged() {
        return totalCharged;
    }

    /** Last four digits of {@code number}, skipping non-digits such as spaces; all digits if there are fewer. */
    protected static String lastFour(String number) {
        String digits = number.replaceAll("\\D", "");
        return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    }
}
