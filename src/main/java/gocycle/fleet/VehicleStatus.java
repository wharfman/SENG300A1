/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Part of the fleet model: the state that Vehicle tracks and that rentals, faults, repairs and
 * control-room moves change.
 */
package gocycle.fleet;

/**
 * Where a vehicle is in its lifecycle. Only {@link #AVAILABLE} vehicles can be rented. Screens and kiosk
 * messages translate the constant through {@code LocalizationService} (key {@code VehicleStatus.<CONSTANT>}).
 */
public enum VehicleStatus {
    AVAILABLE,
    IN_USE,
    OUT_OF_SERVICE,
    /** Being moved between stations by the control room. */
    IN_TRANSIT
}
