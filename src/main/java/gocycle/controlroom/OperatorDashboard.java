/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Control-room console view: subscribes to fleet events and reads stations, vehicles, trips and
 * faults directly from the services it is given.
 */
package gocycle.controlroom;

import gocycle.common.Ansi;
import gocycle.common.Money;
import gocycle.common.SimulationClock;
import gocycle.faults.FaultReport;
import gocycle.faults.FaultService;
import gocycle.faults.FleetEvent;
import gocycle.faults.FleetEventListener;
import gocycle.faults.MaintenanceCrew;
import gocycle.fleet.ElectricVehicle;
import gocycle.fleet.Vehicle;
import gocycle.riders.Rider;
import gocycle.stations.DockingPoint;
import gocycle.stations.DockingStation;
import gocycle.stations.Zone;
import gocycle.trips.Trip;
import gocycle.trips.TripService;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Live view of every station, vehicle, trip and fault for the control room. Subscribes to fleet
 * events for its activity feed (the last {@value #FEED_SIZE} events, newest first); everything else is
 * read from the network when it is shown. When an {@link Operator} is viewing, only stations in their
 * zones are listed. Not thread-safe: in the app it is notified and shown under the system lock.
 */
public class OperatorDashboard implements FleetEventListener {
    private static final int FEED_SIZE = 8;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final PrintStream out;
    private final SimulationClock clock;
    private final List<Zone> zones;
    private final List<Vehicle> vehicles;
    private final TripService tripService;
    private final FaultService faultService;
    private final MaintenanceCrew crew;
    private final Deque<FleetEvent> feed = new ArrayDeque<>();
    private Operator viewer;

    public OperatorDashboard(PrintStream out, SimulationClock clock, List<Zone> zones, List<Vehicle> vehicles,
                             TripService tripService, FaultService faultService, MaintenanceCrew crew) {
        this.out = Objects.requireNonNull(out);
        this.clock = Objects.requireNonNull(clock);
        this.zones = Objects.requireNonNull(zones);
        this.vehicles = Objects.requireNonNull(vehicles);
        this.tripService = Objects.requireNonNull(tripService);
        this.faultService = Objects.requireNonNull(faultService);
        this.crew = Objects.requireNonNull(crew);
    }

    @Override
    public void onEvent(FleetEvent e) {
        feed.addFirst(e);
        while (feed.size() > FEED_SIZE) {
            feed.removeLast();
        }
    }

    /** Package-private: only {@link Operator}'s constructor sets the viewer. */
    void setViewer(Operator viewer) {
        this.viewer = viewer;
    }

    /** Snapshot of the activity feed, newest first. */
    public List<FleetEvent> getRecentEvents() {
        return List.copyOf(feed);
    }

    /** Prints stations, vehicles, active trips, open faults and the activity feed to the output stream. */
    public void showNetwork() {
        LocalDateTime now = clock.now();
        out.println(Ansi.paint("GoCycle control room", Ansi.BOLD)
                + (viewer == null ? "" : " - " + viewer.getName()) + " - " + CLOCK.format(now));

        out.println(Ansi.paint("Stations", Ansi.BOLD));
        // Vehicle -> where it is, keyed by reference as stations compare vehicles with ==. A vehicle found
        // neither docked in a listed station nor on a trip is shown as "in transit", which includes
        // vehicles docked in zones this operator does not oversee.
        Map<Vehicle, String> locations = new IdentityHashMap<>();
        for (Zone zone : zones) {
            if (viewer != null && !viewer.oversees(zone.getZoneId())) {
                continue;
            }
            for (DockingStation station : zone.getStations()) {
                for (DockingPoint point : station.getPoints()) {
                    if (!point.isFree()) {
                        locations.put(point.getVehicle(), station.getName() + " #" + point.getPointNumber());
                    }
                }
                int capacity = station.getPoints().size();
                int free = station.getFreePoints().size();
                int ready = station.getRentableVehicles().size();
                out.printf("  %-16s %-11s %d/%d docked  %d ready  %s%n", station.getName(), zone.getName(),
                        capacity - free, capacity, ready, stationAlert(free, ready));
            }
        }

        List<Trip> active = tripService.getActiveTrips();
        active.forEach(t -> locations.put(t.getVehicle(), "on trip " + t.getTripId()));
        out.println(Ansi.paint("Vehicles", Ansi.BOLD));
        for (Vehicle v : vehicles) {
            String battery = v instanceof ElectricVehicle ev ? ev.getBattery().getChargePercent() + "%" : "-";
            out.printf("  %-7s %-11s %-15s %-5s %s%n", v.getVehicleId(), v.getType().getLabel(),
                    v.getStatus().getLabel(), battery, locations.getOrDefault(v, "in transit"));
        }

        out.println(Ansi.paint("Active trips", Ansi.BOLD));
        if (active.isEmpty()) {
            out.println("  none");
        }
        for (Trip t : active) {
            out.printf("  %s  %s  %s from %s  %d min  %s so far  %s deposit held%n", t.getTripId(), t.getRider(),
                    t.getVehicle(), t.getStartStation(), t.durationMinutes(now),
                    Money.format(t.costSoFar(now)), Money.format(t.getDeposit().getAmount()));
        }

        out.println(Ansi.paint("Open faults", Ansi.BOLD));
        List<FaultReport> open = faultService.getOpenReports();
        if (open.isEmpty()) {
            out.println("  none");
        }
        for (FaultReport f : open) {
            String due = crew.getRepairDue(f).map(t -> "repair due " + CLOCK.format(t)).orElse("");
            out.printf("  %s  %s  %s  by %s  %s%n", f.getReportId(), f.getVehicle(), f.getType().getLabel(),
                    f.getReportedBy().map(Rider::getName).orElse("the vehicle"), due);
        }

        out.println(Ansi.paint("Activity", Ansi.BOLD));
        for (FleetEvent e : feed) {
            out.println("  " + CLOCK.format(e.getOccurredAt()) + "  " + e.getMessage());
        }
    }

    /** Flags stations the control room should rebalance: no free points, or nothing ready to rent. */
    private static String stationAlert(int free, int ready) {
        if (free == 0) {
            return Ansi.paint("FULL - move vehicles out", Ansi.RED);
        }
        if (ready == 0) {
            return Ansi.paint("EMPTY - send vehicles here", Ansi.YELLOW);
        }
        return "";
    }
}
