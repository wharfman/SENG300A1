/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Groups stations under one set of business rules. GoCycleSystem builds zones from config/zones.properties,
 * TripService checks the payment rule when a trip starts, and the operator dashboard reports per zone.
 */
package gocycle.stations;

import gocycle.payments.PaymentMethod;
import gocycle.payments.PaymentType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A neighbourhood with its own allowed payment methods and branding. The allowed payment types are fixed at
 * construction and never empty; stations are added during seeding.
 */
public class Zone {
    private final String zoneId;
    private final String name;
    private final Set<PaymentType> allowedPayments;
    private final ZoneBranding branding;
    private final List<DockingStation> stations = new ArrayList<>();

    /**
     * @param allowedPayments copied, so later changes to the caller's set have no effect
     * @throws IllegalArgumentException if {@code allowedPayments} is empty
     */
    public Zone(String zoneId, String name, Set<PaymentType> allowedPayments, ZoneBranding branding) {
        if (allowedPayments.isEmpty()) {
            throw new IllegalArgumentException("Zone " + zoneId + " must allow at least one payment method");
        }
        this.zoneId = Objects.requireNonNull(zoneId);
        this.name = Objects.requireNonNull(name);
        this.allowedPayments = Collections.unmodifiableSet(EnumSet.copyOf(allowedPayments));
        this.branding = Objects.requireNonNull(branding);
    }

    /** Decided by the method's {@link PaymentType} alone, not by the specific card or wallet. */
    public boolean allows(PaymentMethod method) {
        return allowedPayments.contains(method.getType());
    }

    /** Also sets the station's zone and applies this zone's branding to its kiosk. */
    public void addStation(DockingStation station) {
        stations.add(station);
        station.assignZone(this);
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
