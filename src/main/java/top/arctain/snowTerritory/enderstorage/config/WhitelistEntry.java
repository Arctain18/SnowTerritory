package top.arctain.snowTerritory.enderstorage.config;

import org.bukkit.Material;

/**
 * 白名单物品定义。
 */
public class WhitelistEntry {
    private final String key;
    private final String display;
    private final String mmoItemId;
    private final Material material;
    private final int defaultMax;

    public WhitelistEntry(String key, String display, String mmoItemId, Material material, int defaultMax) {
        this.key = key;
        this.display = display;
        this.mmoItemId = mmoItemId;
        this.material = material;
        this.defaultMax = defaultMax;
    }

    public String getKey() {
        return key;
    }

    public String getDisplay() {
        return display;
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
}

