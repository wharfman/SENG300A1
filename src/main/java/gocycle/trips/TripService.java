/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Application service for the trips package, built by {@code GoCycleSystem} and called by {@code RiderApp};
 * it coordinates riders, stations, vehicles and payments and publishes trip events to the control room.
 */
package gocycle.trips;

import gocycle.common.Money;
import gocycle.common.SimulationClock;
import gocycle.faults.FleetEvent;
import gocycle.faults.FleetEventPublisher;
import gocycle.fleet.Vehicle;
import gocycle.payments.PaymentMethod;
import gocycle.riders.Rider;
import gocycle.stations.DockingPoint;
import gocycle.stations.DockingStation;
import gocycle.stations.StationLocator;
import gocycle.stations.Zone;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Single entry point for the app and the kiosk: start a trip, request a return, confirm docking.
 *
 * <p>Rule violations a rider can cause are thrown as {@link TripException} with a kiosk message key;
 * calls made in the wrong order are programming errors and throw {@link IllegalStateException}. Not
 * thread-safe: callers go through {@code GoCycleSystem.atomically}, which also guards the clock thread.
 */
public class TripService {
    /** Deposit held on the payment method for the length of every trip, in dollars. */
    public static final double DEPOSIT_AMOUNT = 25.00;

    private final SimulationClock clock;
    private final StationLocator locator;
    private final List<ReceiptSender> receiptSenders;
    private final FleetEventPublisher publisher;
    private final List<Trip> trips = new ArrayList<>();
    private int nextTripNumber = 1001;

    /**
     * @param receiptSenders one per delivery channel; each receipt goes to those the rider's setting includes
     * @throws IllegalArgumentException if {@code receiptSenders} is empty
     */
    public TripService(SimulationClock clock, StationLocator locator, List<ReceiptSender> receiptSenders,
                       FleetEventPublisher publisher) {
        if (receiptSenders.isEmpty()) {
            throw new IllegalArgumentException("At least one receipt sender is required");
        }
        this.clock = Objects.requireNonNull(clock);
        this.locator = Objects.requireNonNull(locator);
        this.receiptSenders = List.copyOf(receiptSenders);
        this.publisher = Objects.requireNonNull(publisher);
    }

    /**
     * Unlocks {@code vehicle} from its point at {@code station} and starts billing. On success the deposit
     * is held on {@code method}, the vehicle is in use, the trip is recorded on the rider and a
     * {@code TRIP_STARTED} event is published. The rider's current pricing strategy is fixed for the trip.
     *
     * @throws TripException if the rider already has an unfinished trip, the zone doesn't accept
     *                       {@code method}, the vehicle isn't docked at {@code station}, or it isn't rentable
     */
    public Trip startTrip(Rider rider, DockingStation station, Vehicle vehicle, PaymentMethod method) {
        rider.getActiveTrip().ifPresent(t -> {
            throw new TripException("error.trip.active", t.getTripId());
        });
        Zone zone = station.getZone();
        if (!zone.allows(method)) {
            throw new TripException("error.payment.zone", method.getType(), zone.getName(), zone.getAllowedPayments());
        }
        DockingPoint point = station.findPointHolding(vehicle)
                .orElseThrow(() -> new TripException("error.vehicle.not.docked", vehicle.getVehicleId()));
        vehicle.checkRentable().ifPresent(refusal -> {
            throw new TripException(refusal.messageKey(), refusal.args());
        });

        // Every check is done above so a refused request leaves no hold, unlocked point or in-use vehicle.
        DepositHold deposit = new DepositHold(DEPOSIT_AMOUNT, method.placeHold(DEPOSIT_AMOUNT));
        point.release();
        vehicle.startUse();
        Trip trip = new Trip("T-" + nextTripNumber++, rider, vehicle, station, method, deposit,
                rider.getPricingStrategy(), clock.now());
        rider.addTrip(trip);
        trips.add(trip);
        publisher.publish(new FleetEvent(FleetEvent.Kind.TRIP_STARTED, vehicle.getVehicleId(),
                rider + " took " + vehicle + " from " + station + " (" + trip.getTripId() + ")", clock.now()));
        return trip;
    }

    /**
     * Returns the station the rider should dock at: the one they asked for if it has a free point,
     * otherwise the nearest one that does. The trip keeps running (and charging) either way until
     * the vehicle is locked.
     *
     * <p>If {@code station} has room the trip moves to {@code AWAITING_DOCK}. Otherwise it goes back to
     * {@code ACTIVE}, a {@code TRIP_REDIRECTED} event is published, and the rider is expected to call this
     * again at the suggested station.
     *
     * @throws IllegalStateException if the trip has already ended
     * @throws TripException         if no station in the network has a free point
     */
    public DockingStation requestReturn(Trip trip, DockingStation station) {
        if (!trip.isActive()) {
            throw new IllegalStateException(trip.getTripId() + " has already ended");
        }
        if (station.hasFreePoint()) {
            trip.awaitDock(station);
            return station;
        }
        trip.resumeRiding();
        DockingStation nearest = locator.findNearestWithFreePoint(station)
                .orElseThrow(() -> new TripException("error.network.full"));
        publisher.publish(new FleetEvent(FleetEvent.Kind.TRIP_REDIRECTED, trip.getVehicle().getVehicleId(),
                station + " is full: " + trip.getRider().getName() + " sent to " + nearest
                        + String.format(Locale.ROOT, " (%.1f km)", StationLocator.distanceKm(station, nearest)),
                clock.now()));
        return nearest;
    }

    /**
     * Ends the trip only if the vehicle is really locked at {@code point}; then charges and sends the receipt.
     * Completing the trip also releases the deposit and ends the vehicle's use (applying any fault reported
     * during the ride), and a {@code TRIP_ENDED} event is published. A receipt channel that fails does not
     * fail the call: see {@link Receipt#getUndelivered()}.
     *
     * @param point a point at the station accepted by {@link #requestReturn}, after the rider called
     *              {@code DockingPoint.dock} on it
     * @throws IllegalStateException if {@link #requestReturn} hasn't accepted a station for this trip
     * @throws TripException         if the vehicle isn't locked at {@code point} at that station, or the
     *                               charge is declined; the trip stays {@code AWAITING_DOCK} in both cases.
     *                               A declined charge also unlocks the vehicle from {@code point} again, so
     *                               it is back with the rider like any running trip
     */
    public Receipt confirmDocked(Trip trip, DockingPoint point) {
        if (trip.getStatus() != Trip.Status.AWAITING_DOCK) {
            throw new IllegalStateException("Call requestReturn before confirmDocked");
        }
        boolean atReturnStation = trip.getEndStation().getPoints().contains(point);
        if (!atReturnStation || point.getVehicle() != trip.getVehicle() || !point.isLocked()) {
            throw new TripException("error.not.locked", point.getPointNumber());
        }
        LocalDateTime end = clock.now();
        double cost = trip.getPricing().calculate(trip.durationMinutes(end));
        if (!trip.getPaymentMethod().charge(cost)) {
            // The trip keeps running, so the vehicle must not stay recorded in the point: otherwise the next
            // return attempt would lock the same vehicle into a second point.
            point.release();
            throw new TripException("error.payment.declined");
        }
        trip.complete(point, end, cost);
        trip.getVehicle().endUse();

        Receipt receipt = new Receipt(trip, end);
        Rider rider = trip.getRider();
        for (ReceiptSender sender : receiptSenders) {
            if (rider.getReceiptDelivery().includes(sender.channel())) {
                deliver(sender, receipt, trip);
            }
        }
        publisher.publish(new FleetEvent(FleetEvent.Kind.TRIP_ENDED, trip.getVehicle().getVehicleId(),
                rider.getName() + " docked " + trip.getVehicle() + " at " + trip.getEndStation() + ": "
                        + receipt.getMinutes() + " min, " + Money.format(cost),
                end));
        return receipt;
    }

    /**
     * Sends one copy of the receipt. The trip is already completed and charged, so a channel that can't
     * deliver (e.g. the email outbox can't be written) must neither undo that nor stop the other channels:
     * the failure is recorded on the receipt and reported to the control room instead.
     */
    private void deliver(ReceiptSender sender, Receipt receipt, Trip trip) {
        try {
            sender.send(receipt, trip.getRider());
        } catch (UncheckedIOException e) {
            receipt.markUndelivered(sender.channel());
            publisher.publish(new FleetEvent(FleetEvent.Kind.RECEIPT_FAILED, trip.getVehicle().getVehicleId(),
                    "Receipt " + receipt.getTripId() + " for " + trip.getRider().getName() + " not sent by "
                            + sender.channel() + ": " + e.getMessage(),
                    clock.now()));
        }
    }

    /** Trips not yet completed, including those awaiting a dock. */
    public List<Trip> getActiveTrips() {
        return trips.stream().filter(Trip::isActive).toList();
    }
}
