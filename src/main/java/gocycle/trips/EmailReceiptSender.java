package gocycle.trips;

import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stands in for an email service by writing each message to an outbox folder. */
public class EmailReceiptSender implements ReceiptSender {
    private final Path outbox;

    public EmailReceiptSender(Path outbox) {
        this.outbox = outbox;
    }

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

    public Path fileFor(Receipt receipt) {
        return outbox.resolve(receipt.getTripId() + ".txt");
    }

    @Override
    public ReceiptDelivery channel() {
        return ReceiptDelivery.EMAIL;
    }
}
