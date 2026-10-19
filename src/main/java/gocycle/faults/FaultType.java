/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Fault categories offered to riders in the app and raised by electric vehicles themselves.
 */
package gocycle.faults;

/**
 * Kind of fault, with the label shown to riders and operators. Only types that require a repair take
 * the vehicle out of service and become maintenance-crew jobs.
 */
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
