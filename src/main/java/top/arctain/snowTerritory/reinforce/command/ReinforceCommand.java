package top.arctain.snowTerritory.reinforce.command;

import org.bukkit.Bukkit;
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

            // 检查玩家是否在 trmenu 中，如果是，需要延迟更长时间以确保 trmenu 完全关闭
            long delay = isPlayerInTrMenu(targetPlayer) ? 5L : 2L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (targetPlayer.isOnline()) {
                    gui.openGUI(targetPlayer);
                }
            }, delay);
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

        // 检查玩家是否在 trmenu 中，如果是，需要延迟更长时间以确保 trmenu 完全关闭
        long delay = isPlayerInTrMenu(player) ? 5L : 2L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                gui.openGUI(player);
            }
        }, delay);
        return true;
    }

    /**
     * 检查玩家是否在 trmenu 中
     * 通过检查玩家当前打开的 Inventory 的 holder 类型来判断
     */
    private boolean isPlayerInTrMenu(Player player) {
        try {
            // 检查 trmenu 是否已加载
            if (plugin.getServer().getPluginManager().getPlugin("TrMenu") == null) {
                return false;
            }
            // 如果玩家打开了 GUI，检查是否是 trmenu 的 GUI
            if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
                // trmenu 使用特定的 holder 类型，通过反射检查
                org.bukkit.inventory.InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
                if (holder != null) {
                    String className = holder.getClass().getName();
                    // trmenu 的 holder 类名包含 "trplugins.menu" 或 "StaticInventory"
                    if (className.contains("trplugins.menu") || className.contains("StaticInventory")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 如果检查失败，假设不在 trmenu 中，使用较短的延迟
        }
        return false;
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

