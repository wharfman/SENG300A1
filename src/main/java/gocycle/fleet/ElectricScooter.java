package gocycle.fleet;

import gocycle.faults.FaultService;

/** Electric scooter. Uses its battery faster than an e-bike because there is no pedal assist. */
public class ElectricScooter extends ElectricVehicle {
    public ElectricScooter(String vehicleId, int chargePercent, FaultService faultService) {
        super(vehicleId, chargePercent, faultService);
    }

    @Override
    protected int drainPerMinute() {
        return 2;
    }

    @Override
    public VehicleType getType() {
        return VehicleType.E_SCOOTER;
    }
}
