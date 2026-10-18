package gocycle.payments;

public enum PaymentType {
    CREDIT_CARD("Credit card"),
    TRANSIT_CARD("Transit card"),
    PHONE_WALLET("Phone wallet");

    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
