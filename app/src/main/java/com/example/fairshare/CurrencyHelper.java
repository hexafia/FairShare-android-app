package com.example.fairshare;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for standardized currency formatting across the application.
 * All monetary amounts are displayed in PHP (Philippine Pesos) with the ₱ symbol.
 */
public class CurrencyHelper {

    private static final Locale PHP_LOCALE = new Locale("en", "PH");
    private static final DecimalFormat currencyFormat;

    static {
        NumberFormat baseFormat = NumberFormat.getCurrencyInstance(PHP_LOCALE);
        if (baseFormat instanceof DecimalFormat) {
            currencyFormat = (DecimalFormat) baseFormat;
            // Ensure PHP currency symbol is used
            currencyFormat.setCurrency(java.util.Currency.getInstance("PHP"));
        } else {
            // Fallback to a custom format
            currencyFormat = new DecimalFormat("₱#,##0.00");
        }
    }

    /**
     * Formats a monetary amount as a string in PHP currency.
     * Examples:
     *   - 1250.50 → "₱1,250.50"
     *   - 100 → "₱100.00"
     *   - 0 → "₱0.00"
     *
     * @param amount the amount to format
     * @return formatted currency string with ₱ symbol
     */
    public static String format(double amount) {
        synchronized (currencyFormat) {
            return currencyFormat.format(amount);
        }
    }

    /**
     * Formats a monetary amount without decimal places (for summary/totals).
     * Examples:
     *   - 1250.50 → "₱1,251"
     *   - 100 → "₱100"
     *
     * @param amount the amount to format
     * @return formatted currency string without decimals
     */
    public static String formatWholeNumber(double amount) {
        DecimalFormat wholeFormat = new DecimalFormat("₱#,##0");
        synchronized (wholeFormat) {
            return wholeFormat.format(amount);
        }
    }

    /**
     * Formats a net balance, adding a "+" prefix for positive amounts.
     * Examples:
     *   - 100 → "+₱100.00"
     *   - -50 → "-₱50.00"
     *   - 0 → "₱0.00"
     *
     * @param amount the amount to format
     * @return formatted currency string with sign prefix for non-zero amounts
     */
    public static String formatBalance(double amount) {
        String formatted = format(Math.abs(amount));
        if (amount > 0) {
            return "+" + formatted;
        } else if (amount < 0) {
            return "-" + formatted;
        } else {
            return formatted;
        }
    }
}
