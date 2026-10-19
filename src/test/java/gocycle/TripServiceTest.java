/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Tests for the trip lifecycle in TripService, run against TestNetwork.
 */
package gocycle;

import gocycle.faults.FleetEvent;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;
import gocycle.fleet.VehicleType;
import gocycle.payments.CreditCardPayment;
import gocycle.payments.PhoneWalletPayment;
import gocycle.pricing.LongTripSurcharge;
import gocycle.riders.ReceiptDelivery;
import gocycle.riders.Rider;
import gocycle.stations.DockingPoint;
import gocycle.stations.DockingStation;
import gocycle.stations.StationLocator;
import gocycle.trips.AppReceiptSender;
import gocycle.trips.Receipt;
import gocycle.trips.ReceiptSender;
import gocycle.trips.Trip;
import gocycle.trips.TripException;
import gocycle.trips.TripService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers starting a trip (deposit hold, zone payment rules, battery and one-active-trip checks), returning
 * to a full station, and ending only once the lock sensor confirms. Fares assume the casual rider's rate
 * (1.25 unlock + 0.35/min).
 */
class TripServiceTest {
    private final TestNetwork net = new TestNetwork();
    private final CreditCardPayment card = new CreditCardPayment("4111 1111 1111 1111");

    @Test
    void startingATripHoldsTheDepositAndUnlocksTheVehicle() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);

        Trip trip = net.trips.startTrip(net.rider, net.a, bike, card);

        assertEquals(Trip.Status.ACTIVE, trip.getStatus());
        assertEquals(VehicleStatus.IN_USE, bike.getStatus());
        assertTrue(net.a.findPointHolding(bike).isEmpty());
        assertEquals(TripService.DEPOSIT_AMOUNT, card.getHeldAmount(), 1e-9);
        assertEquals(1, net.eventsOf(FleetEvent.Kind.TRIP_STARTED).size());
    }

    @Test
    void zoneRefusesPaymentMethodsItDoesNotAllow() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        PhoneWalletPayment wallet = new PhoneWalletPayment("token");

        TripException e = assertThrows(TripException.class, () -> net.trips.startTrip(net.rider, net.a, bike, wallet));

        assertEquals("error.payment.zone", e.getMessageKey());
        assertEquals(VehicleStatus.AVAILABLE, bike.getStatus());
        assertEquals(0, wallet.getHeldAmount(), 1e-9);
    }

    @Test
    void electricVehicleBelowTwentyPercentCannotBeRented() {
        Vehicle low = net.park(net.a, VehicleType.E_SCOOTER, "ES-1", 19);
        Vehicle ok = net.park(net.a, VehicleType.E_BIKE, "EB-1", 20);

        TripException e = assertThrows(TripException.class, () -> net.trips.startTrip(net.rider, net.a, low, card));
        assertEquals("error.battery.low", e.getMessageKey());
        assertTrue(ok.isRentable());
    }

    @Test
    void riderCanOnlyHaveOneActiveTrip() {
        Vehicle first = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        Vehicle second = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-2", 0);
        net.trips.startTrip(net.rider, net.a, first, card);

        TripException e = assertThrows(TripException.class, () -> net.trips.startTrip(net.rider, net.a, second, card));
        assertEquals("error.trip.active", e.getMessageKey());
    }

    @Test
    void tripDoesNotEndUntilTheVehicleIsReallyLocked() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        Trip trip = net.trips.startTrip(net.rider, net.a, bike, card);
        net.advanceMinutes(10);
        DockingStation target = net.trips.requestReturn(trip, net.c);
        DockingPoint point = target.getFreePoints().get(0);

        net.lockWorks = false;
        assertFalse(point.dock(bike));
        TripException e = assertThrows(TripException.class, () -> net.trips.confirmDocked(trip, point));
        assertEquals("error.not.locked", e.getMessageKey());
        assertTrue(trip.isActive());

        // The failed attempt costs the rider time: 10 minutes riding + 2 retrying = 12 billed.
        net.advanceMinutes(2);
        net.lockWorks = true;
        assertTrue(point.dock(bike));
        Receipt receipt = net.trips.confirmDocked(trip, point);

        assertEquals(Trip.Status.COMPLETED, trip.getStatus());
        assertEquals(12, receipt.getMinutes());
        assertEquals(1.25 + 12 * 0.35, receipt.getAmount(), 1e-9);
        assertEquals(receipt.getAmount(), card.getTotalCharged(), 1e-9);
        assertEquals(0, card.getHeldAmount(), 1e-9);
        assertTrue(trip.getDeposit().isReleased());
        assertEquals(VehicleStatus.AVAILABLE, bike.getStatus());
        assertSame(receipt, net.rider.getAppInbox().get(0));
    }

    @Test
    void failedEmailReceiptStillEndsTheTripAndDeliversTheOtherCopies() {
        ReceiptSender brokenEmail = new ReceiptSender() {
            @Override
            public void send(Receipt receipt, Rider to) {
                throw new UncheckedIOException(new IOException("outbox is read-only"));
            }

            @Override
            public ReceiptDelivery channel() {
                return ReceiptDelivery.EMAIL;
            }
        };
        TripService trips = new TripService(net.clock, new StationLocator(List.of(net.a, net.b, net.c)),
                List.of(brokenEmail, new AppReceiptSender()), net.publisher);
        net.rider.setReceiptDelivery(ReceiptDelivery.BOTH);
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        Trip trip = trips.startTrip(net.rider, net.a, bike, card);
        net.advanceMinutes(10);
        trips.requestReturn(trip, net.b);
        DockingPoint point = net.b.getFreePoints().get(0);
        point.dock(bike);

        Receipt receipt = trips.confirmDocked(trip, point);

        assertEquals(Trip.Status.COMPLETED, trip.getStatus());
        assertEquals(receipt.getAmount(), card.getTotalCharged(), 1e-9);
        assertEquals(Set.of(ReceiptDelivery.EMAIL), receipt.getUndelivered());
        // The email sender runs first, so this shows a failure doesn't stop the remaining channels.
        assertSame(receipt, net.rider.getAppInbox().get(0));
        assertEquals(1, net.eventsOf(FleetEvent.Kind.RECEIPT_FAILED).size());
        assertEquals(1, net.eventsOf(FleetEvent.Kind.TRIP_ENDED).size());
    }

    @Test
    void fullStationRedirectsToNearestFreeStationAndKeepsCharging() {
        // Fills B's only point.
        net.park(net.b, VehicleType.PEDAL_BIKE, "PB-B", 0);
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        Trip trip = net.trips.startTrip(net.rider, net.a, bike, card);
        net.advanceMinutes(5);

        DockingStation target = net.trips.requestReturn(trip, net.b);

        assertSame(net.c, target, "C is the nearest station with a free point");
        assertEquals(Trip.Status.ACTIVE, trip.getStatus());
        assertEquals(1, net.eventsOf(FleetEvent.Kind.TRIP_REDIRECTED).size());

        // A redirect leaves the trip ACTIVE, so the rider must request a return at C before docking there.
        net.advanceMinutes(3);
        assertSame(net.c, net.trips.requestReturn(trip, net.c));
        DockingPoint point = net.c.getFreePoints().get(0);
        point.dock(bike);
        Receipt receipt = net.trips.confirmDocked(trip, point);
        assertEquals(8, receipt.getMinutes(), "charging continued during the redirect");
    }

    @Test
    void longTripPaysTheSurcharge() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        Trip trip = net.trips.startTrip(net.rider, net.a, bike, card);
        // Past the 120-minute surcharge threshold.
        net.advanceMinutes(130);
        net.trips.requestReturn(trip, net.a);
        DockingPoint point = net.a.getFreePoints().get(0);
        point.dock(bike);

        Receipt receipt = net.trips.confirmDocked(trip, point);

        assertEquals(1.25 + 130 * 0.35 + LongTripSurcharge.DEFAULT_SURCHARGE, receipt.getAmount(), 1e-9);
    }
}
