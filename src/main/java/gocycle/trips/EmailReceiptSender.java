/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Trip side of receipt delivery: the email channel that {@code GoCycleSystem} hands to {@code TripService},
 * writing into the {@code outbox} folder under {@code gocycle.dataDir}.
 */
package gocycle.trips;

import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stands in for an email service by writing each message to an outbox folder, one file per trip
 * named {@code <tripId>.txt}.
 */
public class EmailReceiptSender implements ReceiptSender {
    private final Path outbox;

    /** {@code outbox} is created on first send if it doesn't exist yet. */
    public EmailReceiptSender(Path outbox) {
        this.outbox = outbox;
    }

    /**
     * Writes the email (headers plus {@link Receipt#toText()}) to {@link #fileFor}, replacing any
     * earlier file for the same trip.
     *
     * @throws UncheckedIOException if the outbox or file can't be written
     */
    @Override
    public void send(Receipt receipt, Rider to) {
        String message = "To: " + to.getEmail() + System.lineSeparator()
                + "Subject: Your GoCycle receipt for " + receipt.getTripId() + System.lineSeparator()
                + System.lineSeparator()
                + receipt.toText() + System.lineSeparator();
        try {
            Files.createDirectories(outbox);
            Files.writeString(fileFor(receipt), message);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not email receipt " + receipt.getTripId(), e);
        }
    }

    /** Where the email for {@code receipt} is (or will be) saved. */
    public Path fileFor(Receipt receipt) {
        return outbox.resolve(receipt.getTripId() + ".txt");
    }

    @Override
    public ReceiptDelivery channel() {
        return ReceiptDelivery.EMAIL;
    }
}
