/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Root of the fleet model. TripService rents and returns vehicles, FaultService and the control room
 * change their status, and FleetSimulator advances them minute by minute.
 */
package gocycle.fleet;

import java.util.Objects;
import java.util.Optional;

/**
 * State shared by every vehicle: its id, its status and whether it can be rented.
 *
 * <p>{@link #checkRentable()} is the single rentability rule; subclasses extend it rather than adding
 * checks elsewhere. Not thread-safe: callers go through {@code GoCycleSystem.atomically(...)}.
 */
public abstract class Vehicle {
    private final String vehicleId;
    private VehicleStatus status = VehicleStatus.AVAILABLE;
    private boolean serviceDueOnReturn;

    protected Vehicle(String vehicleId) {
        this.vehicleId = Objects.requireNonNull(vehicleId);
    }

    public abstract VehicleType getType();

    /** Why this vehicle can't be rented right now; empty when it can. Subclasses add their own rules. */
    public Optional<RentalRefusal> checkRentable() {
        if (status != VehicleStatus.AVAILABLE) {
            return Optional.of(new RentalRefusal("error.vehicle.unavailable", vehicleId, status.getLabel()));
        }
        return Optional.empty();
    }

    public boolean isRentable() {
        return checkRentable().isEmpty();
    }

    /**
     * Marks the vehicle as out on a trip.
     *
     * @throws IllegalStateException if {@link #checkRentable()} refuses it; callers check first so they
     *         can show the reason
     */
    public void startUse() {
        if (!isRentable()) {
            throw new IllegalStateException(vehicleId + " cannot be rented");
        }
        status = VehicleStatus.IN_USE;
    }

    /**
     * Ends a trip. The vehicle becomes available again, or goes out of service if a fault was reported
     * while it was being ridden (see {@link #markOutOfService()}).
     *
     * @throws IllegalStateException if the vehicle is not in use
     */
    public void endUse() {
        if (status != VehicleStatus.IN_USE) {
            throw new IllegalStateException(vehicleId + " is not in use");
        }
        status = serviceDueOnReturn ? VehicleStatus.OUT_OF_SERVICE : VehicleStatus.AVAILABLE;
        serviceDueOnReturn = false;
    }

    /**
     * A vehicle that is being ridden can't be pulled mid-trip, so it is flagged instead and goes out
     * of service as soon as it is docked.
     */
    public void markOutOfService() {
        if (status == VehicleStatus.IN_USE) {
            serviceDueOnReturn = true;
        } else {
            status = VehicleStatus.OUT_OF_SERVICE;
        }
    }

    /**
     * Returns the vehicle to service after a repair or a control-room move. A vehicle on a trip keeps
     * riding; only its pending out-of-service flag is cleared.
     */
    public void markAvailable() {
        serviceDueOnReturn = false;
        if (status != VehicleStatus.IN_USE) {
            status = VehicleStatus.AVAILABLE;
        }
    }

    /**
     * Marks the vehicle as being moved between stations. The control room sets its final status
     * ({@link #markAvailable()} or {@link #markOutOfService()}) once it is docked at the destination.
     *
     * @throws IllegalStateException if the vehicle is out on a trip
     */
    public void markInTransit() {
        if (status == VehicleStatus.IN_USE) {
            throw new IllegalStateException(vehicleId + " is out on a trip");
        }
        status = VehicleStatus.IN_TRANSIT;
    }

    /**
     * Advances the vehicle by {@code minutes} simulated minutes out on a trip. FleetSimulator calls it
     * once per simulated minute. Does nothing for vehicles without a battery.
     */
    public void ride(int minutes) {
    }

    /**
     * Advances the vehicle by {@code minutes} simulated minutes in a docking point, whatever its status.
     * FleetSimulator calls it once per simulated minute. Does nothing for vehicles without a battery.
     */
    public void rest(int minutes) {
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    /** True when the vehicle is on a trip but has been reported broken. */
    public boolean isServiceDue() {
        return serviceDueOnReturn;
    }

    @Override
    public String toString() {
        return vehicleId;
    }
}
