/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Per-zone look of the station kiosks. GoCycleSystem reads it from config/zones.properties for each Zone,
 * and StationKiosk (plus RiderApp's station list) uses its colours.
 */
package gocycle.stations;

import java.util.Properties;

/**
 * A zone's brand colours and logo, loaded from configuration. Immutable. Colours are {@code #RRGGBB}
 * strings; they are not validated here, and {@code Ansi} draws an invalid one as no colour. The logo path is
 * only printed as text on the kiosk header; no image is loaded.
 */
public class ZoneBranding {
    public static final ZoneBranding DEFAULT = new ZoneBranding("#1B5E20", "#A5D6A7", "branding/gocycle-logo.png");

    private final String primaryColour;
    private final String secondaryColour;
    private final String logoPath;

    public ZoneBranding(String primaryColour, String secondaryColour, String logoPath) {
        this.primaryColour = primaryColour;
        this.secondaryColour = secondaryColour;
        this.logoPath = logoPath;
    }

    /**
     * Reads {@code primaryColour}, {@code secondaryColour} and {@code logoPath}; missing keys use the defaults.
     * The keys are unprefixed: the caller has already stripped the {@code <zoneId>.} prefix used in the file.
     */
    public static ZoneBranding fromConfig(Properties cfg) {
        return new ZoneBranding(
                cfg.getProperty("primaryColour", DEFAULT.primaryColour).strip(),
                cfg.getProperty("secondaryColour", DEFAULT.secondaryColour).strip(),
                cfg.getProperty("logoPath", DEFAULT.logoPath).strip());
    }

    public String getPrimaryColour() {
        return primaryColour;
    }

    public String getSecondaryColour() {
        return secondaryColour;
    }

    public String getLogoPath() {
        return logoPath;
    }
}
