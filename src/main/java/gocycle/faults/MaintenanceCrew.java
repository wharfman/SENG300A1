/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Maintenance side of fault handling: listens for fault events and is driven by the fleet
 * simulator's clock to finish repairs through the control room.
 */
package gocycle.faults;

import gocycle.common.SimulationClock;
import gocycle.controlroom.ControlRoomService;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Notified of every fault that needs a repair. Each repair takes {@link #REPAIR_TIME}; a vehicle
 * still out on a trip is fixed once it has been docked.
 *
 * <p>Repair time is counted from the report, not from docking, so a vehicle that comes back after its
 * repair was due is fixed on the next {@link #completeDueRepairs()} call. A job only completes while the
 * vehicle is {@code OUT_OF_SERVICE}; a vehicle with several open faults returns to service once the last
 * is fixed. Not thread-safe; the fleet simulator drives it under the system lock.
 */
public class MaintenanceCrew implements FleetEventListener {
    public static final Duration REPAIR_TIME = Duration.ofMinutes(45);

    private final String crewId;
    private final SimulationClock clock;
    private final ControlRoomService controlRoom;
    /** Open repair jobs and the simulated time each is due, in the order they were reported. */
    private final Map<FaultReport, LocalDateTime> jobs = new LinkedHashMap<>();

    public MaintenanceCrew(String crewId, SimulationClock clock, ControlRoomService controlRoom) {
        this.crewId = Objects.requireNonNull(crewId);
        this.clock = Objects.requireNonNull(clock);
        this.controlRoom = Objects.requireNonNull(controlRoom);
    }

    /** Queues a repair job for each fault report that needs one; every other event is ignored. */
    @Override
    public void onEvent(FleetEvent e) {
        e.getFaultReport()
                .filter(report -> report.getType().requiresRepair())
                .ifPresent(report -> jobs.put(report, e.getOccurredAt().plus(REPAIR_TIME)));
    }

    /**
     * Finishes every job that is due and puts the vehicle back in service once nothing else is wrong with it.
     * Jobs for a vehicle that is not out of service (still on a trip, or already returned to service by the
     * control room) stay queued.
     */
    public void completeDueRepairs() {
        LocalDateTime now = clock.now();
        Iterator<Map.Entry<FaultReport, LocalDateTime>> it = jobs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FaultReport, LocalDateTime> job = it.next();
            Vehicle vehicle = job.getKey().getVehicle();
            if (vehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE && !now.isBefore(job.getValue())) {
                job.getKey().resolve(now);
                it.remove();
                if (!hasJobFor(vehicle)) {
                    controlRoom.returnToService(vehicle, "repaired by " + crewId);
                }
            }
        }
    }

    /** When the repair for {@code report} is due; empty once it is done or if it never needed one. */
    public Optional<LocalDateTime> getRepairDue(FaultReport report) {
        return Optional.ofNullable(jobs.get(report));
    }

    private boolean hasJobFor(Vehicle vehicle) {
        return jobs.keySet().stream().anyMatch(r -> r.getVehicle() == vehicle);
    }

    public String getCrewId() {
        return crewId;
    }
}
