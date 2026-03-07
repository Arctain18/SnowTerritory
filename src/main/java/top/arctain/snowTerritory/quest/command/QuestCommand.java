package top.arctain.snowTerritory.quest.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestDatabaseDao;
import top.arctain.snowTerritory.quest.data.QuestStatus;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.service.QuestService;
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

    private final QuestService service;
    private final QuestConfigManager configManager;
    private final QuestDatabaseDao databaseDao;

    public QuestCommand(org.bukkit.plugin.Plugin plugin, QuestConfigManager configManager, QuestService service, QuestDatabaseDao databaseDao) {
        this.service = service;
        this.configManager = configManager;
        this.databaseDao = databaseDao;
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
                // TODO: 击杀任务尚未实现
                MessageUtils.sendConfigMessage(player, "quest.kill-not-implemented",
                        "&c✗ &f击杀任务尚未实现");
                return true;
            }
        }

        Quest quest = service.acceptNormalQuest(player, type);
        if (quest == null) {
            MessageUtils.sendConfigMessage(player, "quest.already-active",
                    "&c✗ &f你已有进行中的任务");
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
            MessageUtils.sendConfigMessage(player, "quest.list-empty",
                    "&7暂无任务");
            return true;
        }

        MessageUtils.sendConfigMessage(player, "quest.list-header",
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
        
        MessageUtils.sendConfigMessage(player, "quest.list-item",
                "&7&l·&r {quest} &7{status}",
                "quest", questDesc,
                "status", statusText);

        if (service.isActiveAndNotExpired(quest)) {
            displayQuestProgress(player, quest);
        }
    }

    private void displayQuestProgress(Player player, Quest quest) {

        String progressBar = DisplayUtils.progressBar(DisplayUtils.BarStyle.BARS, 
            "&c", "&e", "&a", 
            quest.getCurrentAmount(), quest.getRequiredAmount(), 50)
            + "&7 (&e" + Integer.toString(quest.getCurrentAmount()) + "&7/&e" + Integer.toString(quest.getRequiredAmount()) + "&7)";
        MessageUtils.sendConfigMessage(player, "quest.list-progress",
                "&7  &l·&r 进度: &e{progressBar}",
                "progressBar", progressBar);
        
        String currentRating = QuestUtils.getTimeRatingDisplay(quest.getElapsedTime(), configManager.getBonusTimeBonus())
        + "&7 (&e" + DisplayUtils.formatTime(quest.getElapsedTime()) + "&7)";
        MessageUtils.sendConfigMessage(player, "quest.list-rating",
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
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "quest.player-only",
                    "&c✗ &f此命令仅限玩家使用");
            return true;
        }

        int claimed = service.claimCompletedBountyQuests(player);
        
        if (claimed == 0) {
            MessageUtils.sendConfigMessage(player, "quest.no-completed-bounty",
                    "&c✗ &f没有已完成的悬赏任务可领取");
        } else {
            MessageUtils.sendConfigMessage(player, "quest.bounty-claimed",
                    "&a✓ &f已领取 {count} 个悬赏任务奖励",
                    "count", String.valueOf(claimed));
        }

        return true;
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

