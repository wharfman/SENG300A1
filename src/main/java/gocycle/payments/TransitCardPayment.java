package gocycle.payments;

import java.util.Objects;

/** City transit card payments. */
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
    public String describe() {
        return "Transit card **** " + lastFour(cardNumber);
    }
}
