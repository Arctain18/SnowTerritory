package top.arctain.snowTerritory.armor.data;

/** 套装生成费用配置。 */
public class ArmorGenerationCost {

    public enum Mode {
        ADDITIVE,
        PERCENT
    }

    private final double slBase;
    private final double qpBase;
    private final int levelThreshold;
    private final double slPerLevel;
    private final double qpPerLevel;
    private final Mode mode;

    public ArmorGenerationCost(double slBase, double qpBase, int levelThreshold,
                               double slPerLevel, double qpPerLevel, Mode mode) {
        this.slBase = slBase;
        this.qpBase = qpBase;
        this.levelThreshold = levelThreshold;
        this.slPerLevel = slPerLevel;
        this.qpPerLevel = qpPerLevel;
        this.mode = mode != null ? mode : Mode.ADDITIVE;
    }

    public double getSlBase() {
        return slBase;
    }

    public double getQpBase() {
        return qpBase;
    }

    public int getLevelThreshold() {
        return levelThreshold;
    }

    public double getSlPerLevel() {
        return slPerLevel;
    }

    public double getQpPerLevel() {
        return qpPerLevel;
    }

    public Mode getMode() {
        return mode;
    }
}
