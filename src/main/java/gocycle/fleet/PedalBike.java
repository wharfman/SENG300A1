package gocycle.fleet;

/** Regular bike with no battery. */
public class PedalBike extends Vehicle {
    public PedalBike(String vehicleId) {
        super(vehicleId);
    }

    @Override
    public VehicleType getType() {
        return VehicleType.PEDAL_BIKE;
    }
}
