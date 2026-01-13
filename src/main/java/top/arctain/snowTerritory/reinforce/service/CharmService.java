package top.arctain.snowTerritory.reinforce.service;

import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.reinforce.utils.ReinforceUtils;

/**
 * 负责保护符、强化符等符文相关的判定逻辑
 */
public class CharmService {

    private final ReinforceConfigManager config;

    public CharmService(ReinforceConfigManager config) {
        this.config = config;
    }

    /**
     * 符文校验结果，复用保护符和强化符的判定逻辑
     */
    public static class CharmInfo {
        public final boolean valid;
        public final boolean expired;
        public final int maxLevel;
        public final int bonus; // 仅强化符使用，保护符为0

        public CharmInfo(boolean valid, boolean expired, int maxLevel, int bonus) {
            this.valid = valid;
            this.expired = expired;
            this.maxLevel = maxLevel;
            this.bonus = bonus;
        }

        public static CharmInfo invalid() {
            return new CharmInfo(false, false, -1, 0);
        }
    }

    public CharmInfo evaluateProtectCharm(ItemStack protectCharm, int nextLevel) {
        if (protectCharm == null) return CharmInfo.invalid();
        if (!ReinforceUtils.isPreservationToken(protectCharm, config.getPreservationTokenType())) return CharmInfo.invalid();

        int maxLevel = ReinforceUtils.parseMaxLevelFromLore(protectCharm);
        boolean valid = maxLevel >= 0 && nextLevel <= maxLevel;
        boolean expired = maxLevel >= 0 && nextLevel > maxLevel;
        return new CharmInfo(valid, expired, maxLevel, 0);
    }

    public CharmInfo evaluateEnhanceCharm(ItemStack enhanceCharm, int nextLevel) {
        if (enhanceCharm == null) return CharmInfo.invalid();
        if (!ReinforceUtils.isUpgradeToken(enhanceCharm, config.getUpgradeTokenType())) return CharmInfo.invalid();

        int maxLevel = ReinforceUtils.parseMaxLevelFromLore(enhanceCharm);
        int bonus = ReinforceUtils.parseProbabilityBoostFromLore(enhanceCharm);
        boolean valid = maxLevel >= 0 && nextLevel <= maxLevel && bonus > 0;
        boolean expired = maxLevel >= 0 && nextLevel > maxLevel;
        return new CharmInfo(valid, expired, maxLevel, bonus);
    }
}

