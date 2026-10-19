/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Tests for fault reporting, maintenance repairs and battery behaviour, run against TestNetwork.
 */
package gocycle;

import gocycle.faults.FaultReport;
import gocycle.faults.FaultType;
import gocycle.faults.FleetEvent;
import gocycle.faults.MaintenanceCrew;
import gocycle.fleet.ElectricVehicle;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;
import gocycle.fleet.VehicleType;
import gocycle.payments.CreditCardPayment;
import gocycle.stations.DockingPoint;
import gocycle.trips.Trip;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers fault reports from riders and from electric vehicles, the out-of-service rules (including
 * faults reported mid-trip), maintenance-crew repairs and the 20% battery rental threshold.
 */
class FaultHandlingTest {
    private final TestNetwork net = new TestNetwork();

    @Test
    void brokenVehicleGoesOutOfServiceAndIsRepairedByTheCrew() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);

        FaultReport report = net.faults.reportByRider(net.rider, bike, FaultType.BROKEN_BRAKES, "");

        assertEquals(VehicleStatus.OUT_OF_SERVICE, bike.getStatus());
        assertFalse(bike.isRentable());
        assertEquals(1, net.eventsOf(FleetEvent.Kind.FAULT_REPORTED).size());

        net.advanceMinutes(MaintenanceCrew.REPAIR_TIME.toMinutes() - 1);
        net.crew.completeDueRepairs();
        assertEquals(VehicleStatus.OUT_OF_SERVICE, bike.getStatus());

        net.advanceMinutes(1);
        net.crew.completeDueRepairs();
        assertEquals(VehicleStatus.AVAILABLE, bike.getStatus());
        assertTrue(report.isResolved());
        assertEquals(1, net.eventsOf(FleetEvent.Kind.BACK_IN_SERVICE).size());
    }

    @Test
    void faultReportedMidTripTakesEffectWhenDocked() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        Trip trip = net.trips.startTrip(net.rider, net.a, bike, new CreditCardPayment("4111"));

        net.faults.reportByRider(net.rider, bike, FaultType.FLAT_TIRE, "back tire");
        assertEquals(VehicleStatus.IN_USE, bike.getStatus());
        assertTrue(bike.isServiceDue());

        net.trips.requestReturn(trip, net.a);
        DockingPoint point = net.a.getFreePoints().get(0);
        point.dock(bike);
        net.trips.confirmDocked(trip, point);

        assertEquals(VehicleStatus.OUT_OF_SERVICE, bike.getStatus());
    }

    // An e-bike drains 1% per minute: 21% -> 20% stays rentable, 20% -> 19% triggers the report, and the
    // third minute (18%) must not report again.
    @Test
    void electricVehicleReportsLowBatteryOnItsOwnOnce() {
        ElectricVehicle bike = (ElectricVehicle) net.park(net.a, VehicleType.E_BIKE, "EB-1", 21);

        bike.ride(1);
        assertTrue(net.eventsOf(FleetEvent.Kind.FAULT_REPORTED).isEmpty());
        bike.ride(1);
        bike.ride(1);

        assertEquals(1, net.eventsOf(FleetEvent.Kind.FAULT_REPORTED).size());
        FaultReport report = net.faults.getReports().get(0);
        assertEquals(FaultType.LOW_BATTERY, report.getType());
        assertTrue(report.getReportedBy().isEmpty());
        assertFalse(bike.isRentable(), "below 20% cannot be rented");
        assertTrue(net.faults.getOpenReports().isEmpty(), "low battery needs a charge, not a repair");
    }

    @Test
    void dockedElectricVehicleRechargesUntilRentable() {
        ElectricVehicle scooter = (ElectricVehicle) net.park(net.a, VehicleType.E_SCOOTER, "ES-1", 19);
        assertFalse(scooter.isRentable());

        // Exactly long enough to gain 1%, taking it from 19% to the 20% threshold.
        scooter.rest(ElectricVehicle.MINUTES_PER_PERCENT_CHARGED);

        assertEquals(20, scooter.getBattery().getChargePercent());
        assertTrue(scooter.isRentable());
    }
}
