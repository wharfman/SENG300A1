/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Fault-handling record created by FaultService and tracked by the
 * maintenance crew and the operator dashboard until the repair is done.
 */
package gocycle.faults;

import gocycle.fleet.Vehicle;
import gocycle.riders.Rider;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * What is wrong, with which vehicle, and who reported it. Everything but the resolution time is fixed
 * at creation; a report is open until {@link #resolve} is called, which only the maintenance crew does.
 * Reports are compared by identity: the crew uses them as map keys.
 */
public class FaultReport {
    private final String reportId;
    private final FaultType type;
    private final Vehicle vehicle;
    private final Rider reportedBy;
    private final String description;
    private final LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;

    /** {@code reportedBy} is null when the vehicle reported the fault itself. */
    public FaultReport(String reportId, FaultType type, Vehicle vehicle, Rider reportedBy,
                       String description, LocalDateTime reportedAt) {
        this.reportId = Objects.requireNonNull(reportId);
        this.type = Objects.requireNonNull(type);
        this.vehicle = Objects.requireNonNull(vehicle);
        this.reportedBy = reportedBy;
        this.description = Objects.requireNonNull(description);
        this.reportedAt = Objects.requireNonNull(reportedAt);
    }

    /** Marks the repair as done at simulated time {@code at}. */
    public void resolve(LocalDateTime at) {
        resolvedAt = at;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    public String getReportId() {
        return reportId;
    }

    public FaultType getType() {
        return type;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    /** Empty when the vehicle reported the fault itself. */
    public Optional<Rider> getReportedBy() {
        return Optional.ofNullable(reportedBy);
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    /** Null while the report is still open. */
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
