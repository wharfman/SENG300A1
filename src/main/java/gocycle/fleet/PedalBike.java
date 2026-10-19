/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One of the three concrete vehicle kinds in the fleet model, built by VehicleFactory for
 * VehicleType.PEDAL_BIKE.
 */
package gocycle.fleet;

/** Regular bike with no battery, so only the status rule in {@link Vehicle#checkRentable()} applies. */
public class PedalBike extends Vehicle {
    public PedalBike(String vehicleId) {
        super(vehicleId);
    }

    @Override
    public VehicleType getType() {
        return VehicleType.PEDAL_BIKE;
    }
}
