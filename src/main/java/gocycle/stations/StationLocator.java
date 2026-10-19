/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Distance and redirect logic for full stations. TripService uses it in requestReturn to send a rider on to
 * another station; RiderApp uses the distance to show the rider how far that is.
 */
package gocycle.stations;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds the nearest station that still has a free docking point. Distances are straight-line (great-circle)
 * kilometres, not riding distance. The station list is fixed at construction; free points are checked live
 * on each call.
 */
public class StationLocator {
    // Mean Earth radius used by the haversine formula below.
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final List<DockingStation> stations;

    public StationLocator(List<DockingStation> stations) {
        this.stations = List.copyOf(stations);
    }

    /**
     * Never returns {@code from} itself, even if it has a free point. Empty when no other station has a free
     * point; {@code TripService} turns that into the {@code error.network.full} message.
     */
    public Optional<DockingStation> findNearestWithFreePoint(DockingStation from) {
        return stations.stream()
                .filter(s -> s != from && s.hasFreePoint())
                .min(Comparator.comparingDouble(s -> distanceKm(from, s)));
    }

    /** Great-circle distance between two stations in km, by the haversine formula. */
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
