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

/** One rental, from unlock to confirmed lock. Owns its deposit hold. */
public class Trip {
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

    public long durationMinutes() {
        if (endTime == null) {
            throw new IllegalStateException(tripId + " is still running");
        }
        return durationMinutes(endTime);
    }

    public double costSoFar(LocalDateTime now) {
        return status == Status.COMPLETED ? cost : pricing.calculate(durationMinutes(now));
    }

    void awaitDock(DockingStation station) {
        requireRunning();
        endStation = station;
        status = Status.AWAITING_DOCK;
    }

    void resumeRiding() {
        requireRunning();
        endStation = null;
        status = Status.ACTIVE;
    }

    /** Ends the trip once the vehicle is confirmed locked at {@code point}, and releases the deposit. */
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

    /** The station the rider is docking at; null while still riding. */
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

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Status getStatus() {
        return status;
    }

    public double getCost() {
        return cost;
    }
}
