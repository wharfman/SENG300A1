package gocycle.faults;

public enum FaultType {
    FLAT_TIRE("Flat tire", true),
    BROKEN_BRAKES("Broken brakes", true),
    /** Needs a charge, not a repair: the 20% rental rule already blocks the vehicle until it recharges at a dock. */
    LOW_BATTERY("Low battery", false),
    SYSTEM_FAULT("System fault", true),
    OTHER("Other problem", true);

    private final String label;
    private final boolean requiresRepair;

    FaultType(String label, boolean requiresRepair) {
        this.label = label;
        this.requiresRepair = requiresRepair;
    }

    public String getLabel() {
        return label;
    }

    public boolean requiresRepair() {
        return requiresRepair;
    }
}
