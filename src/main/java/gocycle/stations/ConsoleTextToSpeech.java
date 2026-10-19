/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Speech output for the station kiosks in the terminal demo; GoCycleSystem gives every StationKiosk one of
 * these, writing to the same stream as the kiosk screen.
 */
package gocycle.stations;

import gocycle.common.Ansi;

import java.io.PrintStream;
import java.util.Locale;

/**
 * Stands in for a speech engine by printing what would be spoken, dimmed and tagged with the language code,
 * so the audio-assist path can be seen in the terminal.
 */
public class ConsoleTextToSpeech implements TextToSpeech {
    private final PrintStream out;

    public ConsoleTextToSpeech(PrintStream out) {
        this.out = out;
    }

    @Override
    public void speak(String text, Locale locale) {
        out.println(Ansi.paint("  ((( read aloud, " + locale.getLanguage() + " ))) " + text, Ansi.DIM));
    }
}
