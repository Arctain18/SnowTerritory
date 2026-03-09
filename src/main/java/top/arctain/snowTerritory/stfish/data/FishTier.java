package top.arctain.snowTerritory.stfish.data;

/** 鱼类品质枚举。 */
public enum FishTier {
    COMMON("普通", 34),
    RARE("稀有", 21),
    EPIC("史诗", 18),
    LEGENDARY("传说", 15),
    STORM("风暴", 23),
    WORLD("世界", 14);

    private final String displayName;
    private final int fishCount;

    FishTier(String displayName, int fishCount) {
        this.displayName = displayName;
        this.fishCount = fishCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getFishCount() {
        return fishCount;
    }
}
