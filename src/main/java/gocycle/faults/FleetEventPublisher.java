/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Subject of the fleet observer design: one instance is shared by every service that publishes
 * fleet events and every listener that consumes them.
 */
package gocycle.faults;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject in the fleet event (observer) design: keeps the subscriber list and notifies each subscriber
 * of every fleet event, in subscription order and on the publishing thread.
 */
public class FleetEventPublisher {
    // Copy-on-write so a listener can subscribe while an event is being delivered.
    private final List<FleetEventListener> subscribers = new CopyOnWriteArrayList<>();

    public void subscribe(FleetEventListener listener) {
        subscribers.add(listener);
    }

    public void publish(FleetEvent event) {
        for (FleetEventListener subscriber : subscribers) {
            subscriber.onEvent(event);
        }
    }
}
