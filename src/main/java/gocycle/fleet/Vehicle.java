package gocycle.fleet;

import java.util.Objects;
import java.util.Optional;

/** State shared by every vehicle: its id, its status and whether it can be rented. */
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

    public void startUse() {
        if (!isRentable()) {
            throw new IllegalStateException(vehicleId + " cannot be rented");
        }
        status = VehicleStatus.IN_USE;
    }

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

    public void markAvailable() {
        serviceDueOnReturn = false;
        if (status != VehicleStatus.IN_USE) {
            status = VehicleStatus.AVAILABLE;
        }
    }

    public void markInTransit() {
        if (status == VehicleStatus.IN_USE) {
            throw new IllegalStateException(vehicleId + " is out on a trip");
        }
        status = VehicleStatus.IN_TRANSIT;
    }

    /** Called for each simulated minute the vehicle spends out on a trip. */
    public void ride(int minutes) {
    }

    /** Called for each simulated minute the vehicle spends in a docking point. */
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
