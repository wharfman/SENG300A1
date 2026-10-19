/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Core entity of the trips package: created and driven through its lifecycle by {@link TripService}, and
 * read by the rider app and the fleet simulator (which drains batteries of
 * active trips).
 */
package gocycle.trips;

import gocycle.fleet.Vehicle;
import gocycle.payments.PaymentMethod;
import gocycle.pricing.PricingStrategy;
import gocycle.riders.Rider;
import gocycle.stations.DockingPoint;
import gocycle.stations.DockingStation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One rental, from unlock to confirmed lock. Owns its deposit hold.
 *
 * <p>State changes are package-private so only {@link TripService} can move a trip through
 * {@code ACTIVE -> AWAITING_DOCK -> COMPLETED}. Not thread-safe: callers go through
 * {@code GoCycleSystem.atomically}.
 */
public class Trip {
    /**
     * {@code TripStatus} in the UML diagram. {@code AWAITING_DOCK} means a return station has been
     * accepted but the vehicle isn't locked yet; the trip is still billed in both non-completed states.
     */
    public enum Status {
        ACTIVE,
        AWAITING_DOCK,
        COMPLETED
    }

    private final String tripId;
    private final Rider rider;
    private final Vehicle vehicle;
    private final DockingStation startStation;
    private final PaymentMethod paymentMethod;
    private final DepositHold deposit;
    private final PricingStrategy pricing;
    private final LocalDateTime startTime;
    private DockingStation endStation;
    private LocalDateTime endTime;
    private Status status = Status.ACTIVE;
    private double cost;

    /** {@code pricing} is fixed when the trip starts, so a membership lapsing mid-ride doesn't change the fare. */
    public Trip(String tripId, Rider rider, Vehicle vehicle, DockingStation startStation,
                PaymentMethod paymentMethod, DepositHold deposit, PricingStrategy pricing, LocalDateTime startTime) {
        this.tripId = Objects.requireNonNull(tripId);
        this.rider = Objects.requireNonNull(rider);
        this.vehicle = Objects.requireNonNull(vehicle);
        this.startStation = Objects.requireNonNull(startStation);
        this.paymentMethod = Objects.requireNonNull(paymentMethod);
        this.deposit = Objects.requireNonNull(deposit);
        this.pricing = Objects.requireNonNull(pricing);
        this.startTime = Objects.requireNonNull(startTime);
    }

    /** Billable minutes, rounded up and at least one. A running trip is measured up to {@code now}. */
    public long durationMinutes(LocalDateTime now) {
        LocalDateTime end = endTime != null ? endTime : now;
        long seconds = Math.max(0, Duration.between(startTime, end).toSeconds());
        return Math.max(1, (seconds + 59) / 60);
    }

    /**
     * Billable minutes of a finished trip.
     *
     * @throws IllegalStateException if the trip hasn't been completed
     */
    public long durationMinutes() {
        if (endTime == null) {
            throw new IllegalStateException(tripId + " is still running");
        }
        return durationMinutes(endTime);
    }

    /** The final charge once completed, otherwise what the rider would pay if the trip ended at {@code now}. */
    public double costSoFar(LocalDateTime now) {
        return status == Status.COMPLETED ? cost : pricing.calculate(durationMinutes(now));
    }

    /** Records the station the rider has been cleared to dock at. */
    void awaitDock(DockingStation station) {
        requireRunning();
        endStation = station;
        status = Status.AWAITING_DOCK;
    }

    /** Drops any pending return station, e.g. when the requested station turned out to be full. */
    void resumeRiding() {
        requireRunning();
        endStation = null;
        status = Status.ACTIVE;
    }

    /**
     * Ends the trip once the vehicle is confirmed locked at {@code point}, and releases the deposit (on the
     * payment method and in the {@link DepositHold}). {@code finalCost} is in dollars and has already
     * been charged by the caller.
     *
     * @throws IllegalStateException if no return is in progress or the vehicle isn't locked at {@code point}
     */
    void complete(DockingPoint point, LocalDateTime end, double finalCost) {
        if (status != Status.AWAITING_DOCK) {
            throw new IllegalStateException(tripId + " has no return in progress");
        }
        if (point.getVehicle() != vehicle || !point.isLocked()) {
            throw new IllegalStateException(vehicle + " is not locked at point " + point.getPointNumber());
        }
        endTime = end;
        cost = finalCost;
        status = Status.COMPLETED;
        paymentMethod.releaseHold(deposit.getHoldRef());
        deposit.release();
    }

    private void requireRunning() {
        if (status == Status.COMPLETED) {
            throw new IllegalStateException(tripId + " has already ended");
        }
    }

    /** True until the trip is completed, so it covers {@link Status#AWAITING_DOCK} as well as {@code ACTIVE}. */
    public boolean isActive() {
        return status != Status.COMPLETED;
    }

    public String getTripId() {
        return tripId;
    }

    public Rider getRider() {
        return rider;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public DockingStation getStartStation() {
        return startStation;
    }

    /** The station the rider is docking at (or docked at, once completed); null while still riding. */
    public DockingStation getEndStation() {
        return endStation;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public DepositHold getDeposit() {
        return deposit;
    }

    public PricingStrategy getPricing() {
        return pricing;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    /** Null until the trip is completed. */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Status getStatus() {
        return status;
    }

    /** Final charge in dollars; 0 until the trip is completed (use {@link #costSoFar} for a running trip). */
    public double getCost() {
        return cost;
    }
}
