/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Part of the payments package; zones list the payment types they accept (configured in
 * {@code config/zones.properties}), and the rider app shows the labels when a rider picks how to pay.
 */
package gocycle.payments;

/** The kinds of payment GoCycle supports, each with a display label. One per {@link PaymentMethod} class. */
public enum PaymentType {
    CREDIT_CARD("Credit card"),
    TRANSIT_CARD("Transit card"),
    PHONE_WALLET("Phone wallet");

    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
