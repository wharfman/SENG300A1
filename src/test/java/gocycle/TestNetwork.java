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
        StationKiosk kiosk = new StationKiosk(new InMemoryLocalizationService(),
                new ConsoleTextToSpeech(new PrintStream(OutputStream.nullOutputStream())),
                new PrintStream(OutputStream.nullOutputStream()));
        DockingStation station = new DockingStation(name, name, latitude, longitude, points, kiosk, () -> lockWorks);
        zone.addStation(station);
        return station;
    }
}
