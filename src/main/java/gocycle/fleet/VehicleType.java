package gocycle.fleet;

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
