/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One of the three concrete vehicle kinds in the fleet model, built by VehicleFactory for
 * VehicleType.E_BIKE.
 */
package gocycle.fleet;

import gocycle.faults.FaultService;

/** Electric-assist bike. Drains 1% of battery per minute of riding. */
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
