/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Port between StationKiosk and a speech engine, used for riders who turn on audio assist.
 */
package gocycle.stations;

import java.util.Locale;

/**
 * Reads screen text aloud. The only implementation is {@link ConsoleTextToSpeech}; a real kiosk would plug a
 * speech engine in here.
 */
public interface TextToSpeech {
    /**
     * @param text   already translated and filled in, not a message key
     * @param locale language the text is written in, so an engine can pick a matching voice
     */
    void speak(String text, Locale locale);
}
