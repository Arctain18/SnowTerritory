package top.arctain.snowTerritory.life.data;

public enum LifeSkillType {
    GATHERING("gathering", "采集"),
    FISHING("fishing", "垂钓"),
    EXPLORATION("exploration", "探索");

    private final String key;
    private final String displayName;

    LifeSkillType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }
}
