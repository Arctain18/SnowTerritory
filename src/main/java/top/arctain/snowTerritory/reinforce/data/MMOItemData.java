package top.arctain.snowTerritory.reinforce.data;

import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.reinforce.utils.ReinforceUtils;

/**
 * MMOItems 数据封装类
 * 用于存储和操作 MMOItems 物品的数据
 */
public class MMOItemData {

    private final ItemStack itemStack;
    private final MMOItem mmoItem;
    private int reinforceLevel;

    public MMOItemData(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.mmoItem = ReinforceUtils.getMMOItem(itemStack);
        this.reinforceLevel = ReinforceUtils.getCurrentLevel(itemStack);
    }

    /**
     * 获取原始 ItemStack
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * 获取 MMOItem
     */
    public MMOItem getMMOItem() {
        return mmoItem;
    }

    /**
     * 获取强化等级
     */
    public int getReinforceLevel() {
        return reinforceLevel;
    }

    /**
     * 设置强化等级
     */
    public void setReinforceLevel(int level) {
        this.reinforceLevel = level;
        ReinforceUtils.updateItemName(itemStack, level);
    }

    /**
     * 检查是否为有效的 MMOItem
     */
    public boolean isValid() {
        return mmoItem != null && itemStack != null;
    }

    /**
     * 检查是否可强化
     */
    public boolean isReinforceable() {
        return ReinforceUtils.isReinforceable(itemStack);
    }
}

