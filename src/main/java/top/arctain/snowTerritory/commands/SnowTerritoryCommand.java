package top.arctain.snowTerritory.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.enderstorage.EnderStorageModule;
import top.arctain.snowTerritory.gui.ItemEditorGUI;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class SnowTerritoryCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final PluginConfig config;
    private final ItemEditorGUI gui;
    private final ItemIdCommand itemIdCommand;
    private final EnderStorageModule enderModule;

    public SnowTerritoryCommand(Main plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.gui = new ItemEditorGUI(config, plugin);
        this.itemIdCommand = new ItemIdCommand();
        this.enderModule = plugin.getEnderStorageModule();
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
        } else if (subCommand.equals("es") || subCommand.equals("enderstorage")) {
            return handleEnderStorage(sender, args);
        } else {
            MessageUtils.sendError(sender, "command.unknown-command", "&c✗ &f未知的子命令！输入 /{label} 查看帮助。", "label", label);
            return true;
        }
    }

    /**
     * 处理强化命令: /sn reinforce 或 /sn r [玩家名字]
     */
    private boolean handleReinforce(CommandSender sender, String[] args) {
        // 如果指定了玩家名字，需要权限检查
        if (args.length > 1) {
            // 只有有权限的玩家或控制台才能为其他玩家打开GUI
            boolean hasPermission = sender.hasPermission("mmoitemseditor.openforothers");
            boolean isOp = sender instanceof Player && ((Player) sender).isOp();
            boolean isConsole = sender instanceof org.bukkit.command.ConsoleCommandSender;
            
            if (!hasPermission && !isOp && !isConsole) {
                MessageUtils.sendError(sender, "command.no-permission", "&c✗ &f您没有权限为其他玩家打开强化界面！");
                return true;
            }

            String targetPlayerName = args[1];
            Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
            
            if (targetPlayer == null) {
                MessageUtils.sendError(sender, "command.player-not-found", "&c✗ &f找不到玩家: " + targetPlayerName);
                return true;
            }

            gui.openGUI(targetPlayer);
            MessageUtils.sendSuccess(sender, "command.open-gui-success", "&a✓ &f已为玩家 &e" + targetPlayerName + " &f打开物品强化界面！");
            return true;
        }

        // 没有指定玩家名字，为自己打开GUI
        if (!(sender instanceof Player)) {
            MessageUtils.sendError(sender, "command.player-only", "&c✗ &f此命令仅限玩家使用！请指定玩家名字：/{label} r <玩家名字>", "label", "sn");
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
        // 同步重载 EnderStorage 模块
        if (plugin.getEnderStorageModule() != null) {
            plugin.getEnderStorageModule().reload();
        }
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

    /**
     * 处理 EnderStorage 子命令: /sn es ...
     */
    private boolean handleEnderStorage(CommandSender sender, String[] args) {
        if (enderModule == null || enderModule.getEnderCommand() == null) {
            MessageUtils.sendError(sender, "command.feature-missing", "&c✗ &fEnderStorage 功能未启用");
            return true;
        }
        // 去除首个 "es"
        String[] forward = new String[Math.max(0, args.length - 1)];
        if (args.length > 1) {
            System.arraycopy(args, 1, forward, 0, args.length - 1);
        }
        // 校验基本格式：当子参数为空且非玩家则提示
        if (forward.length == 0 && !(sender instanceof Player)) {
            MessageUtils.sendError(sender, "enderstorage.player-only", "&c✗ &f此命令仅限玩家使用");
            return true;
        }
        boolean handled = enderModule.getEnderCommand().onCommand(sender, null, "snowterritory es", forward);
        if (!handled) {
            MessageUtils.sendError(sender, "enderstorage.usage", "&c用法: /sn es reload | /sn es give <player> <itemKey> <amount>");
        }
        return true;
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
            if ("es".startsWith(input) || "enderstorage".startsWith(input)) {
                completions.add("es");
            }
            
            return completions;
        }

        // /sn es 子命令补全
        if (args.length == 2 && (args[0].equalsIgnoreCase("es") || args[0].equalsIgnoreCase("enderstorage"))) {
            List<String> subs = new ArrayList<>();
            subs.add("reload");
            subs.add("give");
            return subs;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("es") && args[1].equalsIgnoreCase("give")) {
            List<String> playerNames = new ArrayList<>();
            String input = args[2].toLowerCase();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    playerNames.add(player.getName());
                }
            }
            return playerNames;
        }
        
        // 当输入 /sn r 后，补全玩家名字
        if (args.length == 2 && (args[0].equalsIgnoreCase("r") || args[0].equalsIgnoreCase("reinforce"))) {
            // 检查是否有权限为其他玩家打开GUI
            if (sender.hasPermission("mmoitemseditor.openforothers") || 
                (sender instanceof Player && ((Player) sender).isOp()) || 
                sender instanceof org.bukkit.command.ConsoleCommandSender) {
                
                List<String> playerNames = new ArrayList<>();
                String input = args[1].toLowerCase();
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        playerNames.add(player.getName());
                    }
                }
                
                return playerNames;
            }
        }
        
        return new ArrayList<>();
    }
}

