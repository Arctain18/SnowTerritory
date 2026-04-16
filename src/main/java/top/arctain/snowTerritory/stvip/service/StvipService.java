package top.arctain.snowTerritory.stvip.service;

import org.bukkit.entity.Player;
import top.arctain.snowTerritory.stvip.config.StvipConfigManager;
import top.arctain.snowTerritory.stvip.data.StvipTier;

import java.util.Optional;

/** 按权限解析当前 VIP 档并提供各模块使用的数值。 */
public class StvipService {

    private final StvipConfigManager configManager;

    public StvipService(StvipConfigManager configManager) {
        this.configManager = configManager;
    }

    public Optional<StvipTier> resolveTier(Player player) {
        return configManager.resolveTier(player);
    }

    public double getReinforceCostMultiplier(Player player) {
        return resolveTier(player).map(StvipTier::getReinforceCostMultiplier).orElse(1.0);
    }

    public double getFishSellMultiplier(Player player) {
        return resolveTier(player).map(StvipTier::getFishSellMultiplier).orElse(1.0);
    }

    public int getLootExtraSlots(Player player) {
        return resolveTier(player).map(StvipTier::getLootExtraSlots).orElse(0);
    }

    public int getLootExtraPerItemMax(Player player) {
        return resolveTier(player).map(StvipTier::getLootExtraPerItemMax).orElse(0);
    }

    public boolean hasAnyVip(Player player) {
        return resolveTier(player).isPresent();
    }
}
