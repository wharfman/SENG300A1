package gocycle.stations;

import gocycle.common.Ansi;

import java.io.PrintStream;
import java.util.Locale;

/** Stands in for a speech engine by printing what would be spoken. */
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
