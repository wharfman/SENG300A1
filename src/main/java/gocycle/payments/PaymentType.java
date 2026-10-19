/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Part of the payments package; zones list the payment types they accept (configured in
 * {@code config/zones.properties}), and the rider app shows the labels when a rider picks how to pay.
 */
package gocycle.payments;

/**
 * The kinds of payment GoCycle supports, one per {@link PaymentMethod} class. Screens show the constant
 * translated by {@code LocalizationService} (key {@code PaymentType.<CONSTANT>}).
 */
public enum PaymentType {
    CREDIT_CARD,
    TRANSIT_CARD,
    PHONE_WALLET
}
