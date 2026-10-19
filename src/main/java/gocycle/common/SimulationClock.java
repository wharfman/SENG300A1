/*
 * GoCycle - SENG 300 Assignment 1
 *
 * The single source of "now" for trips, faults and repairs. GoCycleSystem owns one instance
 * and FleetSimulator is what moves it forward; tests advance it directly.
 */
package gocycle.common;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Simulated wall clock shared by every service, so trips can run fast or be fast-forwarded in the demo.
 * It only moves when {@link #advance} is called, never on its own. Methods are synchronized so the
 * background clock thread and the UI thread always see a consistent time, even outside
 * {@code GoCycleSystem.atomically}.
 */
public class SimulationClock {
    private LocalDateTime now;

    public SimulationClock(LocalDateTime start) {
        this.now = start;
    }

    public synchronized LocalDateTime now() {
        return now;
    }

    /**
     * Moves time forward. Does not run any simulation itself; use {@code FleetSimulator.advance} for that,
     * which calls this.
     *
     * @throws IllegalArgumentException if {@code duration} is negative
     */
    public synchronized void advance(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Time cannot go backwards");
        }
        now = now.plus(duration);
    }
}
