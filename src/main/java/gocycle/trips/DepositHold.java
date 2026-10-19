/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Trip-side record of the deposit that {@code TripService} places on the rider's payment method when a
 * trip starts; the {@link Trip} owns it and releases it when the vehicle is confirmed docked.
 */
package gocycle.trips;

import java.util.Objects;

/**
 * Refundable hold placed when a trip starts and released when it ends. Only tracks state on the trip
 * side: the matching hold on the payment method is identified by {@link #getHoldRef()} and released
 * separately by {@link Trip}.
 */
public class DepositHold {
    private final double amount;
    private final String holdRef;
    private boolean released;

    /**
     * @param amount  deposit in dollars
     * @param holdRef reference returned by {@code PaymentMethod.placeHold}
     */
    public DepositHold(double amount, String holdRef) {
        this.amount = amount;
        this.holdRef = Objects.requireNonNull(holdRef);
    }

    /**
     * Marks the deposit as released. Does not contact the payment method.
     *
     * @throws IllegalStateException if it was already released
     */
    public void release() {
        if (released) {
            throw new IllegalStateException("Deposit " + holdRef + " was already released");
        }
        released = true;
    }

    public double getAmount() {
        return amount;
    }

    public String getHoldRef() {
        return holdRef;
    }

    public boolean isReleased() {
        return released;
    }
}
