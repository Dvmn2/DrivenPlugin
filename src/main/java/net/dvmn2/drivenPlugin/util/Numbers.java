package net.dvmn2.drivenplugin.util;

import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * Small stateless numeric helpers.
 */
public final class Numbers {

    private Numbers() {
    }

    /**
     * Formats a number with a fixed count of decimals, independent of the server locale.
     *
     * @param value    number to format
     * @param decimals digits after the decimal point
     * @return formatted number
     */
    public static String format(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /**
     * Returns true if every component of the vector is a finite number.
     *
     * @param vector vector to check
     * @return true if x, y and z are all finite
     */
    public static boolean isFinite(Vector vector) {
        return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
    }

    /**
     * Rounds a number to the nearest int, saturating at the int range.
     *
     * @param value number to round
     * @return rounded value; {@code 0} for NaN
     */
    public static int toInt(double value) {
        long rounded = Math.round(value);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rounded));
    }

    /**
     * Mathematical modulo: the result is always in {@code 0..b-1} for positive {@code b}.
     *
     * @param a dividend
     * @param b positive divisor
     * @return {@code a mod b}
     */
    public static int mod(int a, int b) {
        return (a % b + b) % b;
    }
}
