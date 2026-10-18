package gocycle.common;

import java.util.regex.Pattern;

/** Minimal ANSI styling for the two terminals. Set the NO_COLOR environment variable to turn it off. */
public final class Ansi {
    public static final boolean ENABLED = System.getenv("NO_COLOR") == null;
    public static final String RESET = "\u001b[0m";
    public static final String BOLD = "\u001b[1m";
    public static final String DIM = "\u001b[2m";
    public static final String RED = "\u001b[31m";
    public static final String GREEN = "\u001b[32m";
    public static final String YELLOW = "\u001b[33m";
    public static final String CYAN = "\u001b[36m";

    private static final Pattern CODES = Pattern.compile("\u001b\\[[0-9;?]*[A-Za-z]");

    private Ansi() {
    }

    public static String paint(String text, String... styles) {
        if (!ENABLED || styles.length == 0) {
            return text;
        }
        return String.join("", styles) + text + RESET;
    }

    /** 24-bit background colour from a #RRGGBB string. */
    public static String background(String hex) {
        return rgb(48, hex);
    }

    /** 24-bit text colour from a #RRGGBB string. */
    public static String foreground(String hex) {
        return rgb(38, hex);
    }

    private static String rgb(int mode, String hex) {
        if (!ENABLED || hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) {
            return "";
        }
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return "\u001b[" + mode + ";2;" + r + ";" + g + ";" + b + "m";
    }

    /** Pads to a visible width, ignoring colour codes. */
    public static String pad(String text, int width) {
        int gap = width - CODES.matcher(text).replaceAll("").length();
        return gap > 0 ? text + " ".repeat(gap) : text;
    }
}
