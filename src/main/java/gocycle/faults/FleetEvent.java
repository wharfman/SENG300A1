/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Event object of the fleet observer design, published by the trip, fault and control-room
 * services and consumed by the maintenance crew and the rider app.
 */
package gocycle.faults;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable message sent to subscribers when something changes in the fleet. The {@code message} is a
 * human-readable English description of the event; only {@code FAULT_REPORTED} events
 * carry a {@link FaultReport}.
 */
public class FleetEvent {
    public enum Kind {
        TRIP_STARTED,
        TRIP_REDIRECTED,
        TRIP_ENDED,
        FAULT_REPORTED,
        OUT_OF_SERVICE,
        BACK_IN_SERVICE,
        REBALANCED,
        /** A control-room move was abandoned because the destination would not lock; the vehicle stayed put. */
        REBALANCE_FAILED,
        /** A copy of a receipt could not be delivered; the trip itself was completed and charged. */
        RECEIPT_FAILED
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

    /** Present only for {@code FAULT_REPORTED} events. */
    public Optional<FaultReport> getFaultReport() {
        return Optional.ofNullable(faultReport);
    }
}
