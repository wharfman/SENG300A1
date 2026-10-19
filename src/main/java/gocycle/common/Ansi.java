/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Shared terminal styling used by RiderApp, the operator dashboard and the station kiosks.
 */
package gocycle.common;

import java.util.regex.Pattern;

/**
 * Minimal ANSI styling for the terminal UI. Set the NO_COLOR environment variable to turn it off. When it is
 * off, every method returns plain text, so callers never need to check {@link #ENABLED} themselves.
 */
public final class Ansi {
    // Read once at class load, so NO_COLOR must be set before the program starts. Any value, even empty,
    // disables colour.
    public static final boolean ENABLED = System.getenv("NO_COLOR") == null;
    public static final String RESET = "\u001b[0m";
    public static final String BOLD = "\u001b[1m";
    public static final String DIM = "\u001b[2m";
    public static final String RED = "\u001b[31m";
    public static final String GREEN = "\u001b[32m";
    public static final String YELLOW = "\u001b[33m";
    public static final String CYAN = "\u001b[36m";

    // Matches any CSI escape sequence: colours, and also cursor codes such as ESC [?25l.
    private static final Pattern CODES = Pattern.compile("\u001b\\[[0-9;?]*[A-Za-z]");

    private Ansi() {
    }

    /**
     * Wraps {@code text} in the given style codes followed by {@link #RESET}. The reset clears every style,
     * so painting a substring inside an already painted string ends the outer styling too.
     */
    public static String paint(String text, String... styles) {
        if (!ENABLED || styles.length == 0) {
            return text;
        }
        return String.join("", styles) + text + RESET;
    }

    /** 24-bit background colour from a #RRGGBB string; empty (no colour) if the string is null or malformed. */
    public static String background(String hex) {
        return rgb(48, hex);
    }

    /** 24-bit text colour from a #RRGGBB string; empty (no colour) if the string is null or malformed. */
    public static String foreground(String hex) {
        return rgb(38, hex);
    }

    /** {@code mode} is the SGR selector: 38 for foreground, 48 for background. */
    private static String rgb(int mode, String hex) {
        if (!ENABLED || hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) {
            return "";
        }
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return "\u001b[" + mode + ";2;" + r + ";" + g + ";" + b + "m";
    }

    /** Pads to a visible width, ignoring colour codes. Text already that wide or wider is returned unchanged. */
    public static String pad(String text, int width) {
        int gap = width - CODES.matcher(text).replaceAll("").length();
        return gap > 0 ? text + " ".repeat(gap) : text;
    }
}
