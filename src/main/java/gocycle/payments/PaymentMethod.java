/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Abstraction at the centre of the payments package: {@code TripService} holds deposits and charges fares
 * through it, and each {@code Zone} decides which {@link PaymentType}s it accepts.
 */
package gocycle.payments;

/**
 * A way to pay. Trips depend only on this interface, never on a concrete payment kind. All amounts are
 * in dollars.
 */
public interface PaymentMethod {
    /** Used by zones to decide whether this method is accepted. */
    PaymentType getType();

    /** Places a refundable hold and returns its reference. */
    String placeHold(double amount);

    /** Releases the hold identified by a reference from {@link #placeHold}. */
    void releaseHold(String ref);

    /** Returns false when the charge is declined. */
    boolean charge(double amount);

    /** Masked label safe to show on screens, e.g. "Credit card **** 1111". */
    String describe();
}
