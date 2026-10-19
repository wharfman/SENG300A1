/*
 * GoCycle - SENG 300 Assignment 1
 *
 * The station-side user interface. RiderApp drives it when a rider is "at" a station, and it is where
 * TripException message keys are turned into text in the system language via LocalizationService and
 * TextToSpeech.
 */
package gocycle.stations;

import gocycle.common.Ansi;
import gocycle.riders.Rider;

import java.io.PrintStream;
import java.util.Objects;

/**
 * The on-site screen. Shows text in the system-wide language under the zone's branding, and reads it
 * aloud for riders who turned on audio assist.
 *
 * <p>Each station owns one kiosk, but in the demo every kiosk prints to the same terminal stream. The kiosk
 * remembers the audio setting of the last rider passed to {@link #serve}, so call {@code serve} before
 * announcing to a different rider; until then it does not read aloud. Not thread-safe: callers go through
 * {@code GoCycleSystem.atomically}.
 */
public class StationKiosk {
    private final LocalizationService localization;
    private final TextToSpeech speech;
    private final PrintStream screen;
    private String stationName = "";
    private ZoneBranding branding = ZoneBranding.DEFAULT;
    private boolean audioAssist;

    public StationKiosk(LocalizationService localization, TextToSpeech speech, PrintStream screen) {
        this.localization = Objects.requireNonNull(localization);
        this.speech = Objects.requireNonNull(speech);
        this.screen = Objects.requireNonNull(screen);
    }

    void attachTo(String stationName) {
        this.stationName = stationName;
    }

    /** Switches read-aloud to the setting of the rider now at the kiosk. */
    public void serve(Rider rider) {
        audioAssist = rider.isAudioAssist();
    }

    /**
     * Prints a branded header bar (station name, zone colours, logo path) followed by the translated message.
     * Never reads aloud; use {@link #announce} for rider-facing messages.
     */
    public void display(String messageKey, Object... args) {
        String bar = Ansi.paint(" GoCycle | " + stationName + " ",
                Ansi.background(branding.getPrimaryColour()), Ansi.foreground("#FFFFFF"), Ansi.BOLD)
                + Ansi.paint("  ", Ansi.background(branding.getSecondaryColour()))
                + Ansi.paint(" logo: " + branding.getLogoPath(), Ansi.DIM);
        screen.println();
        screen.println("  " + bar);
        screen.println("  " + text(messageKey, args));
    }

    /** Speaks the translated message regardless of the rider's audio-assist setting. */
    public void readAloud(String messageKey, Object... args) {
        speech.speak(text(messageKey, args), localization.getLanguage());
    }

    /** Displays the message, and reads it aloud when the current rider asked for audio. */
    public void announce(String messageKey, Object... args) {
        display(messageKey, args);
        if (audioAssist) {
            readAloud(messageKey, args);
        }
    }

    /** Called by {@link DockingStation} when the station joins a zone. */
    public void applyBranding(ZoneBranding branding) {
        this.branding = Objects.requireNonNull(branding);
    }

    /** Translates {@code messageKey} into the system language; see {@link LocalizationService#text}. */
    public String text(String messageKey, Object... args) {
        return localization.text(messageKey, args);
    }
}
