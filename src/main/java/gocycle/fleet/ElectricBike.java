package gocycle.fleet;

import gocycle.faults.FaultService;

/** Electric-assist bike. */
public class ElectricBike extends ElectricVehicle {
    public ElectricBike(String vehicleId, int chargePercent, FaultService faultService) {
        super(vehicleId, chargePercent, faultService);
    }

    @Override
    protected int drainPerMinute() {
        return 1;
    }

    @Override
    public VehicleType getType() {
        return VehicleType.E_BIKE;
    }
}
