/*
 * GoCycle - SENG 300 Assignment 1
 *
 * The station-side user interface. RiderApp drives it when a rider is "at" a station, and it is where
 * TripException message keys are turned into text in the system language via LocalizationService.
 */
package gocycle.stations;

import gocycle.common.Ansi;

import java.io.PrintStream;
import java.util.Objects;

/**
 * The on-site screen. Shows text in the system-wide language under the zone's branding.
 *
 * <p>Each station owns one kiosk, but in the demo every kiosk prints to the same terminal stream. Not
 * thread-safe: callers go through {@code GoCycleSystem.atomically}.
 */
public class StationKiosk {
    private final LocalizationService localization;
    private final PrintStream screen;
    private String stationName = "";
    private ZoneBranding branding = ZoneBranding.DEFAULT;

    public StationKiosk(LocalizationService localization, PrintStream screen) {
        this.localization = Objects.requireNonNull(localization);
        this.screen = Objects.requireNonNull(screen);
    }

    void attachTo(String stationName) {
        this.stationName = stationName;
    }

    /**
     * Prints a branded header bar (station name, zone colours, logo path) followed by {@code messageKey}
     * translated into the system language; see {@link LocalizationService#text}.
     */
    public void announce(String messageKey, Object... args) {
        String bar = Ansi.paint(" GoCycle | " + stationName + " ",
                Ansi.background(branding.getPrimaryColour()), Ansi.foreground("#FFFFFF"), Ansi.BOLD)
                + Ansi.paint("  ", Ansi.background(branding.getSecondaryColour()))
                + Ansi.paint(" logo: " + branding.getLogoPath(), Ansi.DIM);
        screen.println();
        screen.println("  " + bar);
        screen.println("  " + localization.text(messageKey, args));
    }

    /** Called by {@link DockingStation} when the station joins a zone. */
    public void applyBranding(ZoneBranding branding) {
        this.branding = Objects.requireNonNull(branding);
    }
}
