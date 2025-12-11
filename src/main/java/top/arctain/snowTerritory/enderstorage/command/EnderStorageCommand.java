package top.arctain.snowTerritory.enderstorage.command;

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

    private final EnderStorageConfigManager configManager;
    private final LootStorageService service;
    private final MessageProvider messages;

    public EnderStorageCommand(EnderStorageConfigManager configManager, LootStorageService service) {
        this.configManager = configManager;
        this.service = service;
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
                    MessageUtils.sendError(sender, "enderstorage.no-permission", messages.get(sender, "no-permission", "&c✗ &f没有权限"));
                    return true;
                }
                service.reload();
                MessageUtils.sendSuccess(sender, "enderstorage.reload", messages.get(sender, "reload-done", "&a✓ &f战利品仓库配置已重载"));
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("st.loot.admin")) {
                    MessageUtils.sendError(sender, "enderstorage.no-permission", messages.get(sender, "no-permission", "&c✗ &f没有权限"));
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
            MessageUtils.sendError(sender, "enderstorage.player-only", "&c✗ &f仅限玩家使用");
            return true;
        }
        if (!player.hasPermission("st.loot.use")) {
            MessageUtils.sendError(sender, "enderstorage.no-permission", messages.get(sender, "no-permission", "&c✗ &f没有权限"));
            return true;
        }
        // TODO: GUI 实装
        MessageUtils.sendSuccess(player, "enderstorage.open", messages.get(sender, "open", "&a✓ &f打开战利品仓库（GUI 开发中）"));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.sendError(sender, "enderstorage.usage", "&c用法: /" + sender.getName() + " give <player> <itemKey> <amount>");
            return true;
        }
        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtils.sendError(sender, "enderstorage.player-not-found", "&c✗ &f玩家不存在");
            return true;
        }
        String itemKey = args[2];
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            MessageUtils.sendError(sender, "enderstorage.amount-invalid", "&c✗ &f数量必须是数字");
            return true;
        }

        int perItemMax = service.resolvePerItemMax(target, itemKey);
        int slotLimit = service.resolveSlots(target);
        service.add(target.getUniqueId(), itemKey, amount, perItemMax, slotLimit);
        MessageUtils.sendSuccess(sender, "enderstorage.given", "&a✓ &f已发放 " + amount + "x " + itemKey + " 给 " + target.getName());
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

