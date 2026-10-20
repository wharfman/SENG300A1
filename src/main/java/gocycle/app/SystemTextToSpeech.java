/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Application layer: real speech for the read-aloud setting, using the operating system's own engine so
 * nothing extra has to be installed (System.Speech on Windows, say on macOS, espeak or spd-say on Linux).
 */
package gocycle.app;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Speaks through an external program. On Windows one long-lived PowerShell process does the talking,
 * because starting PowerShell takes seconds; elsewhere a short command is run per utterance. Either way
 * {@link #speak} returns at once and a new utterance cuts off the previous one.
 *
 * <p>Speech is a convenience: if the engine fails, it is shut down quietly and the menus carry on silently.
 * Not thread-safe; only the menu thread uses it.
 */
final class SystemTextToSpeech implements TextToSpeech {
    // Reads one line per utterance, "<language> <Base64 of the UTF-8 text>". Plain ASCII lines avoid console
    // code-page trouble with accented text. A voice for the language is used when one is installed; otherwise
    // the default voice reads it.
    private static final String WINDOWS_SCRIPT = String.join("\n",
            "Add-Type -AssemblyName System.Speech",
            "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer",
            "$voices = @($s.GetInstalledVoices() | Where-Object { $_.Enabled })",
            "while (($line = [Console]::In.ReadLine()) -ne $null) {",
            "  $lang, $b64 = $line.Split(' ', 2)",
            "  $s.SpeakAsyncCancelAll()",
            "  $voice = $voices | Where-Object { $_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq $lang } | Select-Object -First 1",
            "  try { if ($voice) { $s.SelectVoice($voice.VoiceInfo.Name) } } catch { }",
            "  $s.SpeakAsync([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))) | Out-Null",
            "}");

    // Empty for the Windows engine; otherwise the program run once per utterance.
    private final String program;
    // The program's language option ("-v", "-l"), or null when it has none.
    private final String languageOption;
    private Process process;
    private Writer windowsInput;

    private SystemTextToSpeech(String program, String languageOption) {
        this.program = program;
        this.languageOption = languageOption;
    }

    /** The speech engine for this computer, or empty when none is available. */
    static Optional<TextToSpeech> detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) {
            return Optional.of(new SystemTextToSpeech("", null));
        }
        if (os.contains("mac") && onPath("say")) {
            return Optional.of(new SystemTextToSpeech("say", null));
        }
        for (String[] candidate : new String[][] {{"espeak-ng", "-v"}, {"espeak", "-v"}, {"spd-say", "-l"}}) {
            if (onPath(candidate[0])) {
                return Optional.of(new SystemTextToSpeech(candidate[0], candidate[1]));
            }
        }
        return Optional.empty();
    }

    @Override
    public void speak(String text, Locale language) {
        try {
            if (program.isEmpty()) {
                speakOnWindows(text, language);
            } else {
                speakWithProgram(text, language);
            }
        } catch (IOException e) {
            close();
        }
    }

    private void speakOnWindows(String text, Locale language) throws IOException {
        if (process == null || !process.isAlive()) {
            // -EncodedCommand takes the script as Base64 of UTF-16LE, which sidesteps command-line quoting.
            String script = Base64.getEncoder().encodeToString(WINDOWS_SCRIPT.getBytes(StandardCharsets.UTF_16LE));
            process = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", script)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            windowsInput = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.US_ASCII);
        }
        windowsInput.write(language.getLanguage() + " "
                + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + "\n");
        windowsInput.flush();
    }

    private void speakWithProgram(String text, Locale language) throws IOException {
        if (process != null) {
            process.destroy();
        }
        List<String> command = new ArrayList<>(List.of(program));
        if (languageOption != null) {
            command.add(languageOption);
            command.add(language.getLanguage());
        }
        command.add(text);
        process = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    @Override
    public void close() {
        if (windowsInput != null) {
            try {
                windowsInput.close();
            } catch (IOException ignored) {
                // the process is destroyed below either way
            }
            windowsInput = null;
        }
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    private static boolean onPath(String program) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir, program).canExecute()) {
                return true;
            }
        }
        return false;
    }
}
