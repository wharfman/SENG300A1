/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Shared test fixture: wires a small, deterministic GoCycle network the same way
 * GoCycleSystem does, without the clock thread, simulator or console app.
 */
package gocycle;

import gocycle.common.SimulationClock;
import gocycle.controlroom.ControlRoomService;
import gocycle.faults.FaultService;
import gocycle.faults.FleetEvent;
import gocycle.faults.FleetEventPublisher;
import gocycle.faults.MaintenanceCrew;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleFactory;
import gocycle.fleet.VehicleType;
import gocycle.payments.PaymentType;
import gocycle.riders.CasualRider;
import gocycle.stations.ConsoleTextToSpeech;
import gocycle.stations.DockingStation;
import gocycle.stations.InMemoryLocalizationService;
import gocycle.stations.StationKiosk;
import gocycle.stations.StationLocator;
import gocycle.stations.Zone;
import gocycle.stations.ZoneBranding;
import gocycle.trips.AppReceiptSender;
import gocycle.trips.TripService;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A small network for tests: one zone that accepts credit and transit cards (not phone wallets),
 * with stations A (3 points), B (1 point) and C (2 points). C is closer to B than A is.
 *
 * <p>Assumptions tests rely on: stations start empty (use {@link #park}); the simulated clock starts at
 * 2026-10-18 09:00 and only moves through {@link #advanceMinutes}, and nothing drains or charges batteries
 * unless a test calls {@code ride}/{@code rest}; every published event is recorded in {@link #events}; the
 * maintenance crew is subscribed but no operator dashboard is; kiosk output is discarded; the only
 * receipt sender is the in-app one. Tests hold the network in an instance field, so JUnit gives each test
 * a fresh one.
 */
public final class TestNetwork {
    public final SimulationClock clock = new SimulationClock(LocalDateTime.of(2026, 10, 18, 9, 0));
    public final FleetEventPublisher publisher = new FleetEventPublisher();
    public final List<FleetEvent> events = new ArrayList<>();
    public final FaultService faults = new FaultService(clock, publisher);
    public final ControlRoomService controlRoom = new ControlRoomService(clock, publisher);
    public final MaintenanceCrew crew = new MaintenanceCrew("Test crew", clock, controlRoom);
    public final VehicleFactory factory = new VehicleFactory(faults);
    public final Zone zone = new Zone("test", "Test", EnumSet.of(PaymentType.CREDIT_CARD, PaymentType.TRANSIT_CARD),
            ZoneBranding.DEFAULT);
    /** Flip to false to make docking points fail to latch. */
    public boolean lockWorks = true;
    // Field order matters: stations need zone, and trips needs the stations.
    public final DockingStation a = station("A", 51.000, -114.000, 3);
    public final DockingStation b = station("B", 51.010, -114.000, 1);
    public final DockingStation c = station("C", 51.012, -114.000, 2);
    public final TripService trips = new TripService(clock, new StationLocator(List.of(a, b, c)),
            List.of(new AppReceiptSender()), publisher);
    public final CasualRider rider = new CasualRider("R-1", "Test Rider", "rider@example.com");

    public TestNetwork() {
        publisher.subscribe(events::add);
        publisher.subscribe(crew);
    }

    /**
     * Creates a vehicle and docks it at the station's first free point; {@code charge} is ignored for pedal
     * bikes. Throws if the station is full or {@link #lockWorks} is false.
     */
    public Vehicle park(DockingStation station, VehicleType type, String id, int charge) {
        Vehicle vehicle = factory.create(type, id, charge);
        station.dockAnywhere(vehicle);
        return vehicle;
    }

    public void advanceMinutes(long minutes) {
        clock.advance(Duration.ofMinutes(minutes));
    }

    public List<FleetEvent> eventsOf(FleetEvent.Kind kind) {
        return events.stream().filter(e -> e.getKind() == kind).toList();
    }

    private DockingStation station(String name, double latitude, double longitude, int points) {
        InMemoryLocalizationService localization = new InMemoryLocalizationService();
        StationKiosk kiosk = new StationKiosk(localization,
                new ConsoleTextToSpeech(new PrintStream(OutputStream.nullOutputStream()), localization),
                new PrintStream(OutputStream.nullOutputStream()));
        // The lambda reads the field on every dock, so tests can change lockWorks after construction.
        DockingStation station = new DockingStation(name, name, latitude, longitude, points, kiosk, () -> lockWorks);
        zone.addStation(station);
        return station;
    }
}
