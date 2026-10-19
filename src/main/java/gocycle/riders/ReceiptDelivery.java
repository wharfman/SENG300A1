/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Rider preference read by TripService, which sends each receipt through every sender channel the
 * preference includes.
 */
package gocycle.riders;

/** Where a rider wants receipts delivered. */
public enum ReceiptDelivery {
    APP("Phone app"),
    EMAIL("Email"),
    BOTH("App and email");

    private final String label;

    ReceiptDelivery(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Whether receipts should go out on {@code channel}; {@link #BOTH} includes every channel. */
    public boolean includes(ReceiptDelivery channel) {
        return this == BOTH || this == channel;
    }
}
