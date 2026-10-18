package gocycle.app;

import gocycle.common.SimulationClock;
import gocycle.controlroom.ControlRoomService;
import gocycle.faults.FaultType;
import gocycle.faults.MaintenanceCrew;
import gocycle.fleet.ElectricVehicle;
import gocycle.fleet.Vehicle;
import gocycle.stations.DockingStation;
import gocycle.trips.Trip;
import gocycle.trips.TripService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Moves the network forward in time, one simulated minute at a time: vehicles out on trips use
 * battery, docked ones charge, electric vehicles occasionally report a system fault on their own,
 * the maintenance crew finishes repairs, and the control room rebalances full and empty stations.
 */
public class FleetSimulator {
    static final double SYSTEM_FAULT_CHANCE_PER_MINUTE = 0.001;
    private static final int REBALANCE_EVERY_MINUTES = 30;

    private final SimulationClock clock;
    private final List<DockingStation> stations;
    private final TripService tripService;
    private final MaintenanceCrew crew;
    private final ControlRoomService controlRoom;
    private final Random random;
    private long secondsIntoMinute;
    private long minutesElapsed;

    public FleetSimulator(SimulationClock clock, List<DockingStation> stations, TripService tripService,
                          MaintenanceCrew crew, ControlRoomService controlRoom, Random random) {
        this.clock = clock;
        this.stations = stations;
        this.tripService = tripService;
        this.crew = crew;
        this.controlRoom = controlRoom;
        this.random = random;
    }

    public void advance(Duration duration) {
        long remaining = duration.toSeconds();
        while (remaining > 0) {
            long step = Math.min(remaining, 60 - secondsIntoMinute);
            clock.advance(Duration.ofSeconds(step));
            secondsIntoMinute += step;
            remaining -= step;
            if (secondsIntoMinute == 60) {
                secondsIntoMinute = 0;
                minuteElapsed();
            }
        }
    }

    private void minuteElapsed() {
        minutesElapsed++;
        for (Trip trip : tripService.getActiveTrips()) {
            trip.getVehicle().ride(1);
        }
        for (DockingStation station : stations) {
            for (Vehicle vehicle : station.getDockedVehicles()) {
                vehicle.rest(1);
                if (vehicle instanceof ElectricVehicle ev && ev.isRentable()
                        && random.nextDouble() < SYSTEM_FAULT_CHANCE_PER_MINUTE) {
                    ev.reportSelfFault(FaultType.SYSTEM_FAULT);
                }
            }
        }
        crew.completeDueRepairs();
        if (minutesElapsed % REBALANCE_EVERY_MINUTES == 0) {
            rebalance();
        }
    }

    /** Routine control-room move: one ready vehicle from a full station to an empty one. */
    private void rebalance() {
        Optional<DockingStation> empty = stations.stream()
                .filter(s -> s.getRentableVehicles().isEmpty() && s.hasFreePoint())
                .findFirst();
        Optional<DockingStation> full = stations.stream()
                .filter(s -> !s.hasFreePoint() && !s.getRentableVehicles().isEmpty())
                .findFirst();
        if (empty.isPresent() && full.isPresent()) {
            controlRoom.rebalance(full.get().getRentableVehicles().get(0), full.get(), empty.get());
        }
    }
}
