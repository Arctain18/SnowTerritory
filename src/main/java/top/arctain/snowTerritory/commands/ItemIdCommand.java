package top.arctain.snowTerritory.commands;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.utils.Utils;

public class ItemIdCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mmoitemseditor.itemid") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "您没有权限使用此命令！");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        
        // 检查是否为空
        if (item == null || item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "请手持一个物品！");
            return true;
        }

        // 检查是否为 MMOItems 物品
        if (!Utils.isMMOItem(item)) {
            player.sendMessage(ChatColor.YELLOW + "这不是一个 MMOItems 物品！");
            return true;
        }

        // 获取物品信息
        try {
            net.Indyuce.mmoitems.api.Type type = MMOItems.getType(item);
            String id = MMOItems.getID(item);
            
            if (type == null || id == null || id.isEmpty()) {
                player.sendMessage(ChatColor.RED + "无法获取物品信息！");
                return true;
            }

            // 发送信息给玩家
            player.sendMessage(ChatColor.GREEN + "========== 物品信息 ==========");
            player.sendMessage(ChatColor.GOLD + "物品类型: " + ChatColor.WHITE + type.getId());
            player.sendMessage(ChatColor.GOLD + "物品 ID: " + ChatColor.WHITE + id);
            player.sendMessage(ChatColor.GREEN + "============================");
            
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "获取物品信息时发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}

