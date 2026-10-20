/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Application layer: the speech port behind the read-aloud setting. SystemTextToSpeech is the real one;
 * tests plug in a fake.
 */
package gocycle.app;

import java.util.Locale;

/** Reads text aloud without blocking the caller. */
interface TextToSpeech {
    /** Starts speaking {@code text}, cutting off anything still being spoken. */
    void speak(String text, Locale language);

    /** Stops speaking and releases the engine. Safe to call more than once. */
    void close();
}
