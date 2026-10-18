package gocycle.faults;

/** Anything that wants fleet notifications. */
@FunctionalInterface
public interface FleetEventListener {
    void onEvent(FleetEvent e);
}
