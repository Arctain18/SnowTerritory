package top.arctain.snowTerritory.armor.data;

public class ArmorQuality {

    private final String id;
    private final String displayName;
    private final String suffix;
    private final int weight;
    private final double valueMultiplier;

    public ArmorQuality(String id, String displayName, String suffix, int weight, double valueMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.suffix = suffix;
        this.weight = weight;
        this.valueMultiplier = valueMultiplier;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSuffix() {
        return suffix;
    }

    public int getWeight() {
        return weight;
    }

    public double getValueMultiplier() {
        return valueMultiplier;
    }
}

