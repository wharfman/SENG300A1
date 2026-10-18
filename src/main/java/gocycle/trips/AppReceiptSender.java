package gocycle.trips;

import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;

/** Pushes the receipt to the rider's phone app inbox. */
public class AppReceiptSender implements ReceiptSender {
    @Override
    public void send(Receipt receipt, Rider to) {
        to.receiveInApp(receipt);
    }

    @Override
    public ReceiptDelivery channel() {
        return ReceiptDelivery.APP;
    }
}
