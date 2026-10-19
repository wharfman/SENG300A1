/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Part of the fleet model: the state that Vehicle tracks and that rentals, faults, repairs and
 * control-room moves change.
 */
package gocycle.fleet;

/**
 * Where a vehicle is in its lifecycle. Only {@link #AVAILABLE} vehicles can be rented. The label is
 * shown on screens and passed into kiosk messages untranslated.
 */
public enum VehicleStatus {
    AVAILABLE("Available"),
    IN_USE("In use"),
    OUT_OF_SERVICE("Out of service"),
    /** Being moved between stations by the control room. */
    IN_TRANSIT("In transit");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
