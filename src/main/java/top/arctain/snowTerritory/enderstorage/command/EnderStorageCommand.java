package top.arctain.snowTerritory.enderstorage.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.enderstorage.config.EnderStorageConfigManager;
import top.arctain.snowTerritory.enderstorage.config.MessageProvider;
import top.arctain.snowTerritory.enderstorage.service.LootStorageService;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class EnderStorageCommand implements CommandExecutor, TabCompleter {

    private final org.bukkit.plugin.Plugin plugin;
    private final LootStorageService service;
    private final MessageProvider messages;
    private final top.arctain.snowTerritory.enderstorage.gui.LootStorageGUI gui;

    public EnderStorageCommand(org.bukkit.plugin.Plugin plugin, EnderStorageConfigManager configManager, LootStorageService service, top.arctain.snowTerritory.enderstorage.gui.LootStorageGUI gui) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
        String lang = configManager.getMainConfig().getString("features.default-language", "zh_CN");
        this.messages = new MessageProvider(configManager.getMessagePacks(), lang);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return openSelf(sender);
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("st.loot.admin")) {
                    MessageUtils.sendConfigMessage(sender, "enderstorage.no-permission", messages.get(sender, "no-permission", "&c✗ &f没有权限"));
                    return true;
                }
                service.reload();
                MessageUtils.sendConfigMessage(sender, "enderstorage.reload-done", messages.get(sender, "reload-done", "&a✓ &f战利品仓库配置已重载"));
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("st.loot.admin")) {
                    MessageUtils.sendConfigMessage(sender, "enderstorage.no-permission", messages.get(sender, "no-permission", "&c✗ &f没有权限"));
                    return true;
                }
                return handleGive(sender, args);
            }
            default -> {
                return openSelf(sender);
            }
        }
    }

    private boolean openSelf(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "enderstorage.player-only", "&c✗ &f仅限玩家使用");
            return true;
        }
        if (!player.hasPermission("st.loot.use")) {
            MessageUtils.sendConfigMessage(sender, "enderstorage.no-permission", messages.get(sender, "no-permission", "&c✗ &f没有权限"));
            return true;
        }
        // 检查玩家是否在 trmenu 中，如果是，需要延迟更长时间以确保 trmenu 完全关闭
        // trmenu 的 ListenerBukkitInventory 会在 InventoryOpenEvent 时关闭菜单，并在 4 tick 后强制关闭窗口
        // 因此我们需要延迟至少 5 tick 来避免冲突
        long delay = isPlayerInTrMenu(player) ? 5L : 2L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 再次检查玩家是否在线，避免异步问题
            if (player.isOnline()) {
                gui.open(player, 1);
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
            if (Bukkit.getPluginManager().getPlugin("TrMenu") == null) {
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

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.sendConfigMessage(sender, "enderstorage.usage-give", "&c用法: /sn es give <player> <itemKey> <amount>");
            return true;
        }
        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtils.sendConfigMessage(sender, "enderstorage.player-not-found", "&c✗ &f玩家不存在");
            return true;
        }
        String itemKey = args[2];
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            MessageUtils.sendConfigMessage(sender, "enderstorage.invalid-amount", "&c✗ &f数量必须是数字");
            return true;
        }

        int perItemMax = service.resolvePerItemMax(target, itemKey);
        int slotLimit = service.resolveSlots(target);
        service.add(target.getUniqueId(), itemKey, amount, perItemMax, slotLimit);
        MessageUtils.sendConfigMessage(sender, "enderstorage.give-success", "&a✓ &f已发放 " + amount + "x " + itemKey + " 给 " + target.getName(), "amount", String.valueOf(amount), "itemKey", itemKey, "player", target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) list.add("reload");
            if ("give".startsWith(args[0].toLowerCase())) list.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            sender.getServer().getOnlinePlayers().forEach(p -> list.add(p.getName()));
        }
        return list;
    }
}

