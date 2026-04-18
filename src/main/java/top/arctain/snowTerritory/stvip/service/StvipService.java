package top.arctain.snowTerritory.stvip.service;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stvip.config.StvipConfigManager;
import top.arctain.snowTerritory.stvip.data.StvipTier;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** 按权限解析当前 VIP 档并提供各模块使用的数值。 */
public class StvipService {

    private final Main plugin;
    private final StvipConfigManager configManager;

    public StvipService(Main plugin, StvipConfigManager configManager) {
        this.plugin = plugin;
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

    public double getArmorCostMultiplier(Player player) {
        return resolveTier(player).map(StvipTier::getArmorCostMultiplier).orElse(1.0);
    }

    public int getQuestDailyRemoteClaimLimit(Player player) {
        return resolveTier(player).map(StvipTier::getQuestDailyRemoteClaimLimit).orElse(0);
    }

    public int getQuestDailyFreeClaimLimit(Player player) {
        return resolveTier(player).map(StvipTier::getQuestDailyFreeClaimLimit).orElse(0);
    }

    public int getQuestMinDifficultyExclusive(Player player) {
        return resolveTier(player).map(StvipTier::getQuestMinDifficultyExclusive).orElse(0);
    }

    public boolean canReceiveBountyPreannounce(Player player) {
        return resolveTier(player).map(StvipTier::isBountyPreannounce).orElse(false);
    }

    /**
     * 读取当前档位权限的剩余天数（仅 LuckPerms 临时权限可用）。
     */
    public OptionalInt getRemainingVipDays(Player player) {
        Optional<StvipTier> tierOpt = resolveTier(player);
        if (tierOpt.isEmpty()) {
            return OptionalInt.empty();
        }
        Instant expiry = resolvePermissionExpiry(player, tierOpt.get().getPermission());
        if (expiry == null || !expiry.isAfter(Instant.now())) {
            return OptionalInt.empty();
        }
        long seconds = Duration.between(Instant.now(), expiry).getSeconds();
        if (seconds <= 0) {
            return OptionalInt.of(0);
        }
        int days = (int) Math.ceil(seconds / 86400.0);
        return OptionalInt.of(Math.max(days, 0));
    }

    public boolean grantTemporaryVip(CommandSender sender, OfflinePlayer target, String tierId, String duration) {
        Optional<StvipTier> tierOpt = configManager.getTierById(tierId);
        if (tierOpt.isEmpty()) {
            MessageUtils.sendRaw(sender, MessageUtils.colorize("&c✗ &f未知 VIP 档位: &e" + tierId));
            return false;
        }
        if (duration == null || duration.isBlank()) {
            MessageUtils.sendRaw(sender, MessageUtils.colorize("&c✗ &f时长不能为空，例如: &e7d"));
            return false;
        }
        String identity = target.getName() != null && !target.getName().isBlank()
                ? target.getName()
                : target.getUniqueId().toString();
        String command = "lp user " + identity + " permission settemp " + tierOpt.get().getPermission() + " true " + duration;
        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (success) {
            MessageUtils.sendRaw(sender, MessageUtils.colorize("&a✓ &f已发放 " + tierOpt.get().getDisplayName()
                    + " &f给 &e" + identity + " &f时长: &e" + duration));
        } else {
            MessageUtils.sendRaw(sender, MessageUtils.colorize("&c✗ &fLuckPerms 执行失败，请检查命令与插件状态"));
        }
        return success;
    }

    public List<String> buildFeatureLines(Player player, int remoteRemaining, int freeRemaining) {
        Optional<StvipTier> tierOpt = resolveTier(player);
        if (tierOpt.isEmpty()) {
            return List.of();
        }
        StvipTier tier = tierOpt.get();
        List<String> lines = new ArrayList<>();
        lines.add("&7- &a自动入库");
        lines.add("&7- &b任务远程提交: &f" + remoteRemaining + "&7/&f" + tier.getQuestDailyRemoteClaimLimit());
        lines.add("&7- &dArmor 折扣: &f" + percentOff(tier.getArmorCostMultiplier()) + "%");
        if (tier.getPriority() >= 2) {
            lines.add("&7- &3任务进度预提交显示（ES 库存）");
            lines.add("&7- &3远程提交时可自动消耗 ES 补足缺口");
        }
        if (tier.getLootExtraSlots() > 0 || tier.getLootExtraPerItemMax() > 0) {
            lines.add("&7- &6ES 扩容: &f+" + tier.getLootExtraSlots() + " 槽位 &7| &f+" + tier.getLootExtraPerItemMax() + " 单物品上限");
        }
        if (tier.getQuestMinDifficultyExclusive() > 0) {
            lines.add("&7- &e手动任务过滤: &f难度 > " + tier.getQuestMinDifficultyExclusive());
        }
        if (tier.getQuestDailyFreeClaimLimit() > 0) {
            lines.add("&7- &c免材料领奖: &f" + freeRemaining + "&7/&f" + tier.getQuestDailyFreeClaimLimit() + " &7(0.6 倍奖励)");
        }
        if (tier.isBountyPreannounce()) {
            lines.add("&7- &3悬赏预告: &f刷新前 5 分钟");
        }
        return lines;
    }

    public boolean hasAnyVip(Player player) {
        return resolveTier(player).isPresent();
    }

    private Instant resolvePermissionExpiry(Player player, String permission) {
        LuckPerms luckPerms = lookupLuckPerms();
        if (luckPerms == null) {
            return null;
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return null;
        }
        return user.getNodes(NodeType.PERMISSION).stream()
                .filter(node -> node.getValue())
                .filter(node -> permission.equalsIgnoreCase(node.getPermission()))
                .filter(node -> node.hasExpiry())
                .map(node -> node.getExpiry())
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
    }

    private LuckPerms lookupLuckPerms() {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            return null;
        }
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static int percentOff(double multiplier) {
        return (int) Math.round((1.0 - multiplier) * 100.0);
    }
}
