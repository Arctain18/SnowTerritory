package top.arctain.snowTerritory.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** 通用数字单位格式化工具。 */
public final class NumberFormatUtils {

    private static final double COMPACT_THRESHOLD = 10_000D;
    private static final double UNIT_BASE = 1_000D;
    private static final String[] UNITS = {"k", "M", "B", "T", "Q"};
    private static final DecimalFormat PLAIN_FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat COMPACT_FORMAT = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US));

    private NumberFormatUtils() {
    }

    public static String formatWithUnit(long number) {
        return formatWithUnit((double) number);
    }

    /**
     * 小于 5 位（绝对值 &lt; 10000）保持原样，大于等于 5 位按 k/M/B... 缩写。
     * 例如：11250 -> 11.2k
     */
    public static String formatWithUnit(double number) {
        double absolute = Math.abs(number);
        if (absolute < COMPACT_THRESHOLD) {
            return PLAIN_FORMAT.format(number);
        }

        int unitIndex = -1;
        double compactValue = number;
        while (Math.abs(compactValue) >= UNIT_BASE && unitIndex < UNITS.length - 1) {
            compactValue /= UNIT_BASE;
            unitIndex++;
        }
        return COMPACT_FORMAT.format(compactValue) + UNITS[unitIndex];
    }
}
