/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Shared money helpers used by pricing, payments, trips and every screen that shows an amount.
 */
package gocycle.common;

import java.util.Locale;

/**
 * Rounding and display for dollar amounts. The system keeps money as {@code double} dollars and rounds to
 * whole cents after each calculation, rather than using {@code BigDecimal}; that is accurate enough for the
 * small fares in the demo.
 */
public final class Money {
    private Money() {
    }

    /**
     * Rounds to the nearest cent, halves rounding up. Because the input is a binary {@code double}, a value
     * that looks like an exact half cent (such as 1.005) may round down.
     */
    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    /** Canadian-dollar display with thousands separators, e.g. {@code $1,234.50}. */
    public static String format(double amount) {
        return String.format(Locale.CANADA, "$%,.2f", amount);
    }
}
