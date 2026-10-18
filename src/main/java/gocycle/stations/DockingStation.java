package gocycle.stations;

import gocycle.fleet.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** A station made of docking points plus a kiosk. */
public class DockingStation {
    private static final int STAFF_DOCK_ATTEMPTS = 10;

    private final String stationId;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final List<DockingPoint> points = new ArrayList<>();
    private final StationKiosk kiosk;
    private Zone zone;

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

    public List<Vehicle> getRentableVehicles() {
        return getDockedVehicles().stream().filter(Vehicle::isRentable).toList();
    }

    public Optional<DockingPoint> findPointHolding(Vehicle vehicle) {
        return points.stream().filter(p -> p.getVehicle() == vehicle).findFirst();
    }

    /** Used by staff (seeding, rebalancing): they re-seat the vehicle until the latch confirms. */
    public DockingPoint dockAnywhere(Vehicle vehicle) {
        DockingPoint point = getFreePoints().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(name + " has no free point"));
        for (int attempt = 0; attempt < STAFF_DOCK_ATTEMPTS; attempt++) {
            if (point.dock(vehicle)) {
                return point;
            }
        }
        throw new IllegalStateException("Point " + point.getPointNumber() + " at " + name + " will not lock");
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
