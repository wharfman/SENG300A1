/*
 * GoCycle - SENG 300 Assignment 1
 *
 * The station-side user interface. RiderApp drives it when a rider is "at" a station, and it is where
 * TripException message keys are turned into localized text via LocalizationService and TextToSpeech.
 */
package gocycle.stations;

import gocycle.common.Ansi;
import gocycle.riders.Rider;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

/**
 * The on-site screen. Shows text in the rider's language under the zone's branding, and reads it
 * aloud for riders who turned on audio assist.
 *
 * <p>Each station owns one kiosk, but in the demo every kiosk prints to the same terminal stream. The kiosk
 * remembers the last rider passed to {@link #serve}, so call {@code serve} before announcing to a different
 * rider; until then it uses English without audio. Not thread-safe: callers go through
 * {@code GoCycleSystem.atomically}.
 */
public class StationKiosk {
    private final LocalizationService localization;
    private final TextToSpeech speech;
    private final PrintStream screen;
    private String stationName = "";
    private ZoneBranding branding = ZoneBranding.DEFAULT;
    private Locale locale = Locale.ENGLISH;
    private boolean audioAssist;

    public StationKiosk(LocalizationService localization, TextToSpeech speech, PrintStream screen) {
        this.localization = Objects.requireNonNull(localization);
        this.speech = Objects.requireNonNull(speech);
        this.screen = Objects.requireNonNull(screen);
    }

    void attachTo(String stationName) {
        this.stationName = stationName;
    }

    /** Switches language and read-aloud to the rider now at the kiosk. */
    public void serve(Rider rider) {
        locale = rider.getPreferredLanguage();
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
        speech.speak(text(messageKey, args), locale);
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

    /**
     * Translates {@code messageKey} into the current rider's language and fills {@code {i}} with
     * {@code args[i]} as plain text. Placeholders without a matching argument are left as-is.
     */
    public String text(String messageKey, Object... args) {
        String text = localization.translate(messageKey, locale);
        // A plain replace rather than MessageFormat, which would treat the apostrophes in French text such as
        // "jusqu'au" as quote marks and would re-format numeric arguments.
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    public Locale getLocale() {
        return locale;
    }
}
