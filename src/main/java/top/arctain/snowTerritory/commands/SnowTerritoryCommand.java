package top.arctain.snowTerritory.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.gui.ItemEditorGUI;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class SnowTerritoryCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final PluginConfig config;
    private final ItemEditorGUI gui;
    private final ItemIdCommand itemIdCommand;

    public SnowTerritoryCommand(Main plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.gui = new ItemEditorGUI(config, plugin);
        this.itemIdCommand = new ItemIdCommand();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 处理简写
        if (subCommand.equals("r") || subCommand.equals("reinforce")) {
            return handleReinforce(sender, args);
        } else if (subCommand.equals("reload")) {
            return handleReload(sender, args);
        } else if (subCommand.equals("checkid") || subCommand.equals("check")) {
            return handleCheckID(sender, args);
        } else {
            MessageUtils.sendError(sender, "command.unknown-command", "&c✗ &f未知的子命令！输入 /{label} 查看帮助。", "label", label);
            return true;
        }
    }

    /**
     * 处理强化命令: /st reinforce 或 /st r
     */
    private boolean handleReinforce(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendError(sender, "command.player-only", "&c✗ &f此命令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mmoitemseditor.edit") && !player.isOp()) {
            MessageUtils.sendError(player, "command.no-permission", "&c✗ &f您没有权限使用此命令！");
            return true;
        }

        gui.openGUI(player);
        return true;
    }

    /**
     * 处理重载命令: /st reload
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mmoitemseditor.reload") && !(sender instanceof Player && ((Player) sender).isOp())) {
            MessageUtils.sendError(sender, "command.no-permission", "&c✗ &f您没有权限使用此命令！");
            return true;
        }

        config.reloadConfig();
        MessageUtils.sendSuccess(sender, "command.reload-success", "&a✓ &f插件配置已重载！");
        return true;
    }

    /**
     * 处理查看ID命令: /st checkID
     */
    private boolean handleCheckID(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendError(sender, "command.player-only", "&c✗ &f此命令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mmoitemseditor.itemid") && !player.isOp()) {
            MessageUtils.sendError(player, "command.no-permission", "&c✗ &f您没有权限使用此命令！");
            return true;
        }

        return itemIdCommand.onCommand(sender, null, "checkid", new String[0]);
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        MessageUtils.sendHelp(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            
            if ("reinforce".startsWith(input) || "r".startsWith(input)) {
                completions.add("reinforce");
            }
            if ("reload".startsWith(input)) {
                completions.add("reload");
            }
            if ("checkid".startsWith(input) || "check".startsWith(input)) {
                completions.add("checkID");
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}

