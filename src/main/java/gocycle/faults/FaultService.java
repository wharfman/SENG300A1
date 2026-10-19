/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Entry point for fault reports from the rider app and from electric vehicles; feeds the
 * maintenance crew and the control room through the fleet event publisher.
 */
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
 * hear about it. A vehicle that is out on a trip only goes out of service once it is docked (see
 * {@link Vehicle#markOutOfService()}). Not thread-safe: callers go through
 * {@code GoCycleSystem.atomically}, and listeners run synchronously inside each report call.
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

    /**
     * Files a rider's report and publishes a {@code FAULT_REPORTED} event.
     *
     * @param note the rider's own words; when null or blank the fault type's label is used instead
     */
    public FaultReport reportByRider(Rider rider, Vehicle vehicle, FaultType type, String note) {
        Objects.requireNonNull(rider, "rider");
        String description = note == null || note.isBlank() ? type.getLabel() : note.strip();
        return file(vehicle, type, rider, description);
    }

    /**
     * Files a report raised by the vehicle itself (no rider) and publishes a {@code FAULT_REPORTED} event.
     * The description records the battery level at the time of the report, whatever the fault type.
     */
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
        // Status is updated before publishing so listeners already see the vehicle out of service.
        String reporter = rider == null ? "the vehicle itself" : rider.getName();
        publisher.publish(new FleetEvent(FleetEvent.Kind.FAULT_REPORTED, vehicle.getVehicleId(),
                vehicle + ": " + type.getLabel() + " reported by " + reporter, report.getReportedAt(), report));
        return report;
    }

    /** Every report ever filed, oldest first, including resolved and low-battery ones. */
    public List<FaultReport> getReports() {
        return Collections.unmodifiableList(reports);
    }

    /** Unresolved reports that need a repair. Low-battery reports are informational and never listed. */
    public List<FaultReport> getOpenReports() {
        return reports.stream().filter(r -> !r.isResolved() && r.getType().requiresRepair()).toList();
    }
}
