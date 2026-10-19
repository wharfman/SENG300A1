/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Battery-powered branch of the fleet model. Collaborates with FaultService to file its own fault
 * reports, and with FleetSimulator, which drains and charges it each simulated minute.
 */
package gocycle.fleet;

import gocycle.faults.FaultService;
import gocycle.faults.FaultType;

import java.util.Objects;
import java.util.Optional;

/**
 * A vehicle with a battery. It refuses rental below 20% and reports its own faults.
 *
 * <p>Template method: subclasses only supply {@link #drainPerMinute()}; draining, charging and the
 * low-battery report are handled here. A low-battery report is filed at most once per discharge: it is
 * re-armed only after the battery has charged back up to the rental threshold.
 */
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

    /** Adds the battery rule to the status rule; a status refusal takes precedence. */
    @Override
    public Optional<RentalRefusal> checkRentable() {
        Optional<RentalRefusal> refusal = super.checkRentable();
        if (refusal.isPresent() || battery.meetsRentThreshold()) {
            return refusal;
        }
        return Optional.of(new RentalRefusal("error.battery.low",
                getVehicleId(), battery.getChargePercent(), Battery.MIN_RENT_PERCENT));
    }

    /**
     * Drains the battery and, the first time it drops below the rental threshold, files a
     * {@link FaultType#LOW_BATTERY} report. That fault type does not take the vehicle out of service.
     */
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
        // Charging is 1% per MINUTES_PER_PERCENT_CHARGED minutes, so carry the leftover minutes over to
        // the next call instead of losing them to integer division.
        minutesCharging += minutes;
        battery.charge(minutesCharging / MINUTES_PER_PERCENT_CHARGED);
        minutesCharging %= MINUTES_PER_PERCENT_CHARGED;
        if (battery.meetsRentThreshold()) {
            lowBatteryReported = false;
        }
    }

    /**
     * Files a fault report with no rider attached. Fault types that need a repair take the vehicle out
     * of service (deferred to the end of the trip if it is being ridden).
     */
    public void reportSelfFault(FaultType type) {
        faultService.reportByVehicle(this, type);
    }

    public Battery getBattery() {
        return battery;
    }
}
