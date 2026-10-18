package gocycle.faults;

import gocycle.common.SimulationClock;
import gocycle.fleet.ElectricVehicle;
import gocycle.fleet.Vehicle;
import gocycle.riders.Rider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Accepts fault reports from riders and from electric vehicles. A fault that needs a repair takes
 * the vehicle out of service, and every report is published so maintenance and the control room
 * hear about it.
 */
public class FaultService {
    private final SimulationClock clock;
    private final FleetEventPublisher publisher;
    private final List<FaultReport> reports = new ArrayList<>();
    private int nextReportNumber = 1;

    public FaultService(SimulationClock clock, FleetEventPublisher publisher) {
        this.clock = Objects.requireNonNull(clock);
        this.publisher = Objects.requireNonNull(publisher);
    }

    public FaultReport reportByRider(Rider rider, Vehicle vehicle, FaultType type, String note) {
        Objects.requireNonNull(rider, "rider");
        String description = note == null || note.isBlank() ? type.getLabel() : note.strip();
        return file(vehicle, type, rider, description);
    }

    public FaultReport reportByVehicle(ElectricVehicle vehicle, FaultType type) {
        return file(vehicle, type, null, "Reported automatically at "
                + vehicle.getBattery().getChargePercent() + "% battery");
    }

    private FaultReport file(Vehicle vehicle, FaultType type, Rider rider, String description) {
        FaultReport report = new FaultReport("F-" + nextReportNumber++, type, vehicle, rider, description, clock.now());
        reports.add(report);
        if (type.requiresRepair()) {
            vehicle.markOutOfService();
        }
        String reporter = rider == null ? "the vehicle itself" : rider.getName();
        publisher.publish(new FleetEvent(FleetEvent.Kind.FAULT_REPORTED, vehicle.getVehicleId(),
                vehicle + ": " + type.getLabel() + " reported by " + reporter, report.getReportedAt(), report));
        return report;
    }

    public List<FaultReport> getReports() {
        return Collections.unmodifiableList(reports);
    }

    /** Unresolved reports that need a repair. Low-battery reports are informational and never listed. */
    public List<FaultReport> getOpenReports() {
        return reports.stream().filter(r -> !r.isResolved() && r.getType().requiresRepair()).toList();
    }
}
