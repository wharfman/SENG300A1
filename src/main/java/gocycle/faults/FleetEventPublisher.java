package gocycle.faults;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps the subscriber list and notifies each subscriber of every fleet event. */
public class FleetEventPublisher {
    private final List<FleetEventListener> subscribers = new CopyOnWriteArrayList<>();

    public void subscribe(FleetEventListener listener) {
        subscribers.add(listener);
    }

    public void unsubscribe(FleetEventListener listener) {
        subscribers.remove(listener);
    }

    public void publish(FleetEvent event) {
        for (FleetEventListener subscriber : subscribers) {
            subscriber.onEvent(event);
        }
    }
}
