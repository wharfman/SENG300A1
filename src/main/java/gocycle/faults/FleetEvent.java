package gocycle.faults;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Message sent to subscribers when something changes in the fleet. */
public class FleetEvent {
    public enum Kind {
        TRIP_STARTED,
        TRIP_REDIRECTED,
        TRIP_ENDED,
        FAULT_REPORTED,
        OUT_OF_SERVICE,
        BACK_IN_SERVICE,
        REBALANCED
    }

    private final Kind kind;
    private final String vehicleId;
    private final String message;
    private final LocalDateTime occurredAt;
    private final FaultReport faultReport;

    public FleetEvent(Kind kind, String vehicleId, String message, LocalDateTime occurredAt) {
        this(kind, vehicleId, message, occurredAt, null);
    }

    public FleetEvent(Kind kind, String vehicleId, String message, LocalDateTime occurredAt, FaultReport faultReport) {
        this.kind = Objects.requireNonNull(kind);
        this.vehicleId = vehicleId;
        this.message = Objects.requireNonNull(message);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.faultReport = faultReport;
    }

    public Kind getKind() {
        return kind;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public Optional<FaultReport> getFaultReport() {
        return Optional.ofNullable(faultReport);
    }
}
