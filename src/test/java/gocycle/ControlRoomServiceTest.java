/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Tests for the control room's vehicle moves, run against TestNetwork.
 */
package gocycle;

import gocycle.faults.FleetEvent;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;
import gocycle.fleet.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers rebalancing: a successful move, and a destination whose latch never catches, which must leave the
 * vehicle docked where it was rather than lost.
 */
class ControlRoomServiceTest {
    private final TestNetwork net = new TestNetwork();

    @Test
    void rebalanceMovesTheVehicleAndKeepsItRentable() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);

        assertTrue(net.controlRoom.rebalance(bike, net.a, net.c));

        assertTrue(net.a.findPointHolding(bike).isEmpty());
        assertTrue(net.c.findPointHolding(bike).isPresent());
        assertEquals(VehicleStatus.AVAILABLE, bike.getStatus());
        assertEquals(1, net.eventsOf(FleetEvent.Kind.REBALANCED).size());
    }

    @Test
    void destinationThatWillNotLockLeavesTheVehicleWhereItWas() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        net.lockWorks = false;

        assertFalse(net.controlRoom.rebalance(bike, net.a, net.c));

        assertTrue(net.a.findPointHolding(bike).isPresent(), "still docked at the origin");
        assertTrue(net.c.findPointHolding(bike).isEmpty());
        assertEquals(VehicleStatus.AVAILABLE, bike.getStatus(), "not left IN_TRANSIT");
        assertTrue(bike.isRentable());
        assertEquals(1, net.eventsOf(FleetEvent.Kind.REBALANCE_FAILED).size());
        assertTrue(net.eventsOf(FleetEvent.Kind.REBALANCED).isEmpty());
    }

    @Test
    void failedMoveOfABrokenVehicleKeepsItOutOfService() {
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        bike.markOutOfService();
        net.lockWorks = false;

        assertFalse(net.controlRoom.rebalance(bike, net.a, net.c));

        assertEquals(VehicleStatus.OUT_OF_SERVICE, bike.getStatus());
        assertTrue(net.a.findPointHolding(bike).isPresent());
    }
}
