/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Tests for the read-aloud setting, with a fake speech engine in place of the operating system's.
 */
package gocycle.app;

import gocycle.stations.InMemoryLocalizationService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers that menu titles are spoken only while the setting is on, in the current language, and that the
 * setting cannot be turned on without a speech engine.
 */
class ReadAloudTest {
    /** Records what it was asked to say as "language: text". */
    private static final class FakeSpeech implements TextToSpeech {
        final List<String> spoken = new ArrayList<>();

        @Override
        public void speak(String text, Locale language) {
            spoken.add(language.getLanguage() + ": " + text);
        }

        @Override
        public void close() {
        }
    }

    private final InMemoryLocalizationService l10n = new InMemoryLocalizationService();
    private final FakeSpeech speech = new FakeSpeech();
    private final ReadAloud readAloud = new ReadAloud(Optional.of(speech), l10n);

    /** A console whose rider answers "0" (Back) to every menu. */
    private Console console() {
        byte[] input = "0\n0\n0\n".getBytes(StandardCharsets.UTF_8);
        return new Console(new ByteArrayInputStream(input), new PrintStream(OutputStream.nullOutputStream()), l10n,
                readAloud);
    }

    @Test
    void menuTitlesAreSilentUntilTheSettingIsTurnedOn() {
        Console console = console();

        console.choose("Which station are you at?", List.of("City Hall"));
        assertTrue(speech.spoken.isEmpty());

        readAloud.setOn(true);
        console.choose("Which station are you at?", List.of("City Hall"));
        assertEquals(List.of("en: Which station are you at?"), speech.spoken);
    }

    @Test
    void titlesAreSpokenInTheCurrentLanguage() {
        readAloud.setOn(true);
        l10n.setLanguage(Locale.FRENCH);

        console().choose(l10n.text("stations.ask.start"), List.of("City Hall"));

        assertEquals(List.of("fr: À quelle station êtes-vous ?"), speech.spoken);
    }

    @Test
    void cannotBeTurnedOnWithoutASpeechEngine() {
        ReadAloud none = new ReadAloud(Optional.empty(), l10n);

        none.setOn(true);
        none.header("Settings");

        assertFalse(none.isAvailable());
        assertFalse(none.isOn());
    }
}
