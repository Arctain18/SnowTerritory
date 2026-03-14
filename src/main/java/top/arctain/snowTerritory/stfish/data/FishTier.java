package top.arctain.snowTerritory.stfish.data;

/** 鱼类品质枚举。 */
public enum FishTier {
    COMMON("普通", 34, "&f"),
    RARE("稀有", 21, "&a"),
    EPIC("史诗", 18, "&d"),
    LEGENDARY("传说", 15, "&b"),
    STORM("风暴", 23, "&1&l"),
    WORLD("世界", 14, null);

    private final String displayName;
    private final int fishCount;
    private final String nameColor;

    FishTier(String displayName, int fishCount, String nameColor) {
        this.displayName = displayName;
        this.fishCount = fishCount;
        this.nameColor = nameColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getFishCount() {
        return fishCount;
    }

    /** 同品级统一名称颜色，世界级返回 null 表示使用配置中的渐变。 */
    public String getNameColor() {
        return nameColor;
    }

    public static FishTier fromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (FishTier t : values()) {
            if (t.displayName.equals(displayName)) return t;
        }
        return null;
    }
}
