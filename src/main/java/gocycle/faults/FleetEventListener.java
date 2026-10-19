/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Observer interface of the fleet event design, implemented by the maintenance crew and used
 * as a lambda by the rider app and tests.
 */
package gocycle.faults;

/** Observer in the fleet event design: anything that wants fleet notifications. */
@FunctionalInterface
public interface FleetEventListener {
    /**
     * Called synchronously on the publishing thread (in the app, inside {@code GoCycleSystem.atomically}).
     * An exception thrown here propagates to the publisher's caller and skips the remaining listeners.
     */
    void onEvent(FleetEvent e);
}
