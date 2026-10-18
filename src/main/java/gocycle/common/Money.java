package gocycle.common;

import java.util.Locale;

public final class Money {
    private Money() {
    }

    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public static String format(double amount) {
        return String.format(Locale.CANADA, "$%,.2f", amount);
    }
}
