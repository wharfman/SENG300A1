/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One of the concrete payment kinds in the payments package; riders choose it in the rider app when
 * starting a trip, and zones may accept or refuse it.
 */
package gocycle.payments;

import gocycle.common.Message;

import java.util.Objects;

/** City transit card payments. The full number is kept but only its last four digits are shown. */
public class TransitCardPayment extends AbstractPaymentMethod {
    private final String cardNumber;

    public TransitCardPayment(String cardNumber) {
        this.cardNumber = Objects.requireNonNull(cardNumber);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.TRANSIT_CARD;
    }

    @Override
    public Message describe() {
        return new Message("payment.card", getType(), "**** " + lastFour(cardNumber));
    }
}
