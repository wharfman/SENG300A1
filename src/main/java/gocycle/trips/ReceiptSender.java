/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Extension point between {@code TripService} and the receipt delivery channels; {@code GoCycleSystem}
 * supplies the app and email implementations.
 */
package gocycle.trips;

import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;

/**
 * Delivers a receipt to the rider over one channel. {@code TripService} calls every sender whose
 * {@link #channel()} the rider's {@link ReceiptDelivery} setting includes.
 */
public interface ReceiptSender {
    void send(Receipt receipt, Rider to);

    /** The channel this sender covers; riders pick channels in their settings. */
    ReceiptDelivery channel();
}
