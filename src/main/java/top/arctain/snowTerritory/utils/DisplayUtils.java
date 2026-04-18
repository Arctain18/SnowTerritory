package top.arctain.snowTerritory.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class DisplayUtils {

    private DisplayUtils() {}

    // 解析 "(3/87)"、"3/87"、"  ( 3 / 87 )  " 之类
    private static final Pattern FRACTION = Pattern.compile("\\s*\\(?\\s*(\\d+)\\s*/\\s*(\\d+)\\s*\\)?\\s*");

    /** 进度条样式 */
    public enum BarStyle {
        BLOCKS("█", "░"),
        BARS("|", "|"),
        SQUARES("■", "□"),
        DOTS("●", "○");

        private final String full;
        private final String empty;

        BarStyle(String full, String empty) {
            this.full = full;
            this.empty = empty;
        }
    }

    /**
     * 将 "(3/87)" 转进度条
     */
    public static String progressBar(BarStyle style,
                                     String lowColor, String midColor, String highColor,
                                     String fractionText,
                                     int length) {
        int[] frac = parseFraction(fractionText);
        return progressBar(style, lowColor, midColor, highColor, frac[0], frac[1], length);
    }

    /**
     * 将 current/total 转进度条
     */
    public static String progressBar(BarStyle style,
                                     String lowColor, String midColor, String highColor,
                                     int current, int total,
                                     int length) {
        return progressBar(style, lowColor, midColor, highColor, current, total, length, "&7");
    }

    public static String progressBar(BarStyle style,
                                     String lowColor, String midColor, String highColor,
                                     int current, int total,
                                     int length, String emptySlotColor) {
        if (length <= 0) throw new IllegalArgumentException("length must be > 0");
        if (total <= 0) total = 1;
        current = Math.max(0, Math.min(current, total));

        double p = current / (double) total; // 0..1
        int filled = (int) Math.round(p * length);
        filled = Math.max(0, Math.min(filled, length));

        String color = pickColor(p, lowColor, midColor, highColor);
        String empty = emptySlotColor == null || emptySlotColor.isEmpty() ? "&7" : emptySlotColor;

        StringBuilder sb = new StringBuilder();
        sb.append(ColorUtils.colorize(color));
        sb.append(repeat(style.full, filled));
        sb.append(ColorUtils.colorize(empty));
        sb.append(repeat(style.empty, length - filled));
        sb.append(ColorUtils.colorize("&r")); // reset
        return sb.toString();
    }

    /**
     * 单条进度条内两段：已提交（与 {@link #progressBar} 同色阶）+ ES 可补部分（独立颜色），其余为空。
     *
     * @param current  已提交数量
     * @param esUsable 仓库中可用于本任务补满缺口的数量（已在外层 clamp 为 min(ES, need)）
     */
    public static String progressBarWithEsStorage(BarStyle style,
            String lowColor, String midColor, String highColor, String esColor,
            int current, int esUsable, int total, int length) {
        return progressBarWithEsStorage(style, lowColor, midColor, highColor, esColor,
                current, esUsable, total, length, "&7");
    }

    public static String progressBarWithEsStorage(BarStyle style,
            String lowColor, String midColor, String highColor, String esColor,
            int current, int esUsable, int total, int length, String emptySlotColor) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0");
        }
        if (total <= 0) {
            total = 1;
        }
        int safeCurrent = Math.max(0, Math.min(current, total));
        int need = Math.max(0, total - safeCurrent);
        int safeEs = Math.max(0, Math.min(esUsable, need));

        int wSubmit = (int) Math.round(safeCurrent / (double) total * length);
        int wEs = (int) Math.round(safeEs / (double) total * length);
        wSubmit = Math.max(0, Math.min(wSubmit, length));
        wEs = Math.max(0, Math.min(wEs, length - wSubmit));
        int wEmpty = length - wSubmit - wEs;

        String empty = emptySlotColor == null || emptySlotColor.isEmpty() ? "&7" : emptySlotColor;
        String submitColor = pickColor(safeCurrent / (double) total, lowColor, midColor, highColor);
        StringBuilder sb = new StringBuilder();
        sb.append(ColorUtils.colorize(submitColor));
        sb.append(repeat(style.full, wSubmit));
        sb.append(ColorUtils.colorize(esColor));
        sb.append(repeat(style.full, wEs));
        sb.append(ColorUtils.colorize(empty));
        sb.append(repeat(style.empty, wEmpty));
        sb.append(ColorUtils.colorize("&r"));
        return sb.toString();
    }

    private static String pickColor(double p, String low, String mid, String high) {
        if (p < 1.0 / 3.0) return low;
        if (p < 2.0 / 3.0) return mid;
        return high;
    }

    private static int[] parseFraction(String s) {
        if (s == null) throw new IllegalArgumentException("fractionText is null");
        Matcher m = FRACTION.matcher(s);
        if (!m.matches()) throw new IllegalArgumentException("Invalid fraction: " + s);
        int a = Integer.parseInt(m.group(1));
        int b = Integer.parseInt(m.group(2));
        return new int[]{a, b};
    }

    private static String repeat(String unit, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(unit.length() * n);
        for (int i = 0; i < n; i++) sb.append(unit);
        return sb.toString();
    }

    /**
     * 将毫秒数转换为 HH:MM:SS 格式
     * @param milliseconds 毫秒数
     * @return 格式化的时间字符串，例如 "01:23:45"
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds < 0) milliseconds = 0;
        
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}