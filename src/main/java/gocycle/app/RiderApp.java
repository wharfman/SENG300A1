package gocycle.app;

import gocycle.common.Ansi;
import gocycle.common.Money;
import gocycle.faults.FaultReport;
import gocycle.faults.FaultType;
import gocycle.faults.FleetEvent;
import gocycle.fleet.ElectricVehicle;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;
import gocycle.payments.CreditCardPayment;
import gocycle.payments.PaymentMethod;
import gocycle.payments.PaymentType;
import gocycle.payments.PhoneWalletPayment;
import gocycle.payments.TransitCardPayment;
import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;
import gocycle.stations.DockingPoint;
import gocycle.stations.DockingStation;
import gocycle.stations.StationKiosk;
import gocycle.stations.StationLocator;
import gocycle.stations.Zone;
import gocycle.trips.Receipt;
import gocycle.trips.Trip;
import gocycle.trips.TripException;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Rider terminal: the phone app and the station kiosks in one console. Every action goes through
 * {@link GoCycleSystem#atomically} so it never interleaves with the simulated clock.
 */
public final class RiderApp {
    private static final double RIDE_MINUTES_PER_KM = 4.0;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("EEE HH:mm");
    private static final List<String> MENU = List.of(
            "Sign up or switch rider",
            "Find stations and vehicles",
            "Start a trip",
            "Ride - let time pass",
            "Return my vehicle",
            "Report a problem with a vehicle",
            "My trips and receipts",
            "Language, read-aloud and receipt settings",
            "Exit");

    private final GoCycleSystem system;
    private final Console console;
    private final Queue<String> notices = new ConcurrentLinkedQueue<>();
    private volatile Rider rider;

    RiderApp(GoCycleSystem system, Console console) {
        this.system = system;
        this.console = console;
        this.rider = system.getRiders().get(0);
        system.getPublisher().subscribe(this::noticeForCurrentRider);
    }

    public static void main(String[] args) {
        new RiderApp(GoCycleSystem.create(), new Console(System.in, System.out)).run();
    }

    void run() {
        system.startClock();
        Runtime.getRuntime().addShutdownHook(new Thread(system::shutdown));
        console.say(Ansi.paint("Welcome to GoCycle.", Ansi.BOLD));
        try {
            while (true) {
                console.say(system.atomically(this::statusPanel));
                for (String notice; (notice = notices.poll()) != null; ) {
                    console.say(Ansi.paint("  ! " + notice, Ansi.YELLOW, Ansi.BOLD));
                }
                for (int i = 0; i < MENU.size(); i++) {
                    console.say("  " + (i + 1) + ") " + MENU.get(i));
                }
                int choice = console.readInt("Choose 1-9: ", 1, MENU.size());
                if (choice == 9) {
                    break;
                }
                handle(choice);
            }
        } catch (Console.InputClosedException e) {
            // input closed: exit like option 9
        }
        system.shutdown();
        console.say("Goodbye.");
    }

    private void handle(int choice) {
        switch (choice) {
            case 1 -> switchRider();
            case 2 -> showStations();
            case 3 -> startTrip();
            case 4 -> letTimePass();
            case 5 -> returnVehicle();
            case 6 -> reportProblem();
            case 7 -> showTrips();
            case 8 -> settings();
            default -> throw new IllegalArgumentException("No menu option " + choice);
        }
    }

    // ---------------------------------------------------------------- 1. riders

    private void switchRider() {
        List<Rider> riders = system.atomically(() -> List.copyOf(system.getRiders()));
        List<String> options = new ArrayList<>();
        for (Rider r : riders) {
            options.add(r + (r == rider ? Ansi.paint("  (signed in)", Ansi.DIM) : ""));
        }
        options.add("Sign up as a casual rider - pay per trip");
        options.add("Sign up as a member - " + Money.format(GoCycleSystem.MEMBER_MONTHLY_FEE) + "/month, lower per-minute rate");
        options.add("Sign up as a student - reduced rate");
        int choice = console.choose("Who is riding?", options);
        if (choice < 0) {
            return;
        }
        if (choice < riders.size()) {
            rider = riders.get(choice);
        } else {
            String name = console.readNonBlank("Full name: ");
            String email = console.readNonBlank("Email: ");
            int kind = choice - riders.size();
            String studentNumber = kind == 2 ? console.readNonBlank("Student number: ") : null;
            rider = system.atomically(() -> switch (kind) {
                case 0 -> system.signUpCasual(name, email);
                case 1 -> system.signUpMember(name, email);
                default -> system.signUpStudent(name, email, studentNumber);
            });
            console.say("Welcome, " + rider + ". Your fare: " + system.atomically(() -> rider.getPricingStrategy().describe()));
        }
    }

    // ---------------------------------------------------------------- 2. stations

    private void showStations() {
        console.say(system.atomically(() -> {
            StringBuilder b = new StringBuilder();
            for (Zone zone : system.getZones()) {
                b.append(System.lineSeparator())
                        .append(Ansi.paint(" " + zone.getName() + " zone ", Ansi.BOLD,
                                Ansi.background(zone.getBranding().getPrimaryColour()), Ansi.foreground("#FFFFFF")))
                        .append("  accepts ").append(zone.describeAllowedPayments()).append(System.lineSeparator());
                for (DockingStation station : zone.getStations()) {
                    b.append("  ").append(Ansi.paint(station.getName(), Ansi.BOLD))
                            .append(" - ").append(station.getRentableVehicles().size()).append(" ready, ")
                            .append(station.getFreePoints().size()).append(" free point(s)")
                            .append(System.lineSeparator());
                    for (DockingPoint point : station.getPoints()) {
                        b.append("     #").append(point.getPointNumber()).append("  ")
                                .append(point.isFree() ? Ansi.paint("free", Ansi.DIM) : vehicleLabel(point.getVehicle()))
                                .append(System.lineSeparator());
                    }
                }
            }
            return b.toString();
        }));
        console.pause();
    }

    // ---------------------------------------------------------------- 3. start

    private void startTrip() {
        Optional<Trip> active = system.atomically(() -> rider.getActiveTrip());
        if (active.isPresent()) {
            console.say("You are already on trip " + active.get().getTripId() + ". Return that vehicle first (option 5).");
            return;
        }
        DockingStation station = chooseStation("Which station are you at?");
        if (station == null) {
            return;
        }
        StationKiosk kiosk = station.getKiosk();
        List<Vehicle> vehicles = system.atomically(() -> {
            kiosk.serve(rider);
            kiosk.announce("kiosk.welcome", station.getName());
            return station.getDockedVehicles();
        });
        if (vehicles.isEmpty()) {
            console.say("No vehicles are docked here right now.");
            return;
        }
        List<String> labels = system.atomically(() -> vehicles.stream().map(this::vehicleLabel).toList());
        int pick = console.choose("Choose a vehicle", labels);
        if (pick < 0) {
            return;
        }
        Vehicle vehicle = vehicles.get(pick);
        while (true) {
            PaymentMethod method = choosePayment(station.getZone());
            if (method == null) {
                return;
            }
            try {
                system.atomically(() -> {
                    Optional<DockingPoint> point = station.findPointHolding(vehicle);
                    Trip trip = system.getTripService().startTrip(rider, station, vehicle, method);
                    kiosk.announce("kiosk.unlocked", vehicle.getVehicleId(),
                            point.map(DockingPoint::getPointNumber).orElseThrow(),
                            Money.format(trip.getDeposit().getAmount()));
                });
                return;
            } catch (TripException e) {
                system.atomically(() -> kiosk.announce(e.getMessageKey(), e.getArgs()));
                if (!"error.payment.zone".equals(e.getMessageKey())) {
                    return;
                }
            }
        }
    }

    private PaymentMethod choosePayment(Zone zone) {
        List<PaymentType> types = List.of(PaymentType.values());
        List<String> labels = types.stream()
                .map(t -> t.getLabel() + (zone.getAllowedPayments().contains(t)
                        ? "" : Ansi.paint("  (not accepted in " + zone.getName() + ")", Ansi.DIM)))
                .toList();
        int pick = console.choose("How would you like to pay?", labels);
        if (pick < 0) {
            return null;
        }
        return switch (types.get(pick)) {
            case CREDIT_CARD -> new CreditCardPayment(readCardNumber("Credit card number", "4111 1111 1111 1111"));
            case TRANSIT_CARD -> new TransitCardPayment(readCardNumber("Transit card number", "6012 3456 7890"));
            case PHONE_WALLET -> {
                console.say("Phone wallet paired.");
                yield new PhoneWalletPayment("wallet-" + rider.getRiderId());
            }
        };
    }

    private String readCardNumber(String label, String demoNumber) {
        while (true) {
            String number = console.readOrDefault(label + " (Enter for demo card " + demoNumber + "): ", demoNumber);
            if (number.replaceAll("\\D", "").length() >= 4) {
                return number;
            }
            console.say("Please enter at least 4 digits.");
        }
    }

    // ---------------------------------------------------------------- 4. time

    private void letTimePass() {
        console.say(Ansi.paint("Time also passes on its own: every real second is "
                + Integer.getInteger("gocycle.simSecondsPerTick", 15) + " network seconds.", Ansi.DIM));
        int minutes = console.readInt("How many minutes should pass? (1-240): ", 1, 240);
        system.atomically(() -> system.getSimulator().advance(Duration.ofMinutes(minutes)));
        console.say(minutes + " minute(s) passed.");
    }

    // ---------------------------------------------------------------- 5. return

    private void returnVehicle() {
        Optional<Trip> active = system.atomically(() -> rider.getActiveTrip());
        if (active.isEmpty()) {
            console.say("You have no active trip.");
            return;
        }
        Trip trip = active.get();
        DockingStation chosen = chooseStation("Which station are you returning to?");
        if (chosen == null) {
            return;
        }
        DockingStation station = chosen;
        while (true) {
            DockingStation at = station;
            DockingStation target;
            try {
                target = system.atomically(() -> {
                    at.getKiosk().serve(rider);
                    return system.getTripService().requestReturn(trip, at);
                });
            } catch (TripException e) {
                system.atomically(() -> at.getKiosk().announce(e.getMessageKey(), e.getArgs()));
                return;
            }
            if (target == at) {
                break;
            }
            double km = StationLocator.distanceKm(at, target);
            system.atomically(() -> at.getKiosk().announce("kiosk.station.full", target.getName(),
                    String.format(Locale.ROOT, "%.1f", km)));
            if (!console.confirm("Ride on to " + target.getName() + " now?")) {
                console.say("Your trip is still running and charging until the vehicle is locked.");
                return;
            }
            int minutes = (int) Math.max(1, Math.ceil(km * RIDE_MINUTES_PER_KM));
            console.say("Riding to " + target.getName() + " (" + minutes + " min)...");
            system.atomically(() -> system.getSimulator().advance(Duration.ofMinutes(minutes)));
            station = target;
        }
        dock(trip, station);
    }

    /** The rider pushes the vehicle into a free point until the lock is confirmed; only then does the trip end. */
    private void dock(Trip trip, DockingStation station) {
        StationKiosk kiosk = station.getKiosk();
        while (true) {
            Optional<DockingPoint> free = system.atomically(() -> station.getFreePoints().stream().findFirst());
            if (free.isEmpty()) {
                console.say("Someone just took the last free point. Choose option 5 again to be sent to the nearest free point.");
                return;
            }
            DockingPoint point = free.get();
            system.atomically(() -> kiosk.announce("kiosk.dock.instruction", trip.getVehicle().getVehicleId(), point.getPointNumber()));
            console.readLine("Press Enter to push the vehicle into point " + point.getPointNumber() + "...");
            boolean locked = system.atomically(() -> point.dock(trip.getVehicle()));
            if (!locked) {
                system.atomically(() -> kiosk.announce("error.lock.failed", point.getPointNumber()));
                continue;
            }
            try {
                Receipt receipt = system.atomically(() -> {
                    Receipt r = system.getTripService().confirmDocked(trip, point);
                    kiosk.announce("kiosk.trip.ended", r.getMinutes(), Money.format(r.getAmount()),
                            Money.format(r.getDepositReleased()));
                    return r;
                });
                console.say("");
                console.say(receipt.toText());
                console.say(Ansi.paint(deliveryNote(receipt), Ansi.DIM));
            } catch (TripException e) {
                system.atomically(() -> kiosk.announce(e.getMessageKey(), e.getArgs()));
            }
            return;
        }
    }

    private String deliveryNote(Receipt receipt) {
        ReceiptDelivery delivery = rider.getReceiptDelivery();
        List<String> notes = new ArrayList<>();
        if (delivery.includes(ReceiptDelivery.APP)) {
            notes.add("sent to your app inbox (option 7)");
        }
        if (delivery.includes(ReceiptDelivery.EMAIL)) {
            notes.add("emailed to " + rider.getEmail() + " (saved in " + system.getOutbox().resolve(receipt.getTripId() + ".txt") + ")");
        }
        return "Receipt " + String.join(" and ", notes) + ".";
    }

    // ---------------------------------------------------------------- 6. faults

    private void reportProblem() {
        Optional<Trip> active = system.atomically(() -> rider.getActiveTrip());
        Vehicle vehicle;
        StationKiosk kiosk = null;
        List<String> where = new ArrayList<>();
        active.ifPresent(t -> where.add("The vehicle I'm riding (" + t.getVehicle().getVehicleId() + ")"));
        where.add("A vehicle docked at a station");
        int pick = console.choose("Which vehicle has the problem?", where);
        if (pick < 0) {
            return;
        }
        if (active.isPresent() && pick == 0) {
            vehicle = active.get().getVehicle();
        } else {
            DockingStation station = chooseStation("Which station is it at?");
            if (station == null) {
                return;
            }
            List<Vehicle> docked = system.atomically(() -> station.getDockedVehicles());
            if (docked.isEmpty()) {
                console.say("No vehicles are docked there.");
                return;
            }
            int v = console.choose("Which vehicle?", system.atomically(() -> docked.stream().map(this::vehicleLabel).toList()));
            if (v < 0) {
                return;
            }
            vehicle = docked.get(v);
            kiosk = station.getKiosk();
        }
        List<FaultType> types = List.of(FaultType.values());
        int t = console.choose("What is wrong?", types.stream().map(FaultType::getLabel).toList());
        if (t < 0) {
            return;
        }
        FaultType type = types.get(t);
        String note = console.readLine("Anything else we should know? (optional): ");
        StationKiosk screen = kiosk;
        FaultReport report = system.atomically(() -> {
            FaultReport r = system.getFaultService().reportByRider(rider, vehicle, type, note);
            if (screen != null) {
                screen.serve(rider);
                screen.announce(type.requiresRepair() ? "kiosk.fault.thanks" : "kiosk.fault.noted",
                        vehicle.getVehicleId(), type.getLabel());
            }
            return r;
        });
        console.say("Report " + report.getReportId() + " filed. The control room can see it now.");
        if (screen == null && type.requiresRepair()) {
            console.say("Please return " + vehicle.getVehicleId() + " to a station; it goes out of service once docked.");
        }
    }

    // ---------------------------------------------------------------- 7. trips

    private void showTrips() {
        console.say(system.atomically(() -> {
            StringBuilder b = new StringBuilder(System.lineSeparator());
            b.append(Ansi.paint("Trips for " + rider, Ansi.BOLD)).append(System.lineSeparator());
            if (rider.getTrips().isEmpty()) {
                b.append("  No trips yet.").append(System.lineSeparator());
            }
            for (Trip trip : rider.getTrips()) {
                String to = trip.getEndStation() == null ? "..." : trip.getEndStation().getName();
                String state = trip.isActive()
                        ? Ansi.paint("in progress, " + Money.format(trip.costSoFar(system.getClock().now())) + " so far", Ansi.CYAN)
                        : Money.format(trip.getCost()) + " paid";
                b.append(String.format("  %s  %-7s %s -> %s  %d min  ", trip.getTripId(), trip.getVehicle().getVehicleId(),
                                trip.getStartStation().getName(), to, trip.durationMinutes(system.getClock().now())))
                        .append(state).append(System.lineSeparator());
            }
            List<Receipt> inbox = rider.getAppInbox();
            b.append(System.lineSeparator()).append(Ansi.paint("App inbox: " + inbox.size() + " receipt(s)", Ansi.BOLD))
                    .append(System.lineSeparator());
            if (!inbox.isEmpty()) {
                b.append(inbox.get(inbox.size() - 1).toText()).append(System.lineSeparator());
            }
            if (rider.getReceiptDelivery().includes(ReceiptDelivery.EMAIL)) {
                b.append("Emailed receipts are saved in ").append(system.getOutbox()).append(System.lineSeparator());
            }
            return b.toString();
        }));
        console.pause();
    }

    // ---------------------------------------------------------------- 8. settings

    private void settings() {
        while (true) {
            List<String> options = system.atomically(() -> List.of(
                    "Language: " + languageName(rider.getPreferredLanguage()),
                    "Read screens aloud: " + (rider.isAudioAssist() ? "on" : "off"),
                    "Receipts: " + rider.getReceiptDelivery().getLabel()));
            int pick = console.choose("Settings for " + rider.getName(), options);
            switch (pick) {
                case 0 -> {
                    List<Locale> locales = system.getLocalization().supportedLocales();
                    int l = console.choose("Screen language", locales.stream().map(RiderApp::languageName).toList());
                    if (l >= 0) {
                        system.atomically(() -> rider.setPreferredLanguage(locales.get(l)));
                    }
                }
                case 1 -> system.atomically(() -> rider.setAudioAssist(!rider.isAudioAssist()));
                case 2 -> {
                    List<ReceiptDelivery> channels = List.of(ReceiptDelivery.values());
                    int c = console.choose("Send receipts to", channels.stream().map(ReceiptDelivery::getLabel).toList());
                    if (c >= 0) {
                        system.atomically(() -> rider.setReceiptDelivery(channels.get(c)));
                    }
                }
                default -> {
                    return;
                }
            }
        }
    }

    // ---------------------------------------------------------------- shared

    private String statusPanel() {
        StringBuilder b = new StringBuilder(System.lineSeparator());
        b.append(Ansi.paint(" GoCycle ", Ansi.BOLD, Ansi.background("#1B5E20"), Ansi.foreground("#FFFFFF")))
                .append("  ").append(CLOCK.format(system.getClock().now()))
                .append("  |  ").append(Ansi.paint(rider.toString(), Ansi.BOLD))
                .append("  |  ").append(rider.getPricingStrategy().describe())
                .append(System.lineSeparator());
        Optional<Trip> active = rider.getActiveTrip();
        if (active.isEmpty()) {
            b.append("  No active trip.");
        } else {
            Trip trip = active.get();
            Vehicle v = trip.getVehicle();
            String where = trip.getStatus() == Trip.Status.AWAITING_DOCK ? "docking at " + trip.getEndStation() : "riding";
            b.append(Ansi.paint("  Trip " + trip.getTripId() + " (" + where + "): ", Ansi.CYAN))
                    .append(v.getType().getLabel()).append(" ").append(v.getVehicleId())
                    .append(v instanceof ElectricVehicle ev ? " " + ev.getBattery().getChargePercent() + "%" : "")
                    .append(" from ").append(trip.getStartStation())
                    .append(" | ").append(trip.durationMinutes(system.getClock().now())).append(" min | ")
                    .append(Ansi.paint(Money.format(trip.costSoFar(system.getClock().now())) + " so far", Ansi.BOLD))
                    .append(" | ").append(Money.format(trip.getDeposit().getAmount())).append(" deposit held");
        }
        return b.toString();
    }

    private DockingStation chooseStation(String title) {
        List<DockingStation> stations = system.getStations();
        List<String> labels = system.atomically(() -> stations.stream()
                .map(s -> s.getName() + " (" + s.getZone().getName() + ") - " + s.getRentableVehicles().size()
                        + " ready, " + s.getFreePoints().size() + " free point(s)")
                .toList());
        int pick = console.choose(title, labels);
        return pick < 0 ? null : stations.get(pick);
    }

    private String vehicleLabel(Vehicle v) {
        String battery = v instanceof ElectricVehicle ev ? "battery " + ev.getBattery().getChargePercent() + "%" : "";
        String state;
        if (v.isRentable()) {
            state = Ansi.paint("ready", Ansi.GREEN);
        } else if (v.getStatus() == VehicleStatus.AVAILABLE) {
            state = Ansi.paint("battery too low to rent", Ansi.YELLOW);
        } else {
            state = Ansi.paint(v.getStatus().getLabel().toLowerCase(), Ansi.RED);
        }
        return String.format("%-7s %-11s %-12s ", v.getVehicleId(), v.getType().getLabel(), battery) + state;
    }

    /** Tells the rider when the vehicle they are on reports a problem by itself, e.g. low battery. */
    private void noticeForCurrentRider(FleetEvent event) {
        Rider current = rider;
        event.getFaultReport()
                .filter(report -> report.getReportedBy().isEmpty())
                .ifPresent(report -> current.getActiveTrip()
                        .filter(trip -> trip.getVehicle() == report.getVehicle())
                        .ifPresent(trip -> notices.add("Your " + trip.getVehicle().getType().getLabel() + " "
                                + trip.getVehicle().getVehicleId() + " reported: " + report.getType().getLabel()
                                + " (" + report.getDescription() + ")")));
    }

    private static String languageName(Locale locale) {
        String name = locale.getDisplayLanguage(locale);
        return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
    }
}
