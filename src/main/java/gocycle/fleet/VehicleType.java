/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Part of the fleet model: VehicleFactory uses it to pick which Vehicle subclass to build.
 */
package gocycle.fleet;

/** The kinds of vehicle in the fleet, each with a display label. */
public enum VehicleType {
    PEDAL_BIKE("Pedal bike"),
    E_BIKE("E-bike"),
    E_SCOOTER("E-scooter");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
