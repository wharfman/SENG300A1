package gocycle.trips;

import java.util.Arrays;

/**
 * A trip rule stopped the request. Carries a kiosk message key so the screen can show it in the
 * rider's language.
 */
public class TripException extends RuntimeException {
    private final String messageKey;
    private final Object[] args;

    public TripException(String messageKey, Object... args) {
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
