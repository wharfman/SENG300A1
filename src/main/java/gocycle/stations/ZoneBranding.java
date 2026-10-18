package gocycle.stations;

import java.util.Properties;

/** A zone's brand colours and logo, loaded from configuration. */
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

    /** Reads {@code primaryColour}, {@code secondaryColour} and {@code logoPath}; missing keys use the defaults. */
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
