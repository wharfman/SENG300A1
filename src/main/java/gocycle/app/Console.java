/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Application layer: the keyboard side of the terminal program. Only RiderApp uses it; kiosk and dashboard
 * output goes straight to their own PrintStream.
 */
package gocycle.app;

import gocycle.common.Ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Numbered-menu input and output for the rider terminal. Every read blocks until a line arrives, re-prompts
 * on invalid input, and throws {@link InputClosedException} at end of input. Not thread-safe; only the menu
 * thread uses it, and never while holding the {@link GoCycleSystem#atomically} lock.
 */
final class Console {
    /** Thrown when standard input closes, e.g. Ctrl+Z / Ctrl+D. */
    static final class InputClosedException extends RuntimeException {
    }

    private final BufferedReader in;
    private final PrintStream out;

    Console(InputStream in, PrintStream out) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = out;
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
            say(Ansi.paint("Please enter a number from " + min + " to " + max + ".", Ansi.YELLOW));
        }
    }

    /** Appends " (y/n): " to the prompt and re-prompts until the answer is y, yes, n or no (any case). */
    boolean confirm(String prompt) {
        while (true) {
            String line = readLine(prompt + " (y/n): ").toLowerCase();
            if (line.equals("y") || line.equals("yes")) {
                return true;
            }
            if (line.equals("n") || line.equals("no")) {
                return false;
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
        say("  0) Back");
        return readInt("> ", 0, options.size()) - 1;
    }

    void pause() {
        readLine(Ansi.paint("Press Enter to return to the menu...", Ansi.DIM));
    }
}
