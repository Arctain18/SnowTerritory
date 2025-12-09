package top.arctain.snowTerritory.commands;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.Utils;

public class ItemIdCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendError(sender, "command.player-only", "&c✗ &f此命令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mmoitemseditor.itemid") && !player.isOp()) {
            MessageUtils.sendError(player, "command.no-permission", "&c✗ &f您没有权限使用此命令！");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        
        // 检查是否为空
        if (item == null || item.getType().isAir()) {
            MessageUtils.sendError(player, "item.no-item", "&c✗ &f请手持一个物品！");
            return true;
        }

        // 检查是否为 MMOItems 物品
        if (!Utils.isMMOItem(item)) {
            MessageUtils.sendWarning(player, "item.not-mmoitem", "&e⚠ &f这不是一个 MMOItems 物品！");
            return true;
        }

        // 获取物品信息
        try {
            net.Indyuce.mmoitems.api.Type type = MMOItems.getType(item);
            String id = MMOItems.getID(item);
            
            if (type == null || id == null || id.isEmpty()) {
                MessageUtils.sendError(player, "item.info-error", "&c✗ &f无法获取物品信息！");
                return true;
            }

            // 发送信息给玩家
            MessageUtils.sendItemInfo(player, type.getId(), id);
            
        } catch (Exception e) {
            MessageUtils.sendError(player, "item.info-error", "&c✗ &f获取物品信息时发生错误: &e{error}", "error", e.getMessage());
            MessageUtils.logError("获取物品信息时发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}

