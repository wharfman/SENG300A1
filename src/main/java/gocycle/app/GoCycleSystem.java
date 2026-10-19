/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Application layer: wires every domain package together for the terminal program. RiderApp owns one
 * instance and reaches all services, stations, vehicles and riders through it.
 */
package gocycle.app;

import gocycle.common.SimulationClock;
import gocycle.controlroom.ControlRoomService;
import gocycle.controlroom.Operator;
import gocycle.controlroom.OperatorDashboard;
import gocycle.faults.FaultService;
import gocycle.faults.FleetEventPublisher;
import gocycle.faults.MaintenanceCrew;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleFactory;
import gocycle.fleet.VehicleType;
import gocycle.payments.PaymentType;
import gocycle.riders.CasualRider;
import gocycle.riders.Member;
import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;
import gocycle.riders.Student;
import gocycle.stations.ConsoleTextToSpeech;
import gocycle.stations.DockingStation;
import gocycle.stations.InMemoryLocalizationService;
import gocycle.stations.LocalizationService;
import gocycle.stations.StationKiosk;
import gocycle.stations.StationLocator;
import gocycle.stations.Zone;
import gocycle.stations.ZoneBranding;
import gocycle.trips.AppReceiptSender;
import gocycle.trips.EmailReceiptSender;
import gocycle.trips.TripService;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Composition root: builds the services, seeds the city's stations, vehicles and riders, and runs
 * the simulated clock. All domain access goes through {@link #atomically} so the clock thread and
 * the menu never interleave.
 *
 * <p>The domain objects are not thread-safe, so the lists and services returned by the accessors are
 * live state and must only be read or changed inside {@link #atomically}. Fleet events are delivered
 * synchronously on the publishing thread, so subscribers (the maintenance crew, the operator dashboard
 * and the rider app) also run while that lock is held. The station list is fixed after construction.
 */
public final class GoCycleSystem {
    public static final double MEMBER_MONTHLY_FEE = 15.00;
    private static final Path CONFIG_FILE = Path.of("config", "zones.properties");
    private static final double DEFAULT_LOCK_FAILURE_RATE = 0.1;
    private static final int DEFAULT_SIM_SECONDS_PER_TICK = 15;

    private final Object lock = new Object();
    private final SimulationClock clock = new SimulationClock(LocalDateTime.now().withSecond(0).withNano(0));
    private final FleetEventPublisher publisher = new FleetEventPublisher();
    private final LocalizationService localization = new InMemoryLocalizationService();
    private final FaultService faultService;
    private final MaintenanceCrew crew;
    private final List<Zone> zones = new ArrayList<>();
    private final List<DockingStation> stations = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Rider> riders = new ArrayList<>();
    private final TripService tripService;
    private final FleetSimulator simulator;
    private final Operator operator;
    private final Path outbox;
    private ScheduledExecutorService ticker;
    private int nextRiderNumber = 1;

    /**
     * Builds and seeds the whole network. Does not start the clock; call {@link #startClock()} for that.
     *
     * @param config          zone settings keyed {@code <zoneId>.<setting>} (see {@code config/zones.properties});
     *                        may be empty, in which case the built-in zone names, all payment types and the
     *                        default branding are used
     * @param dataDir         emailed receipts are written to its {@code outbox} subfolder
     * @param kioskScreen     where every station kiosk and the operator dashboard print
     * @param random          drives the lock sensors and the simulator's random faults
     * @param lockFailureRate probability (0 to 1) that a docking point's latch fails to catch on one attempt
     * @throws IllegalArgumentException if a zone lists a payment type that is not a {@link PaymentType} name
     */
    public GoCycleSystem(Properties config, Path dataDir, PrintStream kioskScreen, Random random, double lockFailureRate) {
        faultService = new FaultService(clock, publisher);
        ControlRoomService controlRoom = new ControlRoomService(clock, publisher);
        crew = new MaintenanceCrew("Crew A", clock, controlRoom);
        publisher.subscribe(crew);

        // Each station gets its own kiosk, but they all print to the same screen.
        Supplier<StationKiosk> kiosks = () -> new StationKiosk(localization, new ConsoleTextToSpeech(kioskScreen, localization), kioskScreen);
        seedNetwork(config, kiosks, () -> random.nextDouble() >= lockFailureRate);
        seedRiders();

        // Built after seeding: the locator and the simulator need the final station list.
        outbox = dataDir.resolve("outbox");
        tripService = new TripService(clock, new StationLocator(stations),
                List.of(new AppReceiptSender(), new EmailReceiptSender(outbox, localization)), publisher);
        simulator = new FleetSimulator(clock, stations, tripService, crew, controlRoom, random);
        OperatorDashboard dashboard = new OperatorDashboard(kioskScreen, clock, zones, vehicles, tripService,
                faultService, crew);
        publisher.subscribe(dashboard);
        operator = new Operator("OP-1", "Jordan Lee",
                zones.stream().map(Zone::getZoneId).collect(Collectors.toSet()), dashboard);
    }

    /**
     * Builds the system from {@code config/zones.properties} (resolved against the working directory; a missing
     * file means defaults) and the {@code gocycle.lockFailureRate} and {@code gocycle.dataDir} system properties.
     *
     * @throws UncheckedIOException if the config file exists but cannot be read
     */
    public static GoCycleSystem create() {
        Properties config = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                config.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + CONFIG_FILE, e);
            }
        }
        double lockFailureRate = Double.parseDouble(
                System.getProperty("gocycle.lockFailureRate", String.valueOf(DEFAULT_LOCK_FAILURE_RATE)));
        return new GoCycleSystem(config, dataDir(), System.out, new Random(), lockFailureRate);
    }

    /** Data directory from {@code gocycle.dataDir} (default {@code data}); emailed receipts go to its {@code outbox}. */
    public static Path dataDir() {
        return Path.of(System.getProperty("gocycle.dataDir", "data"));
    }

    /** Simulated seconds the clock thread adds per real second, from {@code gocycle.simSecondsPerTick} (default 15). */
    public static int simSecondsPerTick() {
        return Integer.getInteger("gocycle.simSecondsPerTick", DEFAULT_SIM_SECONDS_PER_TICK);
    }

    // ---------------------------------------------------------------- seeding

    /** Creates the two Calgary zones and their stations, each partly filled with vehicles. */
    private void seedNetwork(Properties config, Supplier<StationKiosk> kiosks, BooleanSupplier lockSensor) {
        VehicleFactory factory = new VehicleFactory(faultService);
        Zone downtown = zone(config, "downtown", "Downtown");
        Zone kensington = zone(config, "kensington", "Kensington");

        DockingStation cityHall = station(downtown, "CH", "City Hall", 51.0453, -114.0581, 6, kiosks, lockSensor);
        park(cityHall, factory.create(VehicleType.PEDAL_BIKE, "PB-101"));
        park(cityHall, factory.create(VehicleType.E_BIKE, "EB-201", 85));
        park(cityHall, factory.create(VehicleType.E_SCOOTER, "ES-301", 12));
        park(cityHall, factory.create(VehicleType.PEDAL_BIKE, "PB-102"));

        DockingStation stephenAve = station(downtown, "SA", "Stephen Avenue", 51.0459, -114.0690, 5, kiosks, lockSensor);
        park(stephenAve, factory.create(VehicleType.E_BIKE, "EB-202", 34));
        park(stephenAve, factory.create(VehicleType.PEDAL_BIKE, "PB-103"));
        park(stephenAve, factory.create(VehicleType.E_SCOOTER, "ES-302", 90));

        // Starts full, so returning here shows the redirect to the nearest free point.
        DockingStation eauClaire = station(downtown, "EC", "Eau Claire", 51.0530, -114.0707, 4, kiosks, lockSensor);
        park(eauClaire, factory.create(VehicleType.PEDAL_BIKE, "PB-104"));
        park(eauClaire, factory.create(VehicleType.PEDAL_BIKE, "PB-105"));
        park(eauClaire, factory.create(VehicleType.E_BIKE, "EB-203", 60));
        park(eauClaire, factory.create(VehicleType.E_SCOOTER, "ES-303", 75));

        DockingStation kensingtonRd = station(kensington, "KR", "Kensington Road", 51.0524, -114.0869, 5, kiosks, lockSensor);
        park(kensingtonRd, factory.create(VehicleType.PEDAL_BIKE, "PB-106"));
        park(kensingtonRd, factory.create(VehicleType.E_BIKE, "EB-204", 95));
        park(kensingtonRd, factory.create(VehicleType.E_SCOOTER, "ES-304", 48));

        DockingStation sunnyside = station(kensington, "SS", "Sunnyside", 51.0566, -114.0812, 4, kiosks, lockSensor);
        park(sunnyside, factory.create(VehicleType.PEDAL_BIKE, "PB-107"));
        park(sunnyside, factory.create(VehicleType.E_BIKE, "EB-205", 18));
    }

    /**
     * Reads {@code <id>.name}, {@code <id>.payments} and the branding keys for one zone. A missing or blank
     * {@code payments} list allows every payment type.
     */
    private Zone zone(Properties config, String id, String defaultName) {
        Properties own = new Properties();
        String prefix = id + ".";
        for (String key : config.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                own.setProperty(key.substring(prefix.length()), config.getProperty(key));
            }
        }
        Set<PaymentType> payments = EnumSet.allOf(PaymentType.class);
        String listed = own.getProperty("payments");
        if (listed != null && !listed.isBlank()) {
            payments = Arrays.stream(listed.split(","))
                    .map(String::strip)
                    .map(PaymentType::valueOf)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(PaymentType.class)));
        }
        Zone zone = new Zone(id, own.getProperty("name", defaultName).strip(), payments, ZoneBranding.fromConfig(own));
        zones.add(zone);
        return zone;
    }

    private DockingStation station(Zone zone, String id, String name, double latitude, double longitude,
                                   int points, Supplier<StationKiosk> kiosks, BooleanSupplier lockSensor) {
        DockingStation station = new DockingStation(id, name, latitude, longitude, points, kiosks.get(), lockSensor);
        zone.addStation(station);
        stations.add(station);
        return station;
    }

    /** Docks a newly built vehicle at seeding time and registers it with the fleet. */
    private void park(DockingStation station, Vehicle vehicle) {
        station.dockAnywhere(vehicle);
        vehicles.add(vehicle);
    }

    /** One demo rider of each kind, with settings that show off read-aloud and email receipts. */
    private void seedRiders() {
        signUpCasual("Alex Chen", "alex.chen@example.com");
        signUpMember("Sam Okafor", "sam.okafor@example.com");
        Rider priya = signUpStudent("Priya Natarajan", "priya.natarajan@ucalgary.ca", "30123456");
        priya.setAudioAssist(true);
        priya.setReceiptDelivery(ReceiptDelivery.BOTH);
    }

    // ---------------------------------------------------------------- sign-up

    // Sign-up methods must be called inside atomically; they append to the shared rider list.

    public Rider signUpCasual(String name, String email) {
        return register(new CasualRider(nextRiderId(), name, email));
    }

    /**
     * The member's fee is recorded as {@link #MEMBER_MONTHLY_FEE} dollars a month but never billed by the system;
     * the membership is active until one month from today.
     */
    public Rider signUpMember(String name, String email) {
        return register(new Member(nextRiderId(), name, email, MEMBER_MONTHLY_FEE, LocalDate.now().plusMonths(1)));
    }

    /** Student status counts as verified for eight months from today. */
    public Rider signUpStudent(String name, String email, String studentNumber) {
        return register(new Student(nextRiderId(), name, email, studentNumber, LocalDate.now().plusMonths(8)));
    }

    private Rider register(Rider rider) {
        riders.add(rider);
        return rider;
    }

    private String nextRiderId() {
        return String.format("R-%03d", nextRiderNumber++);
    }

    // ---------------------------------------------------------------- running

    /**
     * Runs {@code action} with exclusive access to the network and returns its result. The lock is a plain
     * monitor, so nested calls from the same thread are fine. Exceptions thrown by {@code action} propagate.
     */
    public <T> T atomically(Supplier<T> action) {
        synchronized (lock) {
            return action.get();
        }
    }

    public void atomically(Runnable action) {
        atomically(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Starts a daemon thread that advances simulated time by {@link #simSecondsPerTick()} seconds every real
     * second, inside {@link #atomically}. Call at most once.
     */
    public void startClock() {
        Duration perTick = Duration.ofSeconds(simSecondsPerTick());
        ticker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "gocycle-clock");
            thread.setDaemon(true);
            return thread;
        });
        ticker.scheduleAtFixedRate(() -> {
            try {
                atomically(() -> simulator.advance(perTick));
            } catch (RuntimeException e) {
                // A scheduled task that throws is silently cancelled, so log and keep the clock running.
                System.err.println("Simulation tick failed: " + e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /** Stops the clock thread, if it was started. Safe to call more than once. */
    public void shutdown() {
        if (ticker != null) {
            ticker.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- accessors

    public SimulationClock getClock() {
        return clock;
    }

    public FleetEventPublisher getPublisher() {
        return publisher;
    }

    public TripService getTripService() {
        return tripService;
    }

    public FaultService getFaultService() {
        return faultService;
    }

    public FleetSimulator getSimulator() {
        return simulator;
    }

    /** The translation service, which also holds the system-wide screen language. */
    public LocalizationService getLocalization() {
        return localization;
    }

    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public List<DockingStation> getStations() {
        return Collections.unmodifiableList(stations);
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    public List<Rider> getRiders() {
        return Collections.unmodifiableList(riders);
    }

    public Operator getOperator() {
        return operator;
    }

    public Path getOutbox() {
        return outbox;
    }
}
