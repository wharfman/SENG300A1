/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Translation port used by StationKiosk to render message keys (including the keys carried by TripException)
 * and by RiderApp to list the languages a rider can choose.
 */
package gocycle.stations;

import java.util.List;
import java.util.Locale;

/** Translates screen text into the rider's language. */
public interface LocalizationService {
    /**
     * Message pattern for {@code key}, with {0}, {1}, ... placeholders. The caller fills in the placeholders
     * (see {@link StationKiosk#text}).
     */
    String translate(String key, Locale locale);

    /** Languages offered to riders in the app's language menu. */
    List<Locale> supportedLocales();
}
