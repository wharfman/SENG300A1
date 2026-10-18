package gocycle.stations;

import java.util.Locale;

/** Reads screen text aloud. */
public interface TextToSpeech {
    void speak(String text, Locale locale);
}
