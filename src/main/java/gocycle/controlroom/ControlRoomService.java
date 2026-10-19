/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Control-room actions on the fleet, used by the fleet simulator for routine rebalancing and by
 * the maintenance crew to return repaired vehicles.
 */
package gocycle.controlroom;

import gocycle.common.SimulationClock;
import gocycle.faults.FleetEvent;
import gocycle.faults.FleetEventPublisher;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;
import gocycle.stations.DockingPoint;
import gocycle.stations.DockingStation;

import java.util.Objects;

/**
 * Moves vehicles between stations and takes them out of or back into service, publishing a fleet event
 * for each action. Not thread-safe; callers hold the system lock.
 */
public class ControlRoomService {
    private final SimulationClock clock;
    private final FleetEventPublisher publisher;

    public ControlRoomService(SimulationClock clock, FleetEventPublisher publisher) {
        this.clock = Objects.requireNonNull(clock);
        this.publisher = Objects.requireNonNull(publisher);
    }

    /**
     * Moves a docked vehicle to the first free point at another station, keeping it out of service if it
     * already was; otherwise it ends up available.
     *
     * @throws IllegalArgumentException if {@code from} and {@code to} are the same station
     * @throws IllegalStateException if {@code v} is not docked at {@code from}, {@code to} is full, or the
     *         new point will not lock (in that last case the vehicle has already left {@code from})
     */
    public void rebalance(Vehicle v, DockingStation from, DockingStation to) {
        if (from == to) {
            throw new IllegalArgumentException("Pick two different stations");
        }
        DockingPoint point = from.findPointHolding(v)
                .orElseThrow(() -> new IllegalStateException(v + " is not docked at " + from));
        if (!to.hasFreePoint()) {
            throw new IllegalStateException(to + " has no free point");
        }
        boolean wasOutOfService = v.getStatus() == VehicleStatus.OUT_OF_SERVICE;
        point.release();
        // IN_TRANSIT is only held while the vehicle is between points; the status is restored below. If
        // dockAnywhere throws, the vehicle is left IN_TRANSIT and docked nowhere.
        v.markInTransit();
        to.dockAnywhere(v);
        if (wasOutOfService) {
            v.markOutOfService();
        } else {
            v.markAvailable();
        }
        publisher.publish(new FleetEvent(FleetEvent.Kind.REBALANCED, v.getVehicleId(),
                v + " moved from " + from + " to " + to, clock.now()));
    }

    /**
     * A vehicle on a trip is only flagged and goes out of service when docked (see
     * {@link Vehicle#markOutOfService()}). Does not file a fault report, so no repair is scheduled.
     */
    public void takeOutOfService(Vehicle v) {
        v.markOutOfService();
        publisher.publish(new FleetEvent(FleetEvent.Kind.OUT_OF_SERVICE, v.getVehicleId(),
                v + " taken out of service by the control room", clock.now()));
    }

    public void returnToService(Vehicle v) {
        returnToService(v, "returned to service by the control room");
    }

    /**
     * Makes the vehicle available and clears any pending out-of-service flag; a vehicle on a trip stays in
     * use. Does not resolve open fault reports.
     *
     * @param reason completes the activity-feed line that starts with the vehicle id
     */
    public void returnToService(Vehicle v, String reason) {
        v.markAvailable();
        publisher.publish(new FleetEvent(FleetEvent.Kind.BACK_IN_SERVICE, v.getVehicleId(),
                v + " " + reason, clock.now()));
    }
}
