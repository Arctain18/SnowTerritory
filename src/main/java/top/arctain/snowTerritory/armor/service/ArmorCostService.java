package top.arctain.snowTerritory.armor.service;

import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.armor.data.ArmorGenerationCost;
import top.arctain.snowTerritory.armor.data.ArmorProfileBaseCost;
import top.arctain.snowTerritory.armor.data.ArmorSetDefinition;
import top.arctain.snowTerritory.armor.config.ArmorConfigManager;
import top.arctain.snowTerritory.reinforce.service.EconomyService;
import top.arctain.snowTerritory.reinforce.service.MMOCoreService;
import top.arctain.snowTerritory.utils.CoinsEngineUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

/** 计算制式防具生成费用与余额校验。 */
public class ArmorCostService {

    private final MMOCoreService mmocoreService;
    private final EconomyService economyService;
    private final String qpCurrencyId;
    private final ArmorConfigManager configManager;
    private final Main plugin;

    public ArmorCostService(Main plugin, ArmorConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.mmocoreService = new MMOCoreService();
        this.economyService = new EconomyService();
        this.qpCurrencyId = configManager.getQpCurrencyId();
        if (CoinsEngineUtils.isPluginEnabled() && !CoinsEngineUtils.hasCurrency(this.qpCurrencyId)) {
            MessageUtils.logWarning("CoinsEngine 未找到货币: " + this.qpCurrencyId + "（可改为 starember 等）");
        }
    }

    public CostPreview buildPreview(Player player, ArmorSetDefinition set, String profile) {
        int level = mmocoreService.getPlayerLevel(player);
        String className = mmocoreService.getClassName(player);
        ArmorGenerationCost rule = set.getGenerationCost();
        int levelDelta = Math.max(0, level - rule.getLevelThreshold());
        ArmorProfileBaseCost baseOverride = configManager.getGenerationProfileBaseCost(profile);
        double slBase = baseOverride != null ? baseOverride.slBase() : rule.getSlBase();
        double qpBase = baseOverride != null ? baseOverride.qpBase() : rule.getQpBase();

        double rawSl = calcRaw(slBase, rule.getSlPerLevel(), levelDelta, rule.getMode());
        double rawQp = calcRaw(qpBase, rule.getQpPerLevel(), levelDelta, rule.getMode());

        VipDiscount vip = resolveVipDiscount(player);
        double finalSl = rawSl * vip.multiplier();
        double finalQp = rawQp * vip.multiplier();

        double slBalance = economyService.isEnabled() ? economyService.getBalance(player) : 0.0;
        boolean qpEnabled = CoinsEngineUtils.hasCurrency(qpCurrencyId);
        double qpBalance = qpEnabled ? CoinsEngineUtils.getBalance(player, qpCurrencyId) : 0.0;

        double slShortage = Math.max(0.0, finalSl - slBalance);
        double qpShortage = Math.max(0.0, finalQp - qpBalance);
        boolean enough = economyService.isEnabled() && qpEnabled && slShortage <= 0.0001 && qpShortage <= 0.0001;

        return new CostPreview(
                player.getName(),
                level,
                className == null ? "无" : className,
                rawSl,
                rawQp,
                finalSl,
                finalQp,
                slBalance,
                qpBalance,
                slShortage,
                qpShortage,
                vip,
                enough,
                economyService.isEnabled(),
                qpEnabled
        );
    }

    public boolean tryCharge(Player player, CostPreview preview) {
        if (!preview.enough()) {
            return false;
        }
        if (!economyService.isEnabled() || !CoinsEngineUtils.hasCurrency(qpCurrencyId)) {
            return false;
        }
        economyService.withdraw(player, preview.finalSl());
        return CoinsEngineUtils.removeBalance(player, qpCurrencyId, preview.finalQp());
    }

    private static double calcRaw(double base, double perLevel, int levelDelta, ArmorGenerationCost.Mode mode) {
        if (levelDelta <= 0) {
            return Math.max(0.0, base);
        }
        if (mode == ArmorGenerationCost.Mode.PERCENT) {
            return Math.max(0.0, base * (1.0 + perLevel * levelDelta));
        }
        return Math.max(0.0, base + perLevel * levelDelta);
    }

    private VipDiscount resolveVipDiscount(Player player) {
        if (plugin.getStvipService() != null) {
            var tier = plugin.getStvipService().resolveTier(player);
            if (tier.isPresent()) {
                return new VipDiscount(tier.get().getDisplayName(), plugin.getStvipService().getArmorCostMultiplier(player));
            }
        }
        return new VipDiscount("", 1.0);
    }

    public record VipDiscount(String name, double multiplier) {
        public int percentOff() {
            return (int) Math.round((1.0 - multiplier) * 100.0);
        }

        public boolean active() {
            return multiplier < 0.9999;
        }
    }

    public record CostPreview(
            String playerName,
            int level,
            String className,
            double rawSl,
            double rawQp,
            double finalSl,
            double finalQp,
            double slBalance,
            double qpBalance,
            double slShortage,
            double qpShortage,
            VipDiscount vip,
            boolean enough,
            boolean economyEnabled,
            boolean pointsEnabled
    ) {
    }
}
