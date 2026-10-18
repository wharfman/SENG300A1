package gocycle.payments;

/** A way to pay. Trips depend only on this interface, never on a concrete payment kind. */
public interface PaymentMethod {
    PaymentType getType();

    /** Places a refundable hold and returns its reference. */
    String placeHold(double amount);

    void releaseHold(String ref);

    /** Returns false when the charge is declined. */
    boolean charge(double amount);

    /** Masked label safe to show on screens, e.g. "Credit card **** 1111". */
    String describe();
}
