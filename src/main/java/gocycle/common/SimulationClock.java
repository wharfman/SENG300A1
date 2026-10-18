package gocycle.common;

import java.time.Duration;
import java.time.LocalDateTime;

/** Simulated wall clock shared by every service, so trips can run fast or be fast-forwarded in the demo. */
public class SimulationClock {
    private LocalDateTime now;

    public SimulationClock(LocalDateTime start) {
        this.now = start;
    }

    public synchronized LocalDateTime now() {
        return now;
    }

    public synchronized void advance(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Time cannot go backwards");
        }
        now = now.plus(duration);
    }
}
