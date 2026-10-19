/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One of the concrete payment kinds in the payments package; riders choose it in the rider app when
 * starting a trip, and zones may accept or refuse it.
 */
package gocycle.payments;

import gocycle.common.Message;

/**
 * Phone wallet payments, identified by the wallet's payment token. The token is never shown on screen,
 * so {@link #describe()} is just the payment kind.
 */
public class PhoneWalletPayment extends AbstractPaymentMethod {
    @Override
    public PaymentType getType() {
        return PaymentType.PHONE_WALLET;
    }

    @Override
    public Message describe() {
        return new Message("payment.wallet", getType());
    }
}
