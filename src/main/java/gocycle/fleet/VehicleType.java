/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Part of the fleet model: VehicleFactory uses it to pick which Vehicle subclass to build.
 */
package gocycle.fleet;

/**
 * The kinds of vehicle in the fleet. Screens translate the constant through {@code LocalizationService} (key {@code VehicleType.<CONSTANT>}).
 */
public enum VehicleType {
    PEDAL_BIKE,
    E_BIKE,
    E_SCOOTER
}
