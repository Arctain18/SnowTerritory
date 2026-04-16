package top.arctain.snowTerritory.stvip.data;

/** 单档 VIP：权限、展示名、优先级与数值权益。 */
public final class StvipTier {

    private final String id;
    private final String permission;
    private final String displayName;
    private final int priority;
    private final double reinforceCostMultiplier;
    private final double fishSellMultiplier;
    private final int lootExtraSlots;
    private final int lootExtraPerItemMax;

    public StvipTier(String id, String permission, String displayName, int priority,
                     double reinforceCostMultiplier, double fishSellMultiplier,
                     int lootExtraSlots, int lootExtraPerItemMax) {
        this.id = id;
        this.permission = permission;
        this.displayName = displayName;
        this.priority = priority;
        this.reinforceCostMultiplier = reinforceCostMultiplier;
        this.fishSellMultiplier = fishSellMultiplier;
        this.lootExtraSlots = lootExtraSlots;
        this.lootExtraPerItemMax = lootExtraPerItemMax;
    }

    public String getId() {
        return id;
    }

    public String getPermission() {
        return permission;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPriority() {
        return priority;
    }

    public double getReinforceCostMultiplier() {
        return reinforceCostMultiplier;
    }

    public double getFishSellMultiplier() {
        return fishSellMultiplier;
    }

    public int getLootExtraSlots() {
        return lootExtraSlots;
    }

    public int getLootExtraPerItemMax() {
        return lootExtraPerItemMax;
    }
}
