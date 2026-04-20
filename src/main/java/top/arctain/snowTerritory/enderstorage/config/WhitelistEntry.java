package top.arctain.snowTerritory.enderstorage.config;

import org.bukkit.Material;

import java.util.List;

/**
 * 白名单物品定义。
 */
public class WhitelistEntry {
    private final String key;
    private final String display;
    private final String mmoType;
    private final String mmoItemId;
    private final Material material;
    private final int defaultMax;
    private final List<String> lore;
    private final String taskCategory;
    private final String taskLevel;
    private final String taskLocation;

    public WhitelistEntry(String key, String display, String mmoType, String mmoItemId, Material material, int defaultMax) {
        this(key, display, mmoType, mmoItemId, material, defaultMax, null, "任务材料", "-", "-");
    }

    public WhitelistEntry(String key, String display, String mmoType, String mmoItemId, Material material, int defaultMax, List<String> lore) {
        this(key, display, mmoType, mmoItemId, material, defaultMax, lore, "任务材料", "-", "-");
    }

    public WhitelistEntry(String key, String display, String mmoType, String mmoItemId, Material material, int defaultMax,
                          List<String> lore, String taskCategory, String taskLevel, String taskLocation) {
        this.key = key;
        this.display = display;
        this.mmoType = mmoType;
        this.mmoItemId = mmoItemId;
        this.material = material;
        this.defaultMax = defaultMax;
        this.lore = lore;
        this.taskCategory = taskCategory;
        this.taskLevel = taskLevel;
        this.taskLocation = taskLocation;
    }

    public String getKey() {
        return key;
    }

    public String getDisplay() {
        return display;
    }

    public String getMmoType() {
        return mmoType;
    }

    public String getMmoItemId() {
        return mmoItemId;
    }

    public Material getMaterial() {
        return material;
    }

    public int getDefaultMax() {
        return defaultMax;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getTaskCategory() {
        return taskCategory;
    }

    public String getTaskLevel() {
        return taskLevel;
    }

    public String getTaskLocation() {
        return taskLocation;
    }
}

