package gocycle.trips;

import java.util.Objects;

/** Refundable hold placed when a trip starts and released when it ends. */
public class DepositHold {
    private final double amount;
    private final String holdRef;
    private boolean released;

    public DepositHold(double amount, String holdRef) {
        this.amount = amount;
        this.holdRef = Objects.requireNonNull(holdRef);
    }

    public void release() {
        if (released) {
            throw new IllegalStateException("Deposit " + holdRef + " was already released");
        }
        released = true;
    }

    public double getAmount() {
        return amount;
    }

    public String getHoldRef() {
        return holdRef;
    }

    public boolean isReleased() {
        return released;
    }
}
