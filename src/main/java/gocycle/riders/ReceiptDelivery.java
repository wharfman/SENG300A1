package gocycle.riders;

/** Where a rider wants receipts delivered. */
public enum ReceiptDelivery {
    APP("Phone app"),
    EMAIL("Email"),
    BOTH("App and email");

    private final String label;

    ReceiptDelivery(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean includes(ReceiptDelivery channel) {
        return this == BOTH || this == channel;
    }
}
