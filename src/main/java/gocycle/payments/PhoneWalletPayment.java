package gocycle.payments;

import java.util.Objects;

/** Phone wallet payments, identified by the wallet's payment token. */
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
    public String describe() {
        return "Phone wallet";
    }

    public String getWalletToken() {
        return walletToken;
    }
}
