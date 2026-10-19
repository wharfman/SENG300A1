/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Root of the riders package. TripService records trips against it, receipt senders deliver to it, and
 * StationKiosk reads its language and audio-assist preferences.
 */
package gocycle.riders;

import gocycle.pricing.PricingStrategy;
import gocycle.trips.Receipt;
import gocycle.trips.Trip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * An account holder. Each rider kind supplies its own pricing strategy, so adding a new kind of
 * rider never touches trip or payment code.
 *
 * <p>Not thread-safe: callers go through {@code GoCycleSystem.atomically(...)}. The list getters
 * return read-only views, not copies.
 */
public abstract class Rider {
    private final String riderId;
    private final String name;
    private final String email;
    private Locale preferredLanguage = Locale.ENGLISH;
    private boolean audioAssist;
    private ReceiptDelivery receiptDelivery = ReceiptDelivery.APP;
    private final List<Trip> trips = new ArrayList<>();
    private final List<Receipt> appInbox = new ArrayList<>();

    protected Rider(String riderId, String name, String email) {
        this.riderId = Objects.requireNonNull(riderId, "riderId");
        this.name = Objects.requireNonNull(name, "name");
        this.email = Objects.requireNonNull(email, "email");
    }

    /**
     * The pricing for a trip started now, already wrapped in {@link gocycle.pricing.LongTripSurcharge}.
     * TripService stores the result in the trip at start, so a later change of rate (e.g. a membership
     * lapsing) does not affect a trip already under way.
     */
    public abstract PricingStrategy getPricingStrategy();

    /** Short label for screens, e.g. "Member". */
    public abstract String getKind();

    public List<Trip> getTrips() {
        return Collections.unmodifiableList(trips);
    }

    /** The trip that has not completed yet, including one waiting to be docked; empty if there is none. */
    public Optional<Trip> getActiveTrip() {
        return trips.stream().filter(Trip::isActive).findFirst();
    }

    /** @throws IllegalArgumentException if the trip was started by a different rider */
    public void addTrip(Trip trip) {
        if (trip.getRider() != this) {
            throw new IllegalArgumentException(trip.getTripId() + " belongs to another rider");
        }
        trips.add(trip);
    }

    /** Delivers a receipt to the rider's in-app inbox. */
    public void receiveInApp(Receipt receipt) {
        appInbox.add(receipt);
    }

    public List<Receipt> getAppInbox() {
        return Collections.unmodifiableList(appInbox);
    }

    public String getRiderId() {
        return riderId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Locale getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(Locale preferredLanguage) {
        this.preferredLanguage = Objects.requireNonNull(preferredLanguage);
    }

    /** Whether kiosk screens are also read aloud to this rider. */
    public boolean isAudioAssist() {
        return audioAssist;
    }

    public void setAudioAssist(boolean audioAssist) {
        this.audioAssist = audioAssist;
    }

    public ReceiptDelivery getReceiptDelivery() {
        return receiptDelivery;
    }

    public void setReceiptDelivery(ReceiptDelivery receiptDelivery) {
        this.receiptDelivery = Objects.requireNonNull(receiptDelivery);
    }

    @Override
    public String toString() {
        return name + " (" + getKind() + ")";
    }
}
