/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Translation port shared by the whole terminal program: GoCycleSystem owns one instance, and the station
 * kiosks, RiderApp, the console and email receipts all render their text through it.
 */
package gocycle.stations;

import gocycle.common.Message;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Translates screen text into the system-wide language. The language is a global setting, not a rider
 * preference: changing it switches every screen at once, whoever is signed in.
 *
 * <p>Enum constants are translated by the key {@code <SimpleClassName>.<CONSTANT>}, e.g.
 * {@code VehicleType.E_BIKE}.
 */
public interface LocalizationService {
    /**
     * Message pattern for {@code key}, with {0}, {1}, ... placeholders. The caller fills in the placeholders
     * (see {@link #text}).
     */
    String translate(String key, Locale locale);

    /** Languages offered in the app's language menu. */
    List<Locale> supportedLocales();

    Locale getLanguage();

    /** @throws IllegalArgumentException if {@code language} is not one of {@link #supportedLocales()} */
    void setLanguage(Locale language);

    /**
     * Translates {@code key} into the current language and fills {@code {i}} with {@code args[i]}, each
     * rendered by {@link #render}. Placeholders without a matching argument are left as-is.
     */
    default String text(String key, Object... args) {
        String text = translate(key, getLanguage());
        // A plain replace rather than MessageFormat, which would treat the apostrophes in French text such as
        // "jusqu'au" as quote marks and would re-format numeric arguments.
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", render(args[i]));
        }
        return text;
    }

    /**
     * Display form of one value in the current language: messages and enum constants are translated,
     * collections become a comma-separated list, and anything else is shown with {@code String.valueOf}.
     */
    default String render(Object value) {
        if (value instanceof Message message) {
            return text(message.key(), message.args());
        }
        if (value instanceof Enum<?> constant) {
            return text(constant.getDeclaringClass().getSimpleName() + "." + constant.name());
        }
        if (value instanceof Collection<?> values) {
            return values.stream().map(this::render).collect(Collectors.joining(", "));
        }
        return String.valueOf(value);
    }
}
