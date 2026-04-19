package top.arctain.snowTerritory.quest.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.enderstorage.service.LootStorageService;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.config.QuestListProgressConfig;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestDatabaseDao;
import top.arctain.snowTerritory.quest.data.QuestStatus;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.stvip.service.StvipService;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.DisplayUtils;
import top.arctain.snowTerritory.quest.utils.QuestUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 任务命令处理器
 */
public class QuestCommand implements CommandExecutor, TabCompleter {

    private static final double VIP3_FREE_CLAIM_REWARD_MULTIPLIER = 0.6;

    private final Main plugin;
    private final QuestService service;
    private final QuestConfigManager configManager;
    private final QuestDatabaseDao databaseDao;
    private final StvipService stvipService;
    private final LootStorageService lootStorageService;

    public QuestCommand(org.bukkit.plugin.Plugin plugin, QuestConfigManager configManager, QuestService service, QuestDatabaseDao databaseDao) {
        this.plugin = plugin instanceof Main m ? m : null;
        this.service = service;
        this.configManager = configManager;
        this.databaseDao = databaseDao;
        this.stvipService = this.plugin != null ? this.plugin.getStvipService() : null;
        this.lootStorageService = this.plugin != null && this.plugin.getEnderStorageModule() != null
                ? this.plugin.getEnderStorageModule().getLootStorageService()
                : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleList(sender);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "accept", "a" -> {
                return handleAccept(sender, args);
            }
            case "list", "l" -> {
                return handleList(sender);
            }
            case "complete", "c" -> {
                return handleComplete(sender, args);
            }
            case "setlevel" -> {
                return handleSetLevel(sender, args);
            }
            case "getlevel" -> {
                return handleGetLevel(sender);
            }
            case "reload" -> {
                if (!sender.hasPermission("st.quest.admin")) {
                    MessageUtils.sendConfigMessage(sender, "quest.no-permission",
                            "&c✗ &f没有权限");
                    return true;
                }
                service.reload();
                MessageUtils.sendConfigMessage(sender, "quest.reload-done",
                        "&a✓ &f任务配置已重载");
                return true;
            }
            default -> {
                return handleList(sender);
            }
        }
    }

    /**
     * 处理接取任务
     */
    private boolean handleAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "quest.player-only",
                    "&c✗ &f此命令仅限玩家使用");
            return true;
        }

        QuestType type = QuestType.MATERIAL;
        if (args.length > 1) {
            String typeStr = args[1].toUpperCase();
            if (typeStr.equals("KILL")) {
                type = QuestType.KILL;
                MessageUtils.sendConfigMessage(player, "quest.kill-not-implemented",
                        "&c✗ &f击杀任务尚未实现");
                return true;
            }
            if (typeStr.equals("COLLECT")) {
                type = QuestType.COLLECT;
            }
        }

        if (service.getActiveQuest(player.getUniqueId(), type) != null) {
            MessageUtils.sendConfigMessage(player, "quest.already-active",
                    "&c✗ &f你已有进行中的任务");
            return true;
        }
        Quest quest = service.acceptNormalQuest(player, type);
        if (quest == null) {
            MessageUtils.sendConfigMessage(player, "quest.accept-failed",
                    "&c✗ &f当前没有符合你档位难度要求的任务，请稍后重试");
            return true;
        }

        MessageUtils.sendConfigMessage(player, "quest.accepted",
                "&a✓ &f已接取任务: &e{quest}",
                "quest", quest.getDescription());
        return true;
    }

    /**
     * 处理列出任务
     */
    private boolean handleList(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "quest.player-only",
                    "&c✗ &f此命令仅限玩家使用");
            return true;
        }

        List<Quest> allQuests = service.getAllQuests(player.getUniqueId());
        List<Quest> bountyQuests = service.getActiveBountyQuests();

        if (allQuests.isEmpty() && bountyQuests.isEmpty()) {
            MessageUtils.sendConfigRaw(player, "quest.list-empty",
                    "&7暂无任务");
            return true;
        }

        MessageUtils.sendConfigRaw(player, "quest.list-header",
                "     &7========= &f进行中的任务 &7=========");

        displayQuests(player, bountyQuests);
        displayQuests(player, allQuests);

        return true;
    }

    private void displayQuests(Player player, List<Quest> quests) {
        for (Quest quest : quests) {
            displayQuest(player, quest);
        }
    }

    private void displayQuest(Player player, Quest quest) {

        String questDesc = quest.getDescription();
        String statusText = getStatusText(quest.getStatus(), quest);
        
        MessageUtils.sendConfigRaw(player, "quest.list-item",
                "&7&l·&r {quest} &7{status}",
                "quest", questDesc,
                "status", statusText);

        if (service.isActiveAndNotExpired(quest)) {
            displayQuestProgress(player, quest);
        }
    }

    /** VIP 在进度行尾追加 ES 总库存；非 VIP 或无法解析档位时返回空串。 */
    private String buildListProgressVipStorageSuffix(Player player, Quest quest) {
        if (stvipService == null) {
            return "";
        }
        return stvipService.resolveTier(player).map(tier -> {
            int esTotal = lootStorageService != null
                    ? lootStorageService.getAmount(player.getUniqueId(), quest.getMaterialKey()) : 0;
            String vipLabel = tier.getDisplayName() != null ? tier.getDisplayName() : tier.getId();
            return MessageUtils.getConfigMessage("quest.list-progress-vip-suffix",
                    " {vip} &{#6dc97f}ES &7库存: &3{amount}", "vip", vipLabel, "amount", String.valueOf(esTotal));
        }).orElse("");
    }

    private void displayQuestProgress(Player player, Quest quest) {
        int req = quest.getRequiredAmount();
        int cur = quest.getCurrentAmount();
        String curStr = String.valueOf(cur);
        String reqStr = String.valueOf(req);
        QuestListProgressConfig cfg = configManager.getListProgressConfig();
        String fraction = MessageUtils.getConfigMessage("quest.list-fraction",
                "&7 (&e{current}&7/&e{required}&7)",
                "current", curStr, "required", reqStr);

        String bar = "";
        if (cfg.isBarEnabled()) {
            if (quest.getType() == QuestType.MATERIAL
                    && cfg.isShowEsSegment()
                    && lootStorageService != null
                    && playerCanUseStoragePreview(player)) {
                int esAmount = lootStorageService.getAmount(player.getUniqueId(), quest.getMaterialKey());
                int need = Math.max(0, req - cur);
                int esUsable = Math.max(0, Math.min(esAmount, need));
                if (esUsable > 0) {
                    bar = DisplayUtils.progressBarWithEsStorage(
                            cfg.getStyle(),
                            cfg.getLowColor(), cfg.getMidColor(), cfg.getHighColor(),
                            cfg.getEsColor(),
                            cur, esUsable, req, cfg.getLength(), cfg.getEmptySlotColor());
                } else {
                    bar = DisplayUtils.progressBar(
                            cfg.getStyle(),
                            cfg.getLowColor(), cfg.getMidColor(), cfg.getHighColor(),
                            cur, req, cfg.getLength(), cfg.getEmptySlotColor());
                }
            } else {
                bar = DisplayUtils.progressBar(
                        cfg.getStyle(),
                        cfg.getLowColor(), cfg.getMidColor(), cfg.getHighColor(),
                        cur, req, cfg.getLength(), cfg.getEmptySlotColor());
            }
        }
        String vipStorage = buildListProgressVipStorageSuffix(player, quest);
        String progressText = bar + fraction + vipStorage;
        MessageUtils.sendConfigRaw(player, "quest.list-progress",
                "&7  &l·&r 进度: {bar}{fraction}{vipStorage}",
                "bar", bar, "fraction", fraction, "progressText", progressText,
                "current", curStr, "required", reqStr, "vipStorage", vipStorage);
        
        String currentRating = QuestUtils.getTimeRatingDisplay(quest.getElapsedTime(), configManager.getBonusTimeBonus())
        + "&7 (&e" + DisplayUtils.formatTime(quest.getElapsedTime()) + "&7)";
        MessageUtils.sendConfigRaw(player, "quest.list-rating",
                "&7  &l·&r 评级: &e{currentRating}",
                "currentRating", currentRating);
    }

    private String getStatusText(QuestStatus status, Quest quest) {
        switch (status) {
            case ACTIVE:
                if (quest.isExpired()) {
                    return "&8[已过期]";
                }
                return "&a[进行中]";
            case COMPLETED:
                return "&a[已完成]";
            case EXPIRED:
                return "&8[已过期]";
            default:
                return "&7[未知]";
        }
    }

    /**
     * 处理完成任务（自动领取所有已完成的悬赏任务）
     */
    private boolean handleComplete(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args.length < 2) {
                MessageUtils.sendConfigMessage(sender, "quest.complete-usage-console",
                        "&e用法: /sn q complete <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtils.sendConfigMessage(sender, "quest.complete-target-not-found",
                        "&c✗ &f未找到在线玩家: {name}", "name", args[1]);
                return true;
            }
            executeCompleteFor(sender, target, false, true, false);
            return true;
        }
        if (!(sender instanceof Player actor)) {
            MessageUtils.sendConfigMessage(sender, "quest.player-only", "&c✗ &f此命令仅限玩家使用");
            return true;
        }

        boolean opBypass = actor.isOp();
        boolean hasVip = stvipService != null && stvipService.hasAnyVip(actor);
        if (!opBypass && !hasVip) {
            MessageUtils.sendConfigMessage(actor, "quest.complete-no-permission",
                    "&c✗ &f你不是 VIP，无法使用远程提交");
            return true;
        }

        boolean freeMode = args.length >= 2 && "free".equalsIgnoreCase(args[1]);
        Player target = actor;
        if (args.length >= 2 && !freeMode) {
            if (!opBypass) {
                MessageUtils.sendConfigMessage(actor, "quest.complete-target-denied",
                        "&c✗ &f仅 OP 可指定其他玩家");
                return true;
            }
            Player resolved = Bukkit.getPlayerExact(args[1]);
            if (resolved == null) {
                MessageUtils.sendConfigMessage(actor, "quest.complete-target-not-found",
                        "&c✗ &f未找到在线玩家: {name}", "name", args[1]);
                return true;
            }
            target = resolved;
        }

        if (freeMode && !isVip3(actor)) {
            MessageUtils.sendConfigMessage(actor, "quest.complete-free-only-vip3",
                    "&c✗ &f仅 VIP3 可使用免材料领奖");
            return true;
        }

        executeCompleteFor(actor, target, freeMode, false, opBypass);
        return true;
    }

    private void executeCompleteFor(CommandSender feedback, Player target, boolean freeMode, boolean bypassQuota, boolean opBypass) {
        Player actor = feedback instanceof Player p ? p : null;
        if (actor != null && !opBypass && !bypassQuota) {
            int remoteLimit = stvipService != null ? stvipService.getQuestDailyRemoteClaimLimit(actor) : 0;
            int remoteUsed = service.getDailyRemoteClaimUsed(actor.getUniqueId());
            if (remoteUsed >= remoteLimit) {
                MessageUtils.sendConfigMessage(actor, "quest.complete-remote-limit",
                        "&c✗ &f今日远程提交次数已用尽: {used}/{limit}",
                        "used", String.valueOf(remoteUsed), "limit", String.valueOf(remoteLimit));
                return;
            }
            if (freeMode) {
                int freeLimit = stvipService != null ? stvipService.getQuestDailyFreeClaimLimit(actor) : 0;
                int freeUsed = service.getDailyFreeClaimUsed(actor.getUniqueId());
                if (freeUsed >= freeLimit) {
                    MessageUtils.sendConfigMessage(actor, "quest.complete-free-limit",
                            "&c✗ &f今日免材料次数已用尽: {used}/{limit}",
                            "used", String.valueOf(freeUsed), "limit", String.valueOf(freeLimit));
                    return;
                }
            }
        }

        boolean allowStorage = isVip2OrAbove(target);
        double rewardMultiplier = freeMode ? VIP3_FREE_CLAIM_REWARD_MULTIPLIER : 1.0;
        QuestService.CompletionResult result = service.completeByCommand(target, allowStorage, freeMode, rewardMultiplier);
        if (!result.hasAnySuccess()) {
            MessageUtils.sendConfigMessage(feedback, "quest.no-completed-bounty",
                    "&c✗ &f没有可提交或可领取的任务");
            return;
        }
        if (actor != null && !opBypass && !bypassQuota) {
            service.incrementDailyRemoteClaimUsed(actor.getUniqueId());
            if (freeMode) {
                service.incrementDailyFreeClaimUsed(actor.getUniqueId());
            }
        }

        MessageUtils.sendConfigMessage(feedback, "quest.complete-summary",
                "&a✓ &f提交完成: 背包提交 &e{inv}&f，ES 提交 &e{es}&f，完成普通任务 &e{normal}&f，领取悬赏 &e{bounty}",
                "inv", String.valueOf(result.inventorySubmitted()),
                "es", String.valueOf(result.storageSubmitted()),
                "normal", String.valueOf(result.completedNormal()),
                "bounty", String.valueOf(result.claimedBounty()));
        if (freeMode) {
            MessageUtils.sendConfigMessage(feedback, "quest.complete-free-mode-applied",
                    "&d✦ &f本次免材料模式已生效，奖励倍率: &e0.6x");
        }
    }

    private boolean playerCanUseStoragePreview(Player player) {
        return isVip2OrAbove(player) || player.isOp();
    }

    private boolean isVip2OrAbove(Player player) {
        if (stvipService == null) {
            return false;
        }
        return stvipService.resolveTier(player).map(t -> t.getPriority() >= 2).orElse(false);
    }

    private boolean isVip3(Player player) {
        if (stvipService == null) {
            return false;
        }
        return stvipService.resolveTier(player).map(t -> t.getPriority() >= 3).orElse(false);
    }

    /**
     * 处理设置等级：/sn q setlevel <等级> [玩家]。无玩家参数则给发送者设置；给他人设置需 OP。
     */
    private boolean handleSetLevel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "quest.player-only",
                    "&c✗ &f此命令仅限玩家使用");
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendConfigMessage(player, "quest.setlevel-usage",
                    "&c✗ &f用法: /sn q setlevel <等级> [玩家]");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            MessageUtils.sendConfigMessage(player, "quest.setlevel-invalid",
                    "&c✗ &f等级必须是整数", "reason", "等级必须是整数");
            return true;
        }

        Set<Integer> validLevels = configManager.getValidMaterialLevels();
        if (!validLevels.contains(level)) {
            MessageUtils.sendConfigMessage(player, "quest.setlevel-invalid",
                    "&c✗ &f等级必须在 rewards/level.yml 的键范围内", "reason",
                    "等级必须在 rewards/level.yml 的键范围内: " + validLevels);
            return true;
        }

        UUID targetId;
        String targetName;
        if (args.length >= 3) {
            if (!player.isOp()) {
                MessageUtils.sendConfigMessage(player, "quest.no-permission",
                        "&c✗ &f没有权限");
                return true;
            }
            String name = args[2];
            Player online = Bukkit.getPlayer(name);
            if (online != null) {
                targetId = online.getUniqueId();
                targetName = online.getName();
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
                if (offline == null || !offline.hasPlayedBefore()) {
                    MessageUtils.sendConfigMessage(player, "quest.setlevel-player-not-found",
                            "&c✗ &f未找到玩家: {name}", "name", name);
                    return true;
                }
                targetId = offline.getUniqueId();
                targetName = offline.getName() != null ? offline.getName() : name;
            }
        } else {
            targetId = player.getUniqueId();
            targetName = player.getName();
        }

        databaseDao.setMaxMaterialLevel(targetId, level);
        MessageUtils.sendConfigMessage(player, "quest.setlevel-success",
                "&a✓ &f已设置 {player} 的材料任务等级为 {level}",
                "player", targetName, "level", String.valueOf(level));
        return true;
    }

    /**
     * 处理查看等级上限
     */
    private boolean handleGetLevel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "quest.player-only",
                    "&c✗ &f此命令仅限玩家使用");
            return true;
        }

        int level = databaseDao.getMaxMaterialLevel(player.getUniqueId());
        MessageUtils.sendConfigMessage(player, "quest.getlevel-success",
                "&a✓ &f你的材料任务等级上限: &e{level}",
                "level", String.valueOf(level));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if ("accept".startsWith(args[0].toLowerCase())) list.add("accept");
            if ("list".startsWith(args[0].toLowerCase())) list.add("list");
            if ("complete".startsWith(args[0].toLowerCase())) list.add("complete");
            if ("setlevel".startsWith(args[0].toLowerCase())) list.add("setlevel");
            if ("getlevel".startsWith(args[0].toLowerCase())) list.add("getlevel");
            if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("st.quest.admin")) {
                list.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            list.add("material");
            list.add("kill");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("complete")) {
            if (sender instanceof Player player) {
                if (player.isOp()) {
                    String input = args[1].toLowerCase();
                    if ("free".startsWith(input)) {
                        list.add("free");
                    }
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(input)) {
                            list.add(p.getName());
                        }
                    }
                } else if (isVip3(player) && "free".startsWith(args[1].toLowerCase())) {
                    list.add("free");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setlevel")) {
            String input = args[1].toLowerCase();
            for (Integer l : configManager.getValidMaterialLevels()) {
                if (String.valueOf(l).startsWith(input)) list.add(String.valueOf(l));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setlevel") && sender.isOp()) {
            String input = args[2].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) list.add(p.getName());
            }
        }
        return list;
    }
}

