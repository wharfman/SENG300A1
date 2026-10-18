package gocycle.stations;

import gocycle.payments.PaymentMethod;
import gocycle.payments.PaymentType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** A neighbourhood with its own allowed payment methods and branding. */
public class Zone {
    private final String zoneId;
    private final String name;
    private final Set<PaymentType> allowedPayments;
    private final ZoneBranding branding;
    private final List<DockingStation> stations = new ArrayList<>();

    public Zone(String zoneId, String name, Set<PaymentType> allowedPayments, ZoneBranding branding) {
        if (allowedPayments.isEmpty()) {
            throw new IllegalArgumentException("Zone " + zoneId + " must allow at least one payment method");
        }
        this.zoneId = Objects.requireNonNull(zoneId);
        this.name = Objects.requireNonNull(name);
        this.allowedPayments = Collections.unmodifiableSet(EnumSet.copyOf(allowedPayments));
        this.branding = Objects.requireNonNull(branding);
    }

    public boolean allows(PaymentMethod method) {
        return allowedPayments.contains(method.getType());
    }

    public void addStation(DockingStation station) {
        stations.add(station);
        station.assignZone(this);
    }

    public String describeAllowedPayments() {
        return allowedPayments.stream().map(PaymentType::getLabel).collect(Collectors.joining(", "));
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
    }

    public Set<PaymentType> getAllowedPayments() {
        return allowedPayments;
    }

    public ZoneBranding getBranding() {
        return branding;
    }

    public List<DockingStation> getStations() {
        return Collections.unmodifiableList(stations);
    }
}
