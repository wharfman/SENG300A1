/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Application layer: the program's entry point and 1-9 menu. It drives the domain only through GoCycleSystem
 * and reads the keyboard through Console; station kiosks print their own localized messages.
 */
package gocycle.app;

import gocycle.common.Ansi;
import gocycle.common.Money;
import gocycle.faults.FaultReport;
import gocycle.faults.FaultType;
import gocycle.faults.FleetEvent;
import gocycle.fleet.ElectricVehicle;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;
import gocycle.fleet.VehicleType;
import gocycle.payments.CreditCardPayment;
import gocycle.payments.PaymentMethod;
import gocycle.payments.PaymentType;
import gocycle.payments.PhoneWalletPayment;
import gocycle.payments.TransitCardPayment;
import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;
import gocycle.stations.DockingPoint;
import gocycle.stations.DockingStation;
import gocycle.stations.LocalizationService;
import gocycle.stations.StationKiosk;
import gocycle.stations.StationLocator;
import gocycle.stations.Zone;
import gocycle.trips.Receipt;
import gocycle.trips.Trip;
import gocycle.trips.TripException;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Rider terminal: the phone app and the station kiosks in one console. Every action goes through
 * {@link GoCycleSystem#atomically} so it never interleaves with the simulated clock.
 *
 * <p>One rider is signed in at a time. Keyboard input is always read outside the lock, so the clock keeps
 * running while the menu waits; each step therefore re-reads state, and the domain services re-check
 * their rules, because the network may have changed between prompts.
 *
 * <p>All text comes from {@link LocalizationService} in the system-wide language, which option 8 changes for
 * everyone. Each screen is rebuilt from message keys every time it is shown, so a change applies at once.
 */
public final class RiderApp {
    // Simulated time charged for riding on to a redirect station: 4 min/km is about 15 km/h.
    private static final double RIDE_MINUTES_PER_KM = 4.0;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("EEE HH:mm");
    private static final int MENU_SIZE = 9;

    private final GoCycleSystem system;
    private final Console console;
    private final LocalizationService l10n;
    // Filled by noticeForCurrentRider, which can run on the clock thread; drained by the menu thread.
    private final Queue<String> notices = new ConcurrentLinkedQueue<>();
    // Written only by the menu thread, but also read by noticeForCurrentRider on the clock thread.
    private volatile Rider rider;

    RiderApp(GoCycleSystem system, Console console) {
        this.system = system;
        this.console = console;
        this.l10n = system.getLocalization();
        this.rider = system.getRiders().get(0);
        system.getPublisher().subscribe(this::noticeForCurrentRider);
    }

    public static void main(String[] args) {
        GoCycleSystem system = GoCycleSystem.create();
        new RiderApp(system, new Console(System.in, System.out, system.getLocalization())).run();
    }

    /** Starts the clock and loops over the menu until the rider picks Exit or input closes. */
    void run() {
        system.startClock();
        Runtime.getRuntime().addShutdownHook(new Thread(system::shutdown));
        console.say(Ansi.paint(t("app.welcome"), Ansi.BOLD));
        try {
            while (true) {
                console.say(system.atomically(this::statusPanel));
                for (String notice; (notice = notices.poll()) != null; ) {
                    console.say(Ansi.paint("  ! " + notice, Ansi.YELLOW, Ansi.BOLD));
                }
                for (int i = 1; i <= MENU_SIZE; i++) {
                    console.say("  " + i + ") " + t("menu." + i));
                }
                int choice = console.readInt(t("app.choose"), 1, MENU_SIZE);
                if (choice == 9) {
                    break;
                }
                handle(choice);
            }
        } catch (Console.InputClosedException e) {
            // input closed: exit like option 9
        }
        system.shutdown();
        console.say(t("app.goodbye"));
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
            options.add(system.atomically(() -> riderLabel(r))
                    + (r == rider ? Ansi.paint("  " + t("rider.signed.in"), Ansi.DIM) : ""));
        }
        options.add(t("rider.signup.casual"));
        options.add(t("rider.signup.member", Money.format(GoCycleSystem.MEMBER_MONTHLY_FEE)));
        options.add(t("rider.signup.student"));
        int choice = console.choose(t("rider.who"), options);
        if (choice < 0) {
            return;
        }
        if (choice < riders.size()) {
            rider = riders.get(choice);
        } else {
            String name = console.readNonBlank(t("rider.prompt.name"));
            String email = console.readNonBlank(t("rider.prompt.email"));
            int kind = choice - riders.size();
            String studentNumber = kind == 2 ? console.readNonBlank(t("rider.prompt.student.number")) : null;
            rider = system.atomically(() -> switch (kind) {
                case 0 -> system.signUpCasual(name, email);
                case 1 -> system.signUpMember(name, email);
                default -> system.signUpStudent(name, email, studentNumber);
            });
            console.say(system.atomically(() -> t("rider.welcome", riderLabel(rider), rider.getPricingStrategy().describe())));
        }
    }

    // ---------------------------------------------------------------- 2. stations

    private void showStations() {
        console.say(system.atomically(() -> {
            StringBuilder b = new StringBuilder();
            for (Zone zone : system.getZones()) {
                b.append(System.lineSeparator())
                        .append(Ansi.paint(t("stations.zone", zone.getName()), Ansi.BOLD,
                                Ansi.background(zone.getBranding().getPrimaryColour()), Ansi.foreground("#FFFFFF")))
                        .append(t("stations.accepts", zone.getAllowedPayments())).append(System.lineSeparator());
                for (DockingStation station : zone.getStations()) {
                    b.append("  ").append(Ansi.paint(station.getName(), Ansi.BOLD))
                            .append(" - ").append(stationCounts(station))
                            .append(System.lineSeparator());
                    for (DockingPoint point : station.getPoints()) {
                        b.append("     #").append(point.getPointNumber()).append("  ")
                                .append(point.isFree() ? Ansi.paint(t("stations.point.free"), Ansi.DIM) : vehicleLabel(point.getVehicle()))
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
            console.say(t("start.already.riding", active.get().getTripId()));
            return;
        }
        DockingStation station = chooseStation(t("stations.ask.start"));
        if (station == null) {
            return;
        }
        StationKiosk kiosk = station.getKiosk();
        List<Vehicle> vehicles = system.atomically(() -> {
            kiosk.announce("kiosk.welcome", station.getName());
            return station.getDockedVehicles();
        });
        if (vehicles.isEmpty()) {
            console.say(t("start.no.vehicles"));
            return;
        }
        List<String> labels = system.atomically(() -> vehicles.stream().map(this::vehicleLabel).toList());
        int pick = console.choose(t("start.choose.vehicle"), labels);
        if (pick < 0) {
            return;
        }
        Vehicle vehicle = vehicles.get(pick);
        // Only a payment type the zone refuses lets the rider try again; any other refusal ends the attempt.
        while (true) {
            PaymentMethod method = choosePayment(station.getZone());
            if (method == null) {
                return;
            }
            try {
                system.atomically(() -> {
                    // Look up the point first: startTrip releases it, after which the vehicle is in no point.
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

    /**
     * Lists every payment type, marking those the zone refuses rather than hiding them, so that picking one
     * shows the kiosk's refusal. Returns null if the rider goes back.
     */
    private PaymentMethod choosePayment(Zone zone) {
        List<PaymentType> types = List.of(PaymentType.values());
        List<String> labels = types.stream()
                .map(type -> l10n.render(type) + (zone.getAllowedPayments().contains(type)
                        ? "" : Ansi.paint("  " + t("pay.not.accepted", zone.getName()), Ansi.DIM)))
                .toList();
        int pick = console.choose(t("pay.ask"), labels);
        if (pick < 0) {
            return null;
        }
        return switch (types.get(pick)) {
            case CREDIT_CARD -> new CreditCardPayment(readCardNumber(t("pay.credit.number"), "4111 1111 1111 1111"));
            case TRANSIT_CARD -> new TransitCardPayment(readCardNumber(t("pay.transit.number"), "6012 3456 7890"));
            case PHONE_WALLET -> {
                console.say(t("pay.wallet.paired"));
                yield new PhoneWalletPayment();
            }
        };
    }

    private String readCardNumber(String label, String demoNumber) {
        while (true) {
            String number = console.readOrDefault(t("pay.card.prompt", label, demoNumber), demoNumber);
            if (number.replaceAll("\\D", "").length() >= 4) {
                return number;
            }
            console.say(t("pay.card.too.short"));
        }
    }

    // ---------------------------------------------------------------- 4. time

    /** Advances the simulation by the chosen number of minutes at once, on top of the background clock. */
    private void letTimePass() {
        console.say(Ansi.paint(t("time.background", GoCycleSystem.simSecondsPerTick()), Ansi.DIM));
        int minutes = console.readInt(t("time.ask"), 1, 240);
        system.atomically(() -> system.getSimulator().advance(Duration.ofMinutes(minutes)));
        console.say(t("time.passed", minutes));
    }

    // ---------------------------------------------------------------- 5. return

    /**
     * Asks for a return station and follows redirects (the requested station may be full) until the rider
     * reaches one with a free point, then docks. Declining a redirect leaves the trip running.
     */
    private void returnVehicle() {
        Optional<Trip> active = system.atomically(() -> rider.getActiveTrip());
        if (active.isEmpty()) {
            console.say(t("return.no.trip"));
            return;
        }
        Trip trip = active.get();
        DockingStation chosen = chooseStation(t("stations.ask.return"));
        if (chosen == null) {
            return;
        }
        DockingStation station = chosen;
        while (true) {
            // Effectively-final copy for the lambdas below.
            DockingStation at = station;
            DockingStation target;
            try {
                target = system.atomically(() -> system.getTripService().requestReturn(trip, at));
            } catch (TripException e) {
                system.atomically(() -> at.getKiosk().announce(e.getMessageKey(), e.getArgs()));
                return;
            }
            if (target == at) {
                break;
            }
            double km = StationLocator.distanceKm(at, target);
            system.atomically(() -> at.getKiosk().announce("kiosk.station.full", target.getName(),
                    String.format(l10n.getLanguage(), "%.1f", km)));
            if (!console.confirm(t("return.ride.on", target.getName()))) {
                console.say(t("return.still.running"));
                return;
            }
            int minutes = (int) Math.max(1, Math.ceil(km * RIDE_MINUTES_PER_KM));
            console.say(t("return.riding.to", target.getName(), minutes));
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
                console.say(t("return.point.taken"));
                return;
            }
            DockingPoint point = free.get();
            system.atomically(() -> kiosk.announce("kiosk.dock.instruction", trip.getVehicle().getVehicleId(), point.getPointNumber()));
            console.readLine(t("return.push", point.getPointNumber()));
            // dock also fails if the point was taken while the rider was at the prompt; either way try another.
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
                console.say(receipt.toText(l10n));
                deliveryNote(receipt).ifPresent(note -> console.say(Ansi.paint(note, Ansi.DIM)));
                if (receipt.getUndelivered().contains(ReceiptDelivery.EMAIL)) {
                    console.say(Ansi.paint(t("return.receipt.email.failed", rider.getEmail()), Ansi.YELLOW));
                }
            } catch (TripException e) {
                system.atomically(() -> kiosk.announce(e.getMessageKey(), e.getArgs()));
            }
            return;
        }
    }

    /**
     * Tells the rider where the receipt went, according to their delivery setting and leaving out channels
     * that failed; empty when no copy was delivered.
     */
    private Optional<String> deliveryNote(Receipt receipt) {
        ReceiptDelivery delivery = rider.getReceiptDelivery();
        List<String> notes = new ArrayList<>();
        if (delivery.includes(ReceiptDelivery.APP) && !receipt.getUndelivered().contains(ReceiptDelivery.APP)) {
            notes.add(t("return.receipt.app"));
        }
        if (delivery.includes(ReceiptDelivery.EMAIL) && !receipt.getUndelivered().contains(ReceiptDelivery.EMAIL)) {
            notes.add(t("return.receipt.email", rider.getEmail(), system.getOutbox().resolve(receipt.getTripId() + ".txt")));
        }
        return notes.isEmpty() ? Optional.empty() : Optional.of(t("return.receipt", String.join(t("return.receipt.and"), notes)));
    }

    // ---------------------------------------------------------------- 6. faults

    /**
     * Files a fault for the vehicle being ridden or for one docked at a station. Only a report made at a
     * station is shown on that station's kiosk.
     */
    private void reportProblem() {
        Optional<Trip> active = system.atomically(() -> rider.getActiveTrip());
        Vehicle vehicle;
        StationKiosk kiosk = null;
        List<String> where = new ArrayList<>();
        active.ifPresent(trip -> where.add(t("fault.riding", trip.getVehicle().getVehicleId())));
        where.add(t("fault.docked"));
        int pick = console.choose(t("fault.ask.where"), where);
        if (pick < 0) {
            return;
        }
        if (active.isPresent() && pick == 0) {
            vehicle = active.get().getVehicle();
        } else {
            DockingStation station = chooseStation(t("stations.ask.fault"));
            if (station == null) {
                return;
            }
            List<Vehicle> docked = system.atomically(() -> station.getDockedVehicles());
            if (docked.isEmpty()) {
                console.say(t("fault.none.docked"));
                return;
            }
            int v = console.choose(t("fault.ask.vehicle"), system.atomically(() -> docked.stream().map(this::vehicleLabel).toList()));
            if (v < 0) {
                return;
            }
            vehicle = docked.get(v);
            kiosk = station.getKiosk();
        }
        List<FaultType> types = List.of(FaultType.values());
        int choice = console.choose(t("fault.ask.type"), types.stream().map(l10n::render).toList());
        if (choice < 0) {
            return;
        }
        FaultType type = types.get(choice);
        String note = console.readLine(t("fault.ask.note"));
        // Effectively-final copy of kiosk for the lambda below.
        StationKiosk screen = kiosk;
        FaultReport report = system.atomically(() -> {
            FaultReport r = system.getFaultService().reportByRider(rider, vehicle, type, note);
            if (screen != null) {
                screen.announce(type.requiresRepair() ? "kiosk.fault.thanks" : "kiosk.fault.noted",
                        vehicle.getVehicleId(), type);
            }
            return r;
        });
        console.say(t("fault.filed", report.getReportId()));
        if (screen == null && type.requiresRepair()) {
            console.say(t("fault.return.vehicle", vehicle.getVehicleId()));
        }
    }

    // ---------------------------------------------------------------- 7. trips

    private void showTrips() {
        console.say(system.atomically(() -> {
            StringBuilder b = new StringBuilder(System.lineSeparator());
            b.append(Ansi.paint(t("trips.title", riderLabel(rider)), Ansi.BOLD)).append(System.lineSeparator());
            if (rider.getTrips().isEmpty()) {
                b.append("  ").append(t("trips.none")).append(System.lineSeparator());
            }
            for (Trip trip : rider.getTrips()) {
                String to = trip.getEndStation() == null ? "..." : trip.getEndStation().getName();
                String state = trip.isActive()
                        ? Ansi.paint(t("trips.in.progress", Money.format(trip.costSoFar(system.getClock().now()))), Ansi.CYAN)
                        : t("trips.paid", Money.format(trip.getCost()));
                b.append(String.format("  %s  %-7s %s -> %s  %d min  ", trip.getTripId(), trip.getVehicle().getVehicleId(),
                                trip.getStartStation().getName(), to, trip.durationMinutes(system.getClock().now())))
                        .append(state).append(System.lineSeparator());
            }
            List<Receipt> inbox = rider.getAppInbox();
            b.append(System.lineSeparator()).append(Ansi.paint(t("trips.inbox", inbox.size()), Ansi.BOLD))
                    .append(System.lineSeparator());
            if (!inbox.isEmpty()) {
                b.append(inbox.get(inbox.size() - 1).toText(l10n)).append(System.lineSeparator());
            }
            if (rider.getReceiptDelivery().includes(ReceiptDelivery.EMAIL)) {
                b.append(t("trips.email.folder", system.getOutbox())).append(System.lineSeparator());
            }
            return b.toString();
        }));
        console.pause();
    }

    // ---------------------------------------------------------------- 8. settings

    /** The language here is the system-wide one; the receipt setting belongs to the signed-in rider. */
    private void settings() {
        while (true) {
            List<String> options = system.atomically(() -> List.of(
                    t("settings.language", languageName(l10n.getLanguage())),
                    t("settings.receipts", rider.getReceiptDelivery())));
            int pick = console.choose(t("settings.title", rider.getName()), options);
            switch (pick) {
                case 0 -> {
                    List<Locale> locales = l10n.supportedLocales();
                    int l = console.choose(t("settings.choose.language"), locales.stream().map(RiderApp::languageName).toList());
                    if (l >= 0) {
                        l10n.setLanguage(locales.get(l));
                    }
                }
                case 1 -> {
                    List<ReceiptDelivery> channels = List.of(ReceiptDelivery.values());
                    int c = console.choose(t("settings.choose.receipts"), channels.stream().map(l10n::render).toList());
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

    /** Header shown before each menu: clock, signed-in rider, fare and active trip. Call inside the lock. */
    private String statusPanel() {
        StringBuilder b = new StringBuilder(System.lineSeparator());
        b.append(Ansi.paint(" GoCycle ", Ansi.BOLD, Ansi.background("#1B5E20"), Ansi.foreground("#FFFFFF")))
                .append("  ").append(CLOCK.withLocale(l10n.getLanguage()).format(system.getClock().now()))
                .append("  |  ").append(Ansi.paint(riderLabel(rider), Ansi.BOLD))
                .append("  |  ").append(l10n.render(rider.getPricingStrategy().describe()))
                .append(System.lineSeparator());
        Optional<Trip> active = rider.getActiveTrip();
        if (active.isEmpty()) {
            b.append("  ").append(t("status.no.trip"));
        } else {
            Trip trip = active.get();
            Vehicle v = trip.getVehicle();
            String where = trip.getStatus() == Trip.Status.AWAITING_DOCK
                    ? t("status.docking", trip.getEndStation()) : t("status.riding");
            b.append(Ansi.paint("  " + t("status.trip", trip.getTripId(), where), Ansi.CYAN))
                    .append(l10n.render(v.getType())).append(" ").append(v.getVehicleId())
                    .append(v instanceof ElectricVehicle ev ? " " + ev.getBattery().getChargePercent() + "%" : "")
                    .append(" ").append(t("status.from", trip.getStartStation()))
                    .append(" | ").append(trip.durationMinutes(system.getClock().now())).append(" min | ")
                    .append(Ansi.paint(t("status.so.far", Money.format(trip.costSoFar(system.getClock().now()))), Ansi.BOLD))
                    .append(" | ").append(t("status.deposit", Money.format(trip.getDeposit().getAmount())));
        }
        return b.toString();
    }

    /** Returns the chosen station, or null if the rider goes back. */
    private DockingStation chooseStation(String title) {
        // Read outside the lock: the station list never changes after the system is built.
        List<DockingStation> stations = system.getStations();
        List<String> labels = system.atomically(() -> stations.stream()
                .map(s -> s.getName() + " (" + s.getZone().getName() + ") - " + stationCounts(s))
                .toList());
        int pick = console.choose(title, labels);
        return pick < 0 ? null : stations.get(pick);
    }

    /** "N ready, M free point(s)" for a station. Call inside the lock. */
    private String stationCounts(DockingStation station) {
        return t("stations.counts", station.getRentableVehicles().size(), station.getFreePoints().size());
    }

    /** One aligned line per vehicle: id, type, battery and why it can or cannot be rented. Call inside the lock. */
    private String vehicleLabel(Vehicle v) {
        String battery = v instanceof ElectricVehicle ev ? t("vehicle.battery", ev.getBattery().getChargePercent()) : "";
        String state;
        if (v.isRentable()) {
            state = Ansi.paint(t("vehicle.ready"), Ansi.GREEN);
        } else if (v.getStatus() == VehicleStatus.AVAILABLE) {
            state = Ansi.paint(t("vehicle.battery.too.low"), Ansi.YELLOW);
        } else {
            state = Ansi.paint(l10n.render(v.getStatus()).toLowerCase(l10n.getLanguage()), Ansi.RED);
        }
        // Column widths follow the longest label in the current language so the rows stay aligned.
        int typeWidth = Arrays.stream(VehicleType.values()).mapToInt(type -> l10n.render(type).length()).max().orElse(0);
        int batteryWidth = t("vehicle.battery", 100).length();
        return String.format("%-7s %-" + typeWidth + "s %-" + batteryWidth + "s ",
                v.getVehicleId(), l10n.render(v.getType()), battery) + state;
    }

    /** "Name (Kind)", with the kind translated. Call inside the lock: the kind can depend on rider state. */
    private String riderLabel(Rider r) {
        return t("rider.label", r.getName(), r.getKind());
    }

    /**
     * Fleet-event listener: queues a notice when the vehicle the signed-in rider is on reports a problem by
     * itself, e.g. low battery. Runs on whichever thread published the event, inside the lock.
     */
    private void noticeForCurrentRider(FleetEvent event) {
        // Read the volatile field once so the whole check uses the same rider.
        Rider current = rider;
        event.getFaultReport()
                .filter(report -> report.getReportedBy().isEmpty())
                .ifPresent(report -> current.getActiveTrip()
                        .filter(trip -> trip.getVehicle() == report.getVehicle())
                        .ifPresent(trip -> {
                            // Only electric vehicles report faults by themselves (FaultService.reportByVehicle).
                            if (trip.getVehicle() instanceof ElectricVehicle ev) {
                                notices.add(t("fault.notice", ev.getType(), ev.getVehicleId(), report.getType(),
                                        ev.getBattery().getChargePercent()));
                            }
                        }));
    }

    /** Shorthand for {@link LocalizationService#text} in the current language. */
    private String t(String key, Object... args) {
        return l10n.text(key, args);
    }

    /** The language's own name, capitalized: "English", "Français". */
    private static String languageName(Locale locale) {
        String name = locale.getDisplayLanguage(locale);
        return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
    }
}
