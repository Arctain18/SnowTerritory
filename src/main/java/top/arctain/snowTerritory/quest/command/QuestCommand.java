package top.arctain.snowTerritory.quest.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestStatus;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.data.QuestReleaseMethod;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.DisplayUtils;
import top.arctain.snowTerritory.quest.utils.QuestUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务命令处理器
 */
public class QuestCommand implements CommandExecutor, TabCompleter {

    private final QuestService service;
    private final QuestConfigManager configManager;

    public QuestCommand(org.bukkit.plugin.Plugin plugin, QuestConfigManager configManager, QuestService service) {
        this.service = service;
        this.configManager = configManager;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if ("accept".startsWith(args[0].toLowerCase())) list.add("accept");
            if ("list".startsWith(args[0].toLowerCase())) list.add("list");
            if ("complete".startsWith(args[0].toLowerCase())) list.add("complete");
            if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("st.quest.admin")) {
                list.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            list.add("material");
            list.add("kill");
        }
        return list;
    }
}

