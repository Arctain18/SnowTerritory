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
import java.util.regex.Pattern;

/** 按权限解析当前 VIP 档并提供各模块使用的数值。 */
public class StvipService {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^[1-9]\\d*[shd]$", Pattern.CASE_INSENSITIVE);

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
            MessageUtils.sendConfigMessage(sender, "stvip.give-unknown-tier", "&c✗ &f未知 VIP 档位: &e{tier}", "tier", tierId);
            return false;
        }
        if (duration == null || duration.isBlank()) {
            MessageUtils.sendConfigMessage(sender, "stvip.give-duration-empty", "&c✗ &f时长不能为空，例如: &e7d");
            return false;
        }
        String normalizedDuration = duration.trim().toLowerCase();
        if (!DURATION_PATTERN.matcher(normalizedDuration).matches()) {
            MessageUtils.sendConfigMessage(sender, "stvip.give-duration-invalid",
                    "&c✗ &f时长格式无效，仅支持 &e<数字><s|h|d>&f，例如: &e30d");
            return false;
        }
        String identity = target.getName() != null && !target.getName().isBlank()
                ? target.getName()
                : target.getUniqueId().toString();
        String command = "lp user " + identity + " permission settemp " + tierOpt.get().getPermission() + " true " + normalizedDuration;
        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (success) {
            MessageUtils.sendConfigMessage(sender, "stvip.give-success", "&a✓ &f已发放 {display} &f给 &e{player} &f时长: &e{duration}",
                    "display", tierOpt.get().getDisplayName(), "player", identity, "duration", normalizedDuration);
        } else {
            MessageUtils.sendConfigMessage(sender, "stvip.give-lp-failed", "&c✗ &fLuckPerms 执行失败，请检查命令与插件状态");
        }
        return success;
    }

    public boolean revokeVip(CommandSender sender, OfflinePlayer target) {
        String identity = target.getName() != null && !target.getName().isBlank()
                ? target.getName()
                : target.getUniqueId().toString();
        boolean success = true;
        for (StvipTier tier : configManager.getTiers()) {
            String command = "lp user " + identity + " permission unsettemp " + tier.getPermission();
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                success = false;
            }
        }
        if (success) {
            MessageUtils.sendConfigMessage(sender, "stvip.revoke-success",
                    "&a✓ &f已取消玩家 &e{player} &f的所有 VIP 权限", "player", identity);
        } else {
            MessageUtils.sendConfigMessage(sender, "stvip.revoke-lp-failed",
                    "&c✗ &f取消 VIP 失败，请检查 LuckPerms 状态");
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
        lines.add(MessageUtils.getConfigMessage("stvip.join-line-loot-auto", "&7- &a自动入库"));
        lines.add(MessageUtils.getConfigMessage("stvip.join-line-quest-remote", "&7- &b任务远程提交: &f{remote_remaining}&7/&f{remote_limit}",
                "remote_remaining", String.valueOf(remoteRemaining), "remote_limit", String.valueOf(tier.getQuestDailyRemoteClaimLimit())));
        lines.add(MessageUtils.getConfigMessage("stvip.join-line-armor", "&7- &dArmor 折扣: &f{percent_off}%",
                "percent_off", String.valueOf(percentOff(tier.getArmorCostMultiplier()))));
        if (tier.getPriority() >= 2) {
            lines.add(MessageUtils.getConfigMessage("stvip.join-line-quest-precommit", "&7- &3任务进度预提交显示（ES 库存）"));
            lines.add(MessageUtils.getConfigMessage("stvip.join-line-quest-es", "&7- &3远程提交时可自动消耗 ES 补足缺口"));
        }
        if (tier.getLootExtraSlots() > 0 || tier.getLootExtraPerItemMax() > 0) {
            lines.add(MessageUtils.getConfigMessage("stvip.join-line-loot-expand",
                    "&7- &6ES 扩容: &f+{slots} 槽位 &7| &f+{per_item_max} 单物品上限",
                    "slots", String.valueOf(tier.getLootExtraSlots()), "per_item_max", String.valueOf(tier.getLootExtraPerItemMax())));
        }
        if (tier.getQuestMinDifficultyExclusive() > 0) {
            lines.add(MessageUtils.getConfigMessage("stvip.join-line-difficulty", "&7- &e手动任务过滤: &f难度 > {min_exclusive}",
                    "min_exclusive", String.valueOf(tier.getQuestMinDifficultyExclusive())));
        }
        if (tier.getQuestDailyFreeClaimLimit() > 0) {
            lines.add(MessageUtils.getConfigMessage("stvip.join-line-free-claim",
                    "&7- &c免材料领奖: &f{free_remaining}&7/&f{free_limit} &7(0.6 倍奖励)",
                    "free_remaining", String.valueOf(freeRemaining), "free_limit", String.valueOf(tier.getQuestDailyFreeClaimLimit())));
        }
        if (tier.isBountyPreannounce()) {
            lines.add(MessageUtils.getConfigMessage("stvip.join-line-bounty", "&7- &3悬赏预告: &f刷新前 5 分钟"));
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
