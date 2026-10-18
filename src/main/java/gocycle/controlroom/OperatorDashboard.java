package gocycle.controlroom;

import gocycle.common.Ansi;
import gocycle.common.Money;
import gocycle.faults.FleetEvent;
import gocycle.faults.FleetEventListener;
import gocycle.fleet.Battery;
import gocycle.fleet.VehicleStatus;
import gocycle.pricing.LongTripSurcharge;
import gocycle.snapshot.NetworkSnapshot;
import gocycle.snapshot.NetworkSnapshot.FaultView;
import gocycle.snapshot.NetworkSnapshot.StationView;
import gocycle.snapshot.NetworkSnapshot.Summary;
import gocycle.snapshot.NetworkSnapshot.TripView;
import gocycle.snapshot.NetworkSnapshot.VehicleView;
import gocycle.snapshot.NetworkSnapshot.ZoneView;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Live, read-only view of every station, vehicle, trip and fault. Subscribes to fleet events for
 * its activity feed; everything else comes from the latest snapshot.
 */
public class OperatorDashboard implements FleetEventListener {
    private static final int FEED_SIZE = 8;
    private static final long STALE_AFTER_SECONDS = 5;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_CLOCK = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd HH:mm");

    private final PrintStream out;
    private final Deque<FleetEvent> feed = new ArrayDeque<>();
    private NetworkSnapshot latest;
    private Operator viewer;

    public OperatorDashboard(PrintStream out) {
        this.out = out;
    }

    @Override
    public synchronized void onEvent(FleetEvent e) {
        feed.addFirst(e);
        while (feed.size() > FEED_SIZE) {
            feed.removeLast();
        }
    }

    public synchronized void update(NetworkSnapshot snapshot) {
        latest = snapshot;
    }

    public synchronized void clearFeed() {
        feed.clear();
    }

    void setViewer(Operator viewer) {
        this.viewer = viewer;
    }

    public synchronized void showNetwork() {
        List<String> lines = latest == null ? waiting() : render(latest);
        StringBuilder screen = new StringBuilder(Ansi.ENABLED ? "\u001b[H" : System.lineSeparator());
        for (String line : lines) {
            screen.append(line).append(Ansi.ENABLED ? "\u001b[K" : "").append(System.lineSeparator());
        }
        if (Ansi.ENABLED) {
            screen.append("\u001b[J");
        }
        out.print(screen);
        out.flush();
    }

    private List<String> waiting() {
        return List.of(
                Ansi.paint(" GoCycle Control Room ", Ansi.BOLD),
                "",
                "Waiting for the rider app to publish the network snapshot...",
                Ansi.paint("Start the rider app in another terminal.", Ansi.DIM));
    }

    private List<String> render(NetworkSnapshot s) {
        List<String> l = new ArrayList<>();
        Map<String, String> zoneNames = s.zones().stream().collect(Collectors.toMap(ZoneView::id, ZoneView::name));
        String overseen = s.zones().stream().filter(z -> watches(z.id())).map(ZoneView::name)
                .collect(Collectors.joining(", "));
        l.add(Ansi.paint(" GoCycle Control Room ", Ansi.BOLD, Ansi.background("#263238"), Ansi.foreground("#FFFFFF"))
                + "  " + (viewer == null ? "Operator" : "Operator " + viewer.getName()) + "  |  Zones: " + overseen);
        l.add("Network time " + DATE_CLOCK.format(LocalDateTime.parse(s.simTime())) + "  |  " + connection(s));
        l.add("");
        l.add(summary(s.summary()));

        l.add("");
        l.add(heading("STATIONS"));
        l.add(row(new String[]{"Station", "Zone", "Docked", "Free", "Ready", "Alert"}, 18, 12, 16, 6, 7));
        for (StationView st : s.stations()) {
            if (!watches(st.zoneId())) {
                continue;
            }
            int docked = st.capacity() - st.free();
            l.add(row(new String[]{st.name(), zoneNames.getOrDefault(st.zoneId(), st.zoneId()),
                    "[" + "#".repeat(docked) + ".".repeat(st.free()) + "] " + docked + "/" + st.capacity(),
                    String.valueOf(st.free()), String.valueOf(st.rentable()), stationAlert(st)}, 18, 12, 16, 6, 7));
        }

        l.add("");
        l.add(heading("VEHICLES"));
        l.add(row(new String[]{"Vehicle", "Type", "Status", "Battery", "Location"}, 9, 11, 25, 19));
        for (VehicleView v : s.vehicles()) {
            l.add(row(new String[]{v.id(), v.type(), status(v), battery(v.battery()), v.location()}, 9, 11, 25, 19));
        }

        l.add("");
        l.add(heading("ACTIVE TRIPS"));
        if (s.activeTrips().isEmpty()) {
            l.add(Ansi.paint("  No riders out right now.", Ansi.DIM));
        } else {
            l.add(row(new String[]{"Trip", "Rider", "Vehicle", "From", "Time", "Charged so far", "Deposit", "Payment", "Status"},
                    8, 28, 12, 16, 8, 16, 15, 24));
            for (TripView t : s.activeTrips()) {
                String vehicle = t.vehicleId() + (t.battery() == null ? "" : " " + t.battery() + "%");
                String cost = Money.format(t.cost())
                        + (t.minutes() > LongTripSurcharge.DEFAULT_THRESHOLD_MINUTES ? " (long)" : "");
                String status = "AWAITING_DOCK".equals(t.status())
                        ? Ansi.paint("Docking at " + t.to(), Ansi.YELLOW)
                        : Ansi.paint("Riding", Ansi.CYAN);
                l.add(row(new String[]{t.id(), t.rider() + " (" + t.riderKind() + ")", vehicle, t.from(),
                        t.minutes() + " min", Ansi.paint(cost, Ansi.BOLD), Money.format(t.deposit()) + " held",
                        t.payment(), status}, 8, 28, 12, 16, 8, 16, 15, 24));
            }
        }

        l.add("");
        l.add(heading("OPEN FAULTS - MAINTENANCE"));
        if (s.openFaults().isEmpty()) {
            l.add(Ansi.paint("  Nothing waiting for repair.", Ansi.DIM));
        } else {
            l.add(row(new String[]{"Report", "Vehicle", "Problem", "Reported by", "At", "Crew"}, 8, 9, 15, 20, 7));
            for (FaultView f : s.openFaults()) {
                String crew = f.repairDue() == null
                        ? Ansi.paint("waiting for the vehicle to be docked", Ansi.YELLOW)
                        : "repair due " + time(f.repairDue());
                l.add(row(new String[]{f.id(), f.vehicleId(), Ansi.paint(f.type(), Ansi.RED), f.reportedBy(),
                        time(f.reportedAt()), crew}, 8, 9, 15, 20, 7));
            }
        }

        l.add("");
        l.add(heading("LIVE ACTIVITY"));
        if (feed.isEmpty()) {
            l.add(Ansi.paint("  No activity yet.", Ansi.DIM));
        }
        for (FleetEvent e : feed) {
            l.add("  " + CLOCK.format(e.getOccurredAt()) + "  " + Ansi.pad(kind(e.getKind()), 16) + e.getMessage());
        }

        l.add("");
        l.add(heading("RECENTLY COMPLETED TRIPS"));
        if (s.recentTrips().isEmpty()) {
            l.add(Ansi.paint("  None yet.", Ansi.DIM));
        }
        for (TripView t : s.recentTrips()) {
            l.add(row(new String[]{t.id(), t.rider() + " (" + t.riderKind() + ")", t.vehicleId(),
                    t.from() + " -> " + t.to(), t.minutes() + " min", Money.format(t.cost()),
                    t.depositHeld() ? "deposit held" : "deposit released"}, 8, 28, 9, 30, 9, 10));
        }
        l.add("");
        l.add(Ansi.paint("Read-only view of the shared snapshot, refreshed every second. Ctrl+C to exit.", Ansi.DIM));
        return l;
    }

    private boolean watches(String zoneId) {
        return viewer == null || viewer.oversees(zoneId);
    }

    private static String connection(NetworkSnapshot s) {
        long age = Math.max(0, Duration.between(Instant.parse(s.writtenAt()), Instant.now()).toSeconds());
        if (!s.riderAppOnline()) {
            return Ansi.paint("RIDER APP CLOSED - showing last known state", Ansi.YELLOW, Ansi.BOLD);
        }
        if (age > STALE_AFTER_SECONDS) {
            return Ansi.paint("NO SIGNAL for " + age + "s", Ansi.RED, Ansi.BOLD);
        }
        return Ansi.paint("LIVE", Ansi.GREEN, Ansi.BOLD) + " (updated " + age + "s ago)";
    }

    private static String summary(Summary m) {
        return Ansi.paint("Vehicles " + m.vehicles(), Ansi.BOLD)
                + "   " + Ansi.paint("Ready " + m.ready(), Ansi.GREEN)
                + "   " + Ansi.paint("In use " + m.inUse(), Ansi.CYAN)
                + "   " + Ansi.paint("Out of service " + m.outOfService(), Ansi.RED)
                + "   " + Ansi.paint("Low battery " + m.lowBattery(), Ansi.YELLOW)
                + "   |   Active trips " + m.activeTrips()
                + "   Deposits held " + Money.format(m.depositsHeld())
                + "   Revenue " + Money.format(m.revenue())
                + "   Riders " + m.riders();
    }

    private static String stationAlert(StationView st) {
        if (st.free() == 0) {
            return Ansi.paint("FULL - riders are redirected, move vehicles out", Ansi.RED);
        }
        if (st.rentable() == 0) {
            return Ansi.paint("EMPTY - send vehicles here", Ansi.YELLOW);
        }
        if (st.rentable() == 1) {
            return Ansi.paint("Running low", Ansi.YELLOW);
        }
        return "";
    }

    private static String status(VehicleView v) {
        VehicleStatus status = VehicleStatus.valueOf(v.status());
        String colour = switch (status) {
            case AVAILABLE -> v.rentable() ? Ansi.GREEN : Ansi.YELLOW;
            case IN_USE -> Ansi.CYAN;
            case OUT_OF_SERVICE -> Ansi.RED;
            case IN_TRANSIT -> Ansi.YELLOW;
        };
        String label = status.getLabel();
        if (status == VehicleStatus.AVAILABLE && !v.rentable()) {
            label = "Charging (not rentable)";
        } else if (v.serviceDue()) {
            label += " - fault reported";
        }
        return Ansi.paint(label, colour);
    }

    private static String battery(Integer percent) {
        if (percent == null) {
            return Ansi.paint("-", Ansi.DIM);
        }
        int filled = (percent + 5) / 10;
        String bar = "[" + "#".repeat(filled) + ".".repeat(10 - filled) + "] " + String.format("%3d%%", percent);
        String colour = percent < Battery.MIN_RENT_PERCENT ? Ansi.RED : percent < 50 ? Ansi.YELLOW : Ansi.GREEN;
        return Ansi.paint(bar, colour);
    }

    private static String kind(FleetEvent.Kind kind) {
        Function<String, String> paint = switch (kind) {
            case FAULT_REPORTED, OUT_OF_SERVICE -> t -> Ansi.paint(t, Ansi.RED);
            case TRIP_REDIRECTED, REBALANCED -> t -> Ansi.paint(t, Ansi.YELLOW);
            case BACK_IN_SERVICE, TRIP_ENDED -> t -> Ansi.paint(t, Ansi.GREEN);
            case TRIP_STARTED -> t -> Ansi.paint(t, Ansi.CYAN);
        };
        return paint.apply(kind.name().replace('_', ' ').toLowerCase());
    }

    private static String time(String isoDateTime) {
        return CLOCK.format(LocalDateTime.parse(isoDateTime));
    }

    private static String heading(String title) {
        return Ansi.paint(title, Ansi.BOLD);
    }

    /** Lays out cells with the given widths; the last cell takes the rest of the line. */
    private static String row(String[] cells, int... widths) {
        StringBuilder b = new StringBuilder("  ");
        for (int i = 0; i < cells.length; i++) {
            String cell = cells[i] == null ? "" : cells[i];
            b.append(i < widths.length ? Ansi.pad(cell, widths[i]) + " " : cell);
        }
        return b.toString();
    }
}
