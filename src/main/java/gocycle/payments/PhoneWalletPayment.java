/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One of the concrete payment kinds in the payments package; riders choose it in the rider app when
 * starting a trip, and zones may accept or refuse it.
 */
package gocycle.payments;

import gocycle.common.Message;

import java.util.Objects;

/**
 * Phone wallet payments, identified by the wallet's payment token. The token is never shown on screen,
 * so {@link #describe()} is just the payment kind.
 */
public class PhoneWalletPayment extends AbstractPaymentMethod {
    private final String walletToken;

    public PhoneWalletPayment(String walletToken) {
        this.walletToken = Objects.requireNonNull(walletToken);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.PHONE_WALLET;
    }

    @Override
    public Message describe() {
        return new Message("payment.wallet", getType());
    }

    public String getWalletToken() {
        return walletToken;
    }
}
