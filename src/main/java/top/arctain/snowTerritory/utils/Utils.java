package top.arctain.snowTerritory.utils;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class Utils {

    private static final Random RANDOM = new Random();

    /**
     * 尝试强化，根据概率返回结果（成功/失败/维持）
     */
    public static ReinforceResult attemptReinforce(double successRate, double failDegradeChance, double maintainChance) {
        double roll = RANDOM.nextDouble();
        if (roll <= successRate) return ReinforceResult.SUCCESS;
        if (roll <= successRate + maintainChance) return ReinforceResult.MAINTAIN;
        if (roll <= successRate + maintainChance + failDegradeChance) return ReinforceResult.FAIL_DEGRADE;
        return ReinforceResult.MAINTAIN;  // 默认维持
    }

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

    /**
     * 从 ItemStack 获取 MMOItem
     */
    public static MMOItem getMMOItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        try {
            net.Indyuce.mmoitems.api.Type type = MMOItems.getType(item);
            String id = MMOItems.getID(item);
            if (type == null || id == null || id.isEmpty()) {
                return null;
            }
            return MMOItems.plugin.getMMOItem(type, id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从玩家手中获取 MMOItem
     */
    public static MMOItem getHeldMMOItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return getMMOItem(item);
    }

    /**
     * 检查物品是否可强化（基于物品lore中是否包含"可强化"）
     */
    public static boolean isReinforceable(ItemStack item) {
        if (item == null) return false;
        if (!isMMOItem(item)) return false;  // 必须是 MMOItems 物品
        
        // 检查物品是否有lore
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        // 检查lore中是否包含"可强化"
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        for (String line : lore) {
            if (line != null && line.contains("可强化")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前强化等级（从名字后缀解析，如 +1）
     */
    public static int getCurrentLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return 0;
        
        String name = meta.getDisplayName();
        // 匹配 " +数字" 格式
        if (name.matches(".* \\+\\d+")) {
            try {
                String levelStr = name.substring(name.lastIndexOf("+") + 1).trim();
                return Integer.parseInt(levelStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 更新物品名字添加 +N
     */
    public static void updateItemName(ItemStack item, int newLevel) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        String currentName = meta.hasDisplayName() ? meta.getDisplayName() : "";
        // 移除旧的等级后缀
        String baseName = currentName.replaceAll(" \\+\\d+", "").trim();
        if (baseName.isEmpty()) {
            baseName = "未命名物品";
        }
        meta.setDisplayName(baseName + (newLevel > 0 ? " +" + newLevel : ""));
        item.setItemMeta(meta);
    }

    /**
     * 修改 MMOItems 属性（按百分比）
     * @param mmoItem LiveMMOItem实例
     * @param multiplier 属性倍数（例如1.1表示+10%，0.9表示-10%）
     * @param reinforceableAttributes 可强化属性列表（属性ID列表，例如: ATTACK_DAMAGE, DEFENSE）
     */
    public static void modifyMMOAttribute(LiveMMOItem mmoItem, double multiplier, List<String> reinforceableAttributes) {
        if (mmoItem == null) return;
        if (reinforceableAttributes == null || reinforceableAttributes.isEmpty()) return;
        
        try {
            // 获取所有有数据的统计类型
            @SuppressWarnings("rawtypes")
            Set<ItemStat> statsSet = mmoItem.getStats();
            
            if (statsSet == null || statsSet.isEmpty()) return;
            
            // 遍历所有有数据的统计类型
            for (@SuppressWarnings("rawtypes") ItemStat itemStat : statsSet) {
                if (itemStat == null) continue;
                
                // 检查该属性是否在可强化列表中
                String statId = itemStat.getId();
                if (!reinforceableAttributes.contains(statId)) {
                    continue;
                }
                
                try {
                    // 获取StatData
                    StatData statData = mmoItem.getData(itemStat);
                    if (statData == null) continue;
                    
                    // 根据属性类型修改值
                    if (statData instanceof DoubleData) {
                        DoubleData doubleData = (DoubleData) statData;
                        double currentValue = doubleData.getValue();
                        double newValue = currentValue * multiplier;
                        
                        // 创建新的DoubleData并设置
                        DoubleData newStatData = new DoubleData(newValue);
                        mmoItem.setData(itemStat, newStatData);
                    }
                } catch (Exception e) {
                    // 如果处理某个属性时出错，继续处理下一个
                    continue;
                }
            }
        } catch (Exception e) {
            // 如果出现异常，记录错误但不中断程序
            System.err.println("修改MMOItems属性时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从LiveMMOItem获取更新后的ItemStack
     * @param mmoItem LiveMMOItem实例
     * @return 更新后的ItemStack
     */
    public static ItemStack getUpdatedItemStack(LiveMMOItem mmoItem) {
        if (mmoItem == null) return null;
        
        try {
            // 使用newBuilder().build()方法构建新的ItemStack
            // 这是MMOItems API推荐的方式
            return mmoItem.newBuilder().build();
        } catch (Exception e) {
            System.err.println("获取更新后的ItemStack时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 强化结果枚举
     */
    public enum ReinforceResult {
        SUCCESS,      // 成功
        FAIL_DEGRADE, // 失败并降级
        MAINTAIN      // 维持不变
    }
}