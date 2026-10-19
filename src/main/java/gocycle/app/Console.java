/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Application layer: the keyboard side of the terminal program. Only RiderApp uses it; kiosk and dashboard
 * output goes straight to their own PrintStream.
 */
package gocycle.app;

import gocycle.common.Ansi;
import gocycle.stations.LocalizationService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

/**
 * Numbered-menu input and output for the rider terminal. Every read blocks until a line arrives, re-prompts
 * on invalid input, and throws {@link InputClosedException} at end of input. Its own fixed text ("Back",
 * hints) follows the system language. Not thread-safe; only the menu
 * thread uses it, and never while holding the {@link GoCycleSystem#atomically} lock.
 */
final class Console {
    /** Thrown when standard input closes, e.g. Ctrl+Z / Ctrl+D. */
    static final class InputClosedException extends RuntimeException {
    }

    private final BufferedReader in;
    private final PrintStream out;
    private final LocalizationService l10n;

    Console(InputStream in, PrintStream out, LocalizationService l10n) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = out;
        this.l10n = l10n;
    }

    void say(String text) {
        out.println(text);
    }

    /**
     * Prints {@code prompt} without a newline and returns the next line with surrounding whitespace removed.
     *
     * @throws InputClosedException at end of input
     * @throws UncheckedIOException if reading fails
     */
    String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                throw new InputClosedException();
            }
            return line.strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String readNonBlank(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (!line.isEmpty()) {
                return line;
            }
        }
    }

    /** Returns {@code fallback} when the rider just presses Enter. */
    String readOrDefault(String prompt, String fallback) {
        String line = readLine(prompt);
        return line.isEmpty() ? fallback : line;
    }

    /** Re-prompts until the rider enters a whole number from {@code min} to {@code max} inclusive. */
    int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the hint
            }
            say(Ansi.paint(l10n.text("console.number.range", min, max), Ansi.YELLOW));
        }
    }

    /**
     * Appends " (y/n): " (" (o/n) : " in French) to the prompt and re-prompts until the answer is yes or no.
     * English and French answers are both accepted whatever the language: y, yes, o, oui, n, no or non.
     */
    boolean confirm(String prompt) {
        while (true) {
            String line = readLine(prompt + l10n.text("console.yes.no")).toLowerCase(Locale.ROOT);
            switch (line) {
                case "y", "yes", "o", "oui" -> {
                    return true;
                }
                case "n", "no", "non" -> {
                    return false;
                }
                default -> {
                }
            }
        }
    }

    /** Shows numbered options plus "0) Back"; returns the chosen index, or -1 for back. */
    int choose(String title, List<String> options) {
        say("");
        say(Ansi.paint(title, Ansi.BOLD));
        for (int i = 0; i < options.size(); i++) {
            say("  " + (i + 1) + ") " + options.get(i));
        }
        say("  0) " + l10n.text("console.back"));
        return readInt("> ", 0, options.size()) - 1;
    }

    void pause() {
        readLine(Ansi.paint(l10n.text("console.pause"), Ansi.DIM));
    }
}
