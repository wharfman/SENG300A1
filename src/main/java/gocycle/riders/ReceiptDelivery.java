/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Rider preference read by TripService, which sends each receipt through every sender channel the
 * preference includes.
 */
package gocycle.riders;

/**
 * Where a rider wants receipts delivered. Screens show the constant translated by {@code LocalizationService}
 * (key {@code ReceiptDelivery.<CONSTANT>}).
 */
public enum ReceiptDelivery {
    APP,
    EMAIL,
    BOTH;

    /** Whether receipts should go out on {@code channel}; {@link #BOTH} includes every channel. */
    public boolean includes(ReceiptDelivery channel) {
        return this == BOTH || this == channel;
    }
}
