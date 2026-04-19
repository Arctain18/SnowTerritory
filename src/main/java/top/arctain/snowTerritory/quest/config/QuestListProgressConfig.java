package top.arctain.snowTerritory.quest.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import top.arctain.snowTerritory.utils.DisplayUtils;

/** 任务列表（/sn q list）中进度条显示。 */
public final class QuestListProgressConfig {

    private static final int DEFAULT_LENGTH = 50;

    private final boolean barEnabled;
    private final int length;
    private final DisplayUtils.BarStyle style;
    private final String lowColor;
    private final String midColor;
    private final String highColor;
    private final String esColor;
    private final String emptySlotColor;
    private final boolean showEsSegment;

    public QuestListProgressConfig(boolean barEnabled, int length, DisplayUtils.BarStyle style,
            String lowColor, String midColor, String highColor, String esColor, String emptySlotColor,
            boolean showEsSegment) {
        this.barEnabled = barEnabled;
        this.length = Math.max(1, length);
        this.style = style;
        this.lowColor = lowColor;
        this.midColor = midColor;
        this.highColor = highColor;
        this.esColor = esColor;
        this.emptySlotColor = emptySlotColor;
        this.showEsSegment = showEsSegment;
    }

    public static QuestListProgressConfig defaults() {
        return new QuestListProgressConfig(
                true, DEFAULT_LENGTH, DisplayUtils.BarStyle.BARS,
                "&c", "&e", "&a", "&3", "&7", true
        );
    }

    public static QuestListProgressConfig fromMainConfig(FileConfiguration main) {
        if (main == null) {
            return defaults();
        }
        ConfigurationSection sec = main.getConfigurationSection("list-progress");
        if (sec == null) {
            return defaults();
        }
        return new QuestListProgressConfig(
                sec.getBoolean("bar-enabled", true),
                sec.getInt("length", DEFAULT_LENGTH),
                parseStyle(sec.getString("style", "BARS")),
                sec.getString("low-color", "&c") != null ? sec.getString("low-color", "&c") : "&c",
                sec.getString("mid-color", "&e") != null ? sec.getString("mid-color", "&e") : "&e",
                sec.getString("high-color", "&a") != null ? sec.getString("high-color", "&a") : "&a",
                sec.getString("es-color", "&3") != null ? sec.getString("es-color", "&3") : "&3",
                sec.getString("empty-slot-color", "&7") != null ? sec.getString("empty-slot-color", "&7") : "&7",
                sec.getBoolean("show-es-segment", true)
        );
    }

    private static DisplayUtils.BarStyle parseStyle(String raw) {
        if (raw == null || raw.isBlank()) {
            return DisplayUtils.BarStyle.BARS;
        }
        return switch (raw.trim().toUpperCase()) {
            case "BLOCKS" -> DisplayUtils.BarStyle.BLOCKS;
            case "BARS" -> DisplayUtils.BarStyle.BARS;
            case "SQUARES" -> DisplayUtils.BarStyle.SQUARES;
            case "DOTS" -> DisplayUtils.BarStyle.DOTS;
            default -> DisplayUtils.BarStyle.BARS;
        };
    }

    public boolean isBarEnabled() {
        return barEnabled;
    }

    public int getLength() {
        return length;
    }

    public DisplayUtils.BarStyle getStyle() {
        return style;
    }

    public String getLowColor() {
        return lowColor;
    }

    public String getMidColor() {
        return midColor;
    }

    public String getHighColor() {
        return highColor;
    }

    public String getEsColor() {
        return esColor;
    }

    public String getEmptySlotColor() {
        return emptySlotColor;
    }

    public boolean isShowEsSegment() {
        return showEsSegment;
    }
}
