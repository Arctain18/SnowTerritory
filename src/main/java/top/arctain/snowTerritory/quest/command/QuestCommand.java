package top.arctain.snowTerritory.quest.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.quest.config.MessageProvider;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 任务命令处理器
 */
public class QuestCommand implements CommandExecutor, TabCompleter {

    private final QuestService service;
    private final MessageProvider messages;

    public QuestCommand(org.bukkit.plugin.Plugin plugin, QuestConfigManager configManager, QuestService service) {
        this.service = service;
        String lang = configManager.getMainConfig().getString("features.default-language", "zh_CN");
        this.messages = new MessageProvider(configManager.getMessagePacks(), lang);
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
                            messages.get(sender, "no-permission", "&c✗ &f没有权限"));
                    return true;
                }
                service.reload();
                MessageUtils.sendConfigMessage(sender, "quest.reload-done",
                        messages.get(sender, "reload-done", "&a✓ &f任务配置已重载"));
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
                    messages.get(sender, "player-only", "&c✗ &f此命令仅限玩家使用"));
            return true;
        }

        QuestType type = QuestType.MATERIAL;
        if (args.length > 1) {
            String typeStr = args[1].toUpperCase();
            if (typeStr.equals("KILL")) {
                type = QuestType.KILL;
                // TODO: 击杀任务尚未实现
                player.sendMessage(ColorUtils.colorize("&c✗ &f击杀任务尚未实现"));
                return true;
            }
        }

        Quest quest = service.acceptNormalQuest(player, type);
        if (quest == null) {
            player.sendMessage(ColorUtils.colorize(messages.get(player, "quest-already-active",
                    "&c✗ &f你已有进行中的任务")));
            return true;
        }

        String materialName = quest.getMaterialKey().split(":")[1];
        String message = messages.get(player, "quest-accepted",
                "&a✓ &f已接取任务: &e%quest%");
        message = message.replace("%quest%", String.format("收集 %s x%d", materialName, quest.getRequiredAmount()));
        player.sendMessage(ColorUtils.colorize(message));
        return true;
    }

    /**
     * 处理列出任务
     */
    private boolean handleList(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "quest.player-only",
                    messages.get(sender, "player-only", "&c✗ &f此命令仅限玩家使用"));
            return true;
        }

        List<Quest> quests = service.getActiveQuests(player.getUniqueId());
        List<Quest> bountyQuests = service.getActiveBountyQuests();

        if (quests.isEmpty() && bountyQuests.isEmpty()) {
            player.sendMessage(ColorUtils.colorize(messages.get(player, "quest-list-empty",
                    "&7暂无进行中的任务")));
            return true;
        }

        player.sendMessage(ColorUtils.colorize(messages.get(player, "quest-list-header",
                "&6=== 你的任务列表 ===")));

        // 显示普通任务
        for (Quest quest : quests) {
            if (quest.isExpired()) {
                continue;
            }
            String materialName = quest.getMaterialKey().split(":")[1];
            String questDesc = String.format("收集 %s x%d", materialName, quest.getRequiredAmount());
            String message = messages.get(player, "quest-list-item",
                    "&7- &e%quest% &7(进度: %current%/%required%)");
            message = message.replace("%quest%", questDesc)
                    .replace("%current%", String.valueOf(quest.getCurrentAmount()))
                    .replace("%required%", String.valueOf(quest.getRequiredAmount()));
            player.sendMessage(ColorUtils.colorize(message));
        }

        // 显示悬赏任务
        for (Quest quest : bountyQuests) {
            if (quest.isExpired()) {
                continue;
            }
            String materialName = quest.getMaterialKey().split(":")[1];
            String questDesc = String.format("[悬赏] 收集 %s x%d", materialName, quest.getRequiredAmount());
            String message = messages.get(player, "quest-list-item",
                    "&7- &e%quest% &7(进度: %current%/%required%)");
            message = message.replace("%quest%", questDesc)
                    .replace("%current%", String.valueOf(quest.getCurrentAmount()))
                    .replace("%required%", String.valueOf(quest.getRequiredAmount()));
            player.sendMessage(ColorUtils.colorize(message));
        }

        return true;
    }

    /**
     * 处理完成任务（自动领取所有已完成的悬赏任务）
     */
    private boolean handleComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "quest.player-only",
                    messages.get(sender, "player-only", "&c✗ &f此命令仅限玩家使用"));
            return true;
        }

        int claimed = service.claimCompletedBountyQuests(player);
        
        if (claimed == 0) {
            player.sendMessage(ColorUtils.colorize(messages.get(player, "no-completed-bounty",
                    "&c✗ &f没有已完成的悬赏任务可领取")));
        } else {
            player.sendMessage(ColorUtils.colorize(messages.get(player, "bounty-claimed",
                    "&a✓ &f已领取 %count% 个悬赏任务奖励").replace("%count%", String.valueOf(claimed))));
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

