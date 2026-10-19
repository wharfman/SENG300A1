/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Error type of the trips package: thrown by {@code TripService} and caught by {@code RiderApp}, which
 * passes the key and arguments to {@code StationKiosk.announce} for translation.
 */
package gocycle.trips;

import java.util.Arrays;

/**
 * A trip rule stopped the request. Carries a kiosk message key so the screen can show it in the
 * rider's language.
 */
public class TripException extends RuntimeException {
    private final String messageKey;
    private final Object[] args;

    /**
     * @param messageKey a key defined in every language of {@code InMemoryLocalizationService}
     * @param args       values substituted into the translated message, in order
     */
    public TripException(String messageKey, Object... args) {
        // The English text isn't known here, so the exception message (for logs and stack traces) is
        // just the key and its arguments.
        super(messageKey + " " + Arrays.toString(args));
        this.messageKey = messageKey;
        this.args = args.clone();
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
