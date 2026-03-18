package top.arctain.snowTerritory.armor.data;

public class ArmorStatRange {

    private final double minMultiplier;
    private final double maxMultiplier;

    public ArmorStatRange(double minMultiplier, double maxMultiplier) {
        this.minMultiplier = minMultiplier;
        this.maxMultiplier = maxMultiplier;
    }

    public double getMinMultiplier() {
        return minMultiplier;
    }

    public double getMaxMultiplier() {
        return maxMultiplier;
    }
}

