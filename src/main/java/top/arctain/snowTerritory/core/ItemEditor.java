package top.arctain.snowTerritory.core;

import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.utils.Utils;

/**
 * 物品编辑器核心类
 * 提供物品强化、属性修改等功能
 */
public class ItemEditor {

    /**
     * 强化物品
     * @param item 要强化的物品
     * @param successRate 成功率
     * @param failDegradeChance 失败降级概率
     * @param maintainChance 维持概率
     * @param boostPercent 成功时属性提升百分比
     * @param reducePercent 失败时属性降低百分比
     * @param reinforceableAttributes 可强化属性列表
     * @return 强化结果
     */
    public static Utils.ReinforceResult reinforceItem(ItemStack item, 
                                                      double successRate, 
                                                      double failDegradeChance, 
                                                      double maintainChance,
                                                      double boostPercent,
                                                      double reducePercent,
                                                      java.util.List<String> reinforceableAttributes) {
        if (item == null || !Utils.isMMOItem(item)) {
            return null;
        }

        int currentLevel = Utils.getCurrentLevel(item);
        Utils.ReinforceResult result = Utils.attemptReinforce(successRate, failDegradeChance, maintainChance);

        try {
            LiveMMOItem mmoItem = new LiveMMOItem(item);
            
            switch (result) {
                case SUCCESS:
                    Utils.modifyMMOAttribute(mmoItem, boostPercent, reinforceableAttributes);
                    Utils.updateItemName(item, currentLevel + 1);
                    break;
                case FAIL_DEGRADE:
                    int newLevel = Math.max(0, currentLevel - 1);
                    Utils.modifyMMOAttribute(mmoItem, reducePercent, reinforceableAttributes);
                    Utils.updateItemName(item, newLevel);
                    break;
                case MAINTAIN:
                    // 维持不变，不需要修改
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return result;
    }
}
