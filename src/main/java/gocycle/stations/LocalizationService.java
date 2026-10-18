package gocycle.stations;

import java.util.List;
import java.util.Locale;

/** Translates screen text into the rider's language. */
public interface LocalizationService {
    /** Message pattern for {@code key}, with {0}, {1}, ... placeholders. */
    String translate(String key, Locale locale);

    List<Locale> supportedLocales();
}
