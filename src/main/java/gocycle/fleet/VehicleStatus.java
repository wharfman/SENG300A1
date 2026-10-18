package gocycle.fleet;

public enum VehicleStatus {
    AVAILABLE("Available"),
    IN_USE("In use"),
    OUT_OF_SERVICE("Out of service"),
    IN_TRANSIT("In transit");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
