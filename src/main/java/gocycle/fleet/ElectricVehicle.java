package gocycle.fleet;

import gocycle.faults.FaultService;
import gocycle.faults.FaultType;

import java.util.Objects;
import java.util.Optional;

/** A vehicle with a battery. It refuses rental below 20% and reports its own faults. */
public abstract class ElectricVehicle extends Vehicle {
    /** Docked vehicles gain 1% of charge every this many minutes. */
    public static final int MINUTES_PER_PERCENT_CHARGED = 5;

    private final Battery battery;
    private final FaultService faultService;
    private boolean lowBatteryReported;
    private int minutesCharging;

    protected ElectricVehicle(String vehicleId, int chargePercent, FaultService faultService) {
        super(vehicleId);
        this.battery = new Battery(chargePercent);
        this.faultService = Objects.requireNonNull(faultService);
    }

    /** Percent of charge used per minute of riding. */
    protected abstract int drainPerMinute();

    @Override
    public Optional<RentalRefusal> checkRentable() {
        Optional<RentalRefusal> refusal = super.checkRentable();
        if (refusal.isPresent() || battery.meetsRentThreshold()) {
            return refusal;
        }
        return Optional.of(new RentalRefusal("error.battery.low",
                getVehicleId(), battery.getChargePercent(), Battery.MIN_RENT_PERCENT));
    }

    @Override
    public void ride(int minutes) {
        battery.drain(minutes * drainPerMinute());
        if (!battery.meetsRentThreshold() && !lowBatteryReported) {
            lowBatteryReported = true;
            reportSelfFault(FaultType.LOW_BATTERY);
        }
    }

    @Override
    public void rest(int minutes) {
        minutesCharging += minutes;
        battery.charge(minutesCharging / MINUTES_PER_PERCENT_CHARGED);
        minutesCharging %= MINUTES_PER_PERCENT_CHARGED;
        if (battery.meetsRentThreshold()) {
            lowBatteryReported = false;
        }
    }

    public void reportSelfFault(FaultType type) {
        faultService.reportByVehicle(this, type);
    }

    public Battery getBattery() {
        return battery;
    }
}
