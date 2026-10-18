package gocycle.stations;

import gocycle.fleet.Vehicle;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Holds at most one vehicle and reports whether it is really locked. */
public class DockingPoint {
    private final int pointNumber;
    private final BooleanSupplier lockSensor;
    private Vehicle vehicle;
    private boolean locked;

    /** {@code lockSensor} reports whether the latch caught when a vehicle is pushed in. */
    public DockingPoint(int pointNumber, BooleanSupplier lockSensor) {
        this.pointNumber = pointNumber;
        this.lockSensor = Objects.requireNonNull(lockSensor);
    }

    /** Returns true only if the vehicle is now locked in; otherwise the point stays free. */
    public boolean dock(Vehicle v) {
        if (!isFree() || !lockSensor.getAsBoolean()) {
            return false;
        }
        vehicle = v;
        locked = true;
        return true;
    }

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
