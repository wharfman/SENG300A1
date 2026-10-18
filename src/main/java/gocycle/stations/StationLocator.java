package gocycle.stations;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Finds the nearest station that still has a free docking point. */
public class StationLocator {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final List<DockingStation> stations;

    public StationLocator(List<DockingStation> stations) {
        this.stations = List.copyOf(stations);
    }

    public Optional<DockingStation> findNearestWithFreePoint(DockingStation from) {
        return stations.stream()
                .filter(s -> s != from && s.hasFreePoint())
                .min(Comparator.comparingDouble(s -> distanceKm(from, s)));
    }

    /** Great-circle distance between two stations. */
    public static double distanceKm(DockingStation a, DockingStation b) {
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude()))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
    }

    public List<DockingStation> getStations() {
        return stations;
    }
}
