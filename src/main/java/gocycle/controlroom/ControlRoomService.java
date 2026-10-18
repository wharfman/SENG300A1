package gocycle.controlroom;

import gocycle.common.SimulationClock;
import gocycle.faults.FleetEvent;
import gocycle.faults.FleetEventPublisher;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;
import gocycle.stations.DockingPoint;
import gocycle.stations.DockingStation;

import java.util.Objects;

/** Moves vehicles between stations and takes them out of or back into service. */
public class ControlRoomService {
    private final SimulationClock clock;
    private final FleetEventPublisher publisher;

    public ControlRoomService(SimulationClock clock, FleetEventPublisher publisher) {
        this.clock = Objects.requireNonNull(clock);
        this.publisher = Objects.requireNonNull(publisher);
    }

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

    public void takeOutOfService(Vehicle v) {
        v.markOutOfService();
        publisher.publish(new FleetEvent(FleetEvent.Kind.OUT_OF_SERVICE, v.getVehicleId(),
                v + " taken out of service by the control room", clock.now()));
    }

    public void returnToService(Vehicle v) {
        returnToService(v, "returned to service by the control room");
    }

    public void returnToService(Vehicle v, String reason) {
        v.markAvailable();
        publisher.publish(new FleetEvent(FleetEvent.Kind.BACK_IN_SERVICE, v.getVehicleId(),
                v + " " + reason, clock.now()));
    }
}
