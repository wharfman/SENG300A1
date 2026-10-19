/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One of the concrete payment kinds in the payments package; riders choose it in the rider app when
 * starting a trip, and zones may accept or refuse it.
 */
package gocycle.payments;

import gocycle.common.Message;

/** Credit card payments. Only the masked number is kept. */
public class CreditCardPayment extends AbstractPaymentMethod {
    private final String maskedNumber;

    public CreditCardPayment(String cardNumber) {
        this.maskedNumber = "**** " + lastFour(cardNumber);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.CREDIT_CARD;
    }

    @Override
    public Message describe() {
        return new Message("payment.card", getType(), maskedNumber);
    }
}
