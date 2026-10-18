package gocycle.payments;

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
    public String describe() {
        return "Credit card " + maskedNumber;
    }
}
