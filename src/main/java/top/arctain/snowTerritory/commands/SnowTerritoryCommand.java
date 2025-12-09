package top.arctain.snowTerritory.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.gui.ItemEditorGUI;

import java.util.ArrayList;
import java.util.List;

public class SnowTerritoryCommand implements CommandExecutor, TabCompleter {

    private final PluginConfig config;
    private final ItemEditorGUI gui;
    private final ItemIdCommand itemIdCommand;

    public SnowTerritoryCommand(Main plugin, PluginConfig config) {
        this.config = config;
        this.gui = new ItemEditorGUI(config);
        this.itemIdCommand = new ItemIdCommand();
        // plugin 参数保留以保持接口一致性
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
            sender.sendMessage(ChatColor.RED + "未知的子命令！输入 /" + label + " 查看帮助。");
            return true;
        }
    }

    /**
     * 处理强化命令: /st reinforce 或 /st r
     */
    private boolean handleReinforce(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mmoitemseditor.edit") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "您没有权限使用此命令！");
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
            sender.sendMessage(ChatColor.RED + "您没有权限使用此命令！");
            return true;
        }

        config.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "插件配置已重载！");
        return true;
    }

    /**
     * 处理查看ID命令: /st checkID
     */
    private boolean handleCheckID(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mmoitemseditor.itemid") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "您没有权限使用此命令！");
            return true;
        }

        return itemIdCommand.onCommand(sender, null, "checkid", new String[0]);
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== SnowTerritory 命令帮助 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/snowterritory reinforce" + ChatColor.WHITE + " (或 " + ChatColor.YELLOW + "/st r" + ChatColor.WHITE + ") - 打开物品强化界面");
        sender.sendMessage(ChatColor.YELLOW + "/snowterritory reload" + ChatColor.WHITE + " (或 " + ChatColor.YELLOW + "/st reload" + ChatColor.WHITE + ") - 重载插件配置");
        sender.sendMessage(ChatColor.YELLOW + "/snowterritory checkID" + ChatColor.WHITE + " (或 " + ChatColor.YELLOW + "/st checkID" + ChatColor.WHITE + ") - 查看手中物品的MMOItems ID");
        sender.sendMessage(ChatColor.GOLD + "==========================================");
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

