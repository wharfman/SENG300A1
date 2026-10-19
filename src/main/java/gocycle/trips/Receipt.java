/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Trip outcome produced by {@code TripService.confirmDocked}; delivered through the {@link ReceiptSender}
 * channels and printed by {@code RiderApp}.
 */
package gocycle.trips;

import gocycle.common.Money;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Summary sent to the rider once a trip is paid for. An immutable snapshot: the values are copied from
 * the trip when the receipt is created.
 */
public class Receipt {
    private static final DateTimeFormatter ISSUED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String tripId;
    private final String vehicle;
    private final String route;
    private final long minutes;
    private final double amount;
    private final String fare;
    private final String payment;
    private final double depositReleased;
    private final LocalDateTime issuedAt;

    /**
     * {@code trip} must be completed: it needs the end station, end time and final cost. A running trip
     * fails with a {@code NullPointerException} or {@code IllegalStateException}, depending on its status.
     */
    public Receipt(Trip trip, LocalDateTime issuedAt) {
        this.tripId = trip.getTripId();
        this.vehicle = trip.getVehicle().getVehicleId() + " (" + trip.getVehicle().getType().getLabel() + ")";
        this.route = trip.getStartStation().getName() + " -> " + trip.getEndStation().getName();
        this.minutes = trip.durationMinutes();
        this.amount = trip.getCost();
        this.fare = trip.getPricing().describe();
        this.payment = trip.getPaymentMethod().describe();
        this.depositReleased = trip.getDeposit().getAmount();
        this.issuedAt = issuedAt;
    }

    /** Multi-line plain-text form shown on screen and used as the email body. */
    public String toText() {
        return String.join(System.lineSeparator(),
                "GoCycle receipt " + tripId,
                "  Vehicle   " + vehicle,
                "  Route     " + route,
                "  Duration  " + minutes + " min",
                "  Fare      " + fare,
                "  Charged   " + Money.format(amount) + " to " + payment,
                "  Deposit   " + Money.format(depositReleased) + " hold released",
                "  Issued    " + ISSUED.format(issuedAt));
    }

    public String getTripId() {
        return tripId;
    }

    /** Billable minutes, as charged. */
    public long getMinutes() {
        return minutes;
    }

    /** Amount charged, in dollars. */
    public double getAmount() {
        return amount;
    }

    /** Deposit returned to the rider, in dollars. */
    public double getDepositReleased() {
        return depositReleased;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
}
