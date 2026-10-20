/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Application layer: the read-aloud setting. RiderApp and Console call it whenever a menu or submenu
 * appears; option 8 turns it on and off.
 */
package gocycle.app;

import gocycle.stations.LocalizationService;

import java.util.Objects;
import java.util.Optional;

/**
 * Reads the header of each menu aloud, in the system language, when the setting is on. Like the language,
 * the setting is global rather than per rider, and it starts off. It can only be turned on when this
 * computer has a speech engine.
 */
final class ReadAloud {
    // Null when this computer has no speech engine.
    private final TextToSpeech engine;
    private final LocalizationService l10n;
    private boolean on;

    ReadAloud(Optional<TextToSpeech> engine, LocalizationService l10n) {
        this.engine = engine.orElse(null);
        this.l10n = Objects.requireNonNull(l10n);
    }

    boolean isAvailable() {
        return engine != null;
    }

    boolean isOn() {
        return on;
    }

    /** Has no effect when no speech engine is available. */
    void setOn(boolean on) {
        this.on = on && isAvailable();
    }

    /** Speaks {@code text} if the setting is on; returns at once either way. */
    void header(String text) {
        if (on) {
            engine.speak(text, l10n.getLanguage());
        }
    }

    void close() {
        if (engine != null) {
            engine.close();
        }
    }
}
