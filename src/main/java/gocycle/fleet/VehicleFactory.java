package gocycle.fleet;

import gocycle.faults.FaultService;

import java.util.Objects;

/** Creates the right Vehicle subclass for a VehicleType, keeping construction out of station and control-room code. */
public class VehicleFactory {
    private final FaultService faultService;

    public VehicleFactory(FaultService faultService) {
        this.faultService = Objects.requireNonNull(faultService);
    }

    public Vehicle create(VehicleType type, String id) {
        return create(type, id, 100);
    }

    /** {@code chargePercent} is ignored for pedal bikes. */
    public Vehicle create(VehicleType type, String id, int chargePercent) {
        return switch (type) {
            case PEDAL_BIKE -> new PedalBike(id);
            case E_BIKE -> new ElectricBike(id, chargePercent, faultService);
            case E_SCOOTER -> new ElectricScooter(id, chargePercent, faultService);
        };
    }
}
