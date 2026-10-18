package gocycle.trips;

import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;

/** Delivers a receipt to the rider over one channel. */
public interface ReceiptSender {
    void send(Receipt receipt, Rider to);

    /** The channel this sender covers; riders pick channels in their settings. */
    ReceiptDelivery channel();
}
