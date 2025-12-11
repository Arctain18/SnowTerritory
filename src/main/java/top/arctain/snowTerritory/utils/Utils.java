package top.arctain.snowTerritory.utils;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.inventory.ItemStack;

/**
 * 通用工具类
 * 注意：reinforce相关的工具方法已移至 ReinforceUtils
 */
public class Utils {

    /**
     * 检查物品是否为 MMOItems 物品
     */
    public static boolean isMMOItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        try {
            // 使用 MMOItems 的静态方法检查
            net.Indyuce.mmoitems.api.Type type = MMOItems.getType(item);
            String id = MMOItems.getID(item);
            return type != null && id != null && !id.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
