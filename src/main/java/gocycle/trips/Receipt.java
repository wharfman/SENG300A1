/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Trip outcome produced by {@code TripService.confirmDocked}; delivered through the {@link ReceiptSender}
 * channels and printed by {@code RiderApp}.
 */
package gocycle.trips;

import gocycle.common.Message;
import gocycle.common.Money;
import gocycle.fleet.VehicleType;
import gocycle.riders.ReceiptDelivery;
import gocycle.stations.LocalizationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Summary sent to the rider once a trip is paid for. The trip values are a snapshot copied when the
 * receipt is created, and translated only when shown, so an old receipt follows the current language.
 * The only state added later is which delivery channels failed, recorded by {@link TripService} before
 * it hands the receipt back.
 */
public class Receipt {
    private static final DateTimeFormatter ISSUED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String tripId;
    private final String vehicleId;
    private final VehicleType vehicleType;
    private final String startStation;
    private final String endStation;
    private final long minutes;
    private final double amount;
    private final Message fare;
    private final Message payment;
    private final double depositReleased;
    private final LocalDateTime issuedAt;
    private final Set<ReceiptDelivery> undelivered = EnumSet.noneOf(ReceiptDelivery.class);

    /**
     * {@code trip} must be completed: it needs the end station, end time and final cost. A running trip
     * fails with a {@code NullPointerException} or {@code IllegalStateException}, depending on its status.
     */
    public Receipt(Trip trip, LocalDateTime issuedAt) {
        this.tripId = trip.getTripId();
        this.vehicleId = trip.getVehicle().getVehicleId();
        this.vehicleType = trip.getVehicle().getType();
        this.startStation = trip.getStartStation().getName();
        this.endStation = trip.getEndStation().getName();
        this.minutes = trip.durationMinutes();
        this.amount = trip.getCost();
        this.fare = trip.getPricing().describe();
        this.payment = trip.getPaymentMethod().describe();
        this.depositReleased = trip.getDeposit().getAmount();
        this.issuedAt = issuedAt;
    }

    /** Multi-line plain-text form, in the current language, shown on screen and used as the email body. */
    public String toText(LocalizationService l10n) {
        return String.join(System.lineSeparator(),
                l10n.text("receipt.title", tripId),
                l10n.text("receipt.vehicle", vehicleId, vehicleType),
                l10n.text("receipt.route", startStation, endStation),
                l10n.text("receipt.duration", minutes),
                l10n.text("receipt.fare", fare),
                l10n.text("receipt.charged", Money.format(amount), payment),
                l10n.text("receipt.deposit", Money.format(depositReleased)),
                l10n.text("receipt.issued", ISSUED.format(issuedAt)));
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

    /** Channels the rider asked for that could not deliver this receipt; empty when every copy went out. */
    public Set<ReceiptDelivery> getUndelivered() {
        return Collections.unmodifiableSet(undelivered);
    }

    void markUndelivered(ReceiptDelivery channel) {
        undelivered.add(channel);
    }
}
