/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One dock slot inside a DockingStation. TripService releases vehicles from it when a trip starts and checks
 * it before ending a trip; RiderApp docks the rider's vehicle into it; staff dock through
 * DockingStation.dockAnywhere.
 */
package gocycle.stations;

import gocycle.fleet.Vehicle;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Holds at most one vehicle and reports whether it is really locked.
 *
 * <p>A dock attempt whose latch does not catch leaves the point free, so with the current code an occupied
 * point is always locked and {@link #isLocked()} equals {@code !isFree()}. {@code TripService.confirmDocked}
 * still checks the lock explicitly because that is the rule a trip may only end on. Not thread-safe: callers
 * go through {@code GoCycleSystem.atomically}.
 */
public class DockingPoint {
    private final int pointNumber;
    private final BooleanSupplier lockSensor;
    private Vehicle vehicle;
    private boolean locked;

    /**
     * @param pointNumber 1-based position within the station, shown to riders on the kiosk
     * @param lockSensor  reports whether the latch caught when a vehicle is pushed in; it is asked once per
     *                    dock attempt (in the app it fails at random, at rate {@code gocycle.lockFailureRate})
     */
    public DockingPoint(int pointNumber, BooleanSupplier lockSensor) {
        this.pointNumber = pointNumber;
        this.lockSensor = Objects.requireNonNull(lockSensor);
    }

    /**
     * Returns true only if the vehicle is now locked in; otherwise the point stays free. Returns false
     * without asking the sensor when the point is already occupied. Docking does not end a trip: the caller
     * must follow up with {@code TripService.confirmDocked}.
     */
    public boolean dock(Vehicle v) {
        if (!isFree() || !lockSensor.getAsBoolean()) {
            return false;
        }
        vehicle = v;
        locked = true;
        return true;
    }

    /**
     * Unlocks and removes the vehicle, leaving the point free. Callers (trip start, rebalancing) are expected
     * to have found this point via {@link DockingStation#findPointHolding}.
     *
     * @throws IllegalStateException if the point is empty
     */
    public Vehicle release() {
        if (vehicle == null) {
            throw new IllegalStateException("Point " + pointNumber + " is empty");
        }
        Vehicle released = vehicle;
        vehicle = null;
        locked = false;
        return released;
    }

    public boolean isFree() {
        return vehicle == null;
    }

    public boolean isLocked() {
        return locked;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public int getPointNumber() {
        return pointNumber;
    }
}
