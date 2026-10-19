/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Control-room staff member from the UML design; owns the operator dashboard and limits it to
 * their zones.
 */
package gocycle.controlroom;

import java.util.Objects;
import java.util.Set;

/**
 * Control-room staff member who watches the dashboard for the zones they oversee. Each operator owns one
 * dashboard; constructing an operator registers it as that dashboard's viewer.
 */
public class Operator {
    private final String operatorId;
    private final String name;
    private final Set<String> overseenZoneIds;
    private final OperatorDashboard dashboard;

    public Operator(String operatorId, String name, Set<String> overseenZoneIds, OperatorDashboard dashboard) {
        this.operatorId = Objects.requireNonNull(operatorId);
        this.name = Objects.requireNonNull(name);
        this.overseenZoneIds = Set.copyOf(overseenZoneIds);
        this.dashboard = Objects.requireNonNull(dashboard);
        // Passing this from the constructor is safe here: setViewer only stores the reference.
        dashboard.setViewer(this);
    }

    /** Prints the dashboard, limited to the zones this operator oversees. */
    public void watch() {
        dashboard.showNetwork();
    }

    public boolean oversees(String zoneId) {
        return overseenZoneIds.contains(zoneId);
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getName() {
        return name;
    }

    public Set<String> getOverseenZoneIds() {
        return overseenZoneIds;
    }

    public OperatorDashboard getDashboard() {
        return dashboard;
    }
}
