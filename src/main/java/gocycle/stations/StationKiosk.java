package gocycle.stations;

import gocycle.common.Ansi;
import gocycle.riders.Rider;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

/**
 * The on-site screen. Shows text in the rider's language under the zone's branding, and reads it
 * aloud for riders who turned on audio assist.
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

    public void display(String messageKey, Object... args) {
        String bar = Ansi.paint(" GoCycle | " + stationName + " ",
                Ansi.background(branding.getPrimaryColour()), Ansi.foreground("#FFFFFF"), Ansi.BOLD)
                + Ansi.paint("  ", Ansi.background(branding.getSecondaryColour()))
                + Ansi.paint(" logo: " + branding.getLogoPath(), Ansi.DIM);
        screen.println();
        screen.println("  " + bar);
        screen.println("  " + text(messageKey, args));
    }

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

    public void applyBranding(ZoneBranding branding) {
        this.branding = Objects.requireNonNull(branding);
    }

    public String text(String messageKey, Object... args) {
        String text = localization.translate(messageKey, locale);
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    public Locale getLocale() {
        return locale;
    }
}
