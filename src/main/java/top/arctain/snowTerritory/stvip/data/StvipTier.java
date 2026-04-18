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
    private final double armorCostMultiplier;
    private final int questDailyRemoteClaimLimit;
    private final int questDailyFreeClaimLimit;
    private final int questMinDifficultyExclusive;
    private final boolean bountyPreannounce;

    public StvipTier(String id, String permission, String displayName, int priority,
                     double reinforceCostMultiplier, double fishSellMultiplier,
                     int lootExtraSlots, int lootExtraPerItemMax, double armorCostMultiplier,
                     int questDailyRemoteClaimLimit, int questDailyFreeClaimLimit, int questMinDifficultyExclusive,
                     boolean bountyPreannounce) {
        this.id = id;
        this.permission = permission;
        this.displayName = displayName;
        this.priority = priority;
        this.reinforceCostMultiplier = reinforceCostMultiplier;
        this.fishSellMultiplier = fishSellMultiplier;
        this.lootExtraSlots = lootExtraSlots;
        this.lootExtraPerItemMax = lootExtraPerItemMax;
        this.armorCostMultiplier = armorCostMultiplier;
        this.questDailyRemoteClaimLimit = questDailyRemoteClaimLimit;
        this.questDailyFreeClaimLimit = questDailyFreeClaimLimit;
        this.questMinDifficultyExclusive = questMinDifficultyExclusive;
        this.bountyPreannounce = bountyPreannounce;
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

    public double getArmorCostMultiplier() {
        return armorCostMultiplier;
    }

    public int getQuestDailyRemoteClaimLimit() {
        return questDailyRemoteClaimLimit;
    }

    public int getQuestDailyFreeClaimLimit() {
        return questDailyFreeClaimLimit;
    }

    public int getQuestMinDifficultyExclusive() {
        return questMinDifficultyExclusive;
    }

    public boolean isBountyPreannounce() {
        return bountyPreannounce;
    }
}
