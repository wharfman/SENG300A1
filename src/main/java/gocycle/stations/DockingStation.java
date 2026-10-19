/*
 * GoCycle - SENG 300 Assignment 1
 *
 * A physical station in the network. GoCycleSystem seeds them, TripService starts and ends trips at them,
 * StationLocator ranks them by distance, and FleetSimulator and ControlRoomService move vehicles between them.
 */
package gocycle.stations;

import gocycle.fleet.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * A station made of docking points plus a kiosk. The set of points is fixed at construction; which vehicles
 * sit in them changes over time. The zone is set later, when {@link Zone#addStation} is called, and is null
 * until then. Not thread-safe: callers go through {@code GoCycleSystem.atomically}.
 */
public class DockingStation {
    // With the default 10% lock failure rate, ten tries all failing is about a one-in-ten-billion event.
    private static final int STAFF_DOCK_ATTEMPTS = 10;

    private final String stationId;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final List<DockingPoint> points = new ArrayList<>();
    private final StationKiosk kiosk;
    private Zone zone;

    /**
     * Creates {@code pointCount} empty points numbered from 1, all sharing {@code lockSensor}, and labels the
     * kiosk with the station name.
     *
     * @param latitude  degrees, used by {@link StationLocator#distanceKm}
     * @param longitude degrees
     * @throws IllegalArgumentException if {@code pointCount} is less than 1
     */
    public DockingStation(String stationId, String name, double latitude, double longitude,
                          int pointCount, StationKiosk kiosk, BooleanSupplier lockSensor) {
        if (pointCount < 1) {
            throw new IllegalArgumentException("A station needs at least one docking point");
        }
        this.stationId = Objects.requireNonNull(stationId);
        this.name = Objects.requireNonNull(name);
        this.latitude = latitude;
        this.longitude = longitude;
        this.kiosk = Objects.requireNonNull(kiosk);
        for (int i = 1; i <= pointCount; i++) {
            points.add(new DockingPoint(i, lockSensor));
        }
        kiosk.attachTo(name);
    }

    // Package-private so only Zone.addStation sets it, keeping both sides of the association in step.
    void assignZone(Zone zone) {
        this.zone = zone;
        kiosk.applyBranding(zone.getBranding());
    }

    public boolean hasFreePoint() {
        return points.stream().anyMatch(DockingPoint::isFree);
    }

    public List<DockingPoint> getFreePoints() {
        return points.stream().filter(DockingPoint::isFree).toList();
    }

    public List<Vehicle> getDockedVehicles() {
        return points.stream().filter(p -> !p.isFree()).map(DockingPoint::getVehicle).toList();
    }

    /** Docked vehicles that pass {@link Vehicle#isRentable()} right now (status, and battery for electrics). */
    public List<Vehicle> getRentableVehicles() {
        return getDockedVehicles().stream().filter(Vehicle::isRentable).toList();
    }

    /** Empty when the vehicle is not docked at this station. Matches by identity, not {@code equals}. */
    public Optional<DockingPoint> findPointHolding(Vehicle vehicle) {
        return points.stream().filter(p -> p.getVehicle() == vehicle).findFirst();
    }

    /**
     * Used by staff (seeding, rebalancing): they re-seat the vehicle until the latch confirms. Unlike a
     * rider's {@link DockingPoint#dock}, it retries the first free point up to {@value #STAFF_DOCK_ATTEMPTS}
     * times rather than reporting a single failed attempt.
     *
     * @return the point the vehicle is now locked into
     * @throws IllegalStateException if the station has no free point, or the latch never catches
     */
    public DockingPoint dockAnywhere(Vehicle vehicle) {
        return tryDockAnywhere(vehicle)
                .orElseThrow(() -> new IllegalStateException("No point at " + name + " will lock"));
    }

    /**
     * Same as {@link #dockAnywhere}, but a latch that never catches is an expected outcome rather than an
     * error: the result is empty and the station is unchanged.
     *
     * @throws IllegalStateException if the station has no free point
     */
    public Optional<DockingPoint> tryDockAnywhere(Vehicle vehicle) {
        DockingPoint point = getFreePoints().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(name + " has no free point"));
        for (int attempt = 0; attempt < STAFF_DOCK_ATTEMPTS; attempt++) {
            if (point.dock(vehicle)) {
                return Optional.of(point);
            }
        }
        return Optional.empty();
    }

    public String getStationId() {
        return stationId;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<DockingPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public StationKiosk getKiosk() {
        return kiosk;
    }

    public Zone getZone() {
        return zone;
    }

    @Override
    public String toString() {
        return name;
    }
}
