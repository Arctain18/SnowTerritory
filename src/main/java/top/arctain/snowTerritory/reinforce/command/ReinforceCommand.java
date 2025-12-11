package top.arctain.snowTerritory.reinforce.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.gui.ReinforceGUI;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class ReinforceCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ReinforceGUI gui;

    public ReinforceCommand(Main plugin, ReinforceGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 如果指定了玩家名字，需要权限检查
        if (args.length > 0) {
            // 只有有权限的玩家或控制台才能为其他玩家打开GUI
            boolean hasPermission = sender.hasPermission("mmoitemseditor.openforothers");
            boolean isOp = sender instanceof Player && ((Player) sender).isOp();
            boolean isConsole = sender instanceof org.bukkit.command.ConsoleCommandSender;
            
            if (!hasPermission && !isOp && !isConsole) {
                MessageUtils.sendError(sender, "command.no-permission", "&c✗ &f您没有权限为其他玩家打开强化界面！");
                return true;
            }

            String targetPlayerName = args[0];
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 当输入 /sn r 后，补全玩家名字
        if (args.length == 1) {
            // 检查是否有权限为其他玩家打开GUI
            if (sender.hasPermission("mmoitemseditor.openforothers") || 
                (sender instanceof Player && ((Player) sender).isOp()) || 
                sender instanceof org.bukkit.command.ConsoleCommandSender) {
                
                List<String> playerNames = new ArrayList<>();
                String input = args[0].toLowerCase();
                
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

