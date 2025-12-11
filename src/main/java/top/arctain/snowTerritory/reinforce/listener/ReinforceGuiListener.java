package top.arctain.snowTerritory.reinforce.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.reinforce.gui.ReinforceGUI;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReinforceGuiListener implements Listener {

    private final ReinforceConfigManager config;
    private final ReinforceGUI guiHandler;
    private final Main plugin;

    public ReinforceGuiListener(ReinforceConfigManager config, Main plugin, ReinforceGUI guiHandler) {
        this.config = config;
        this.plugin = plugin;
        this.guiHandler = guiHandler;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();
        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();

        // 检查是否为我们的GUI（使用 InventoryView 获取标题）
        String title = ColorUtils.stripColor(view.getTitle());
        String configTitle = ColorUtils.stripColor(config.getGuiTitle());
        
        if (!title.equals(configTitle)) {
            return;
        }

        int slot = event.getRawSlot();
        
        // 如果点击的不是GUI，而是玩家物品栏，允许操作
        if (clickedInv == null || clickedInv != topInv) {
            return;
        }
        
        // 先处理按钮点击（确认和取消按钮）- 直接通过槽位判断，更可靠
        if (slot == config.getSlotConfirm()) {
            event.setCancelled(true);
            guiHandler.applyReinforce(player, topInv);
            return;
        } else if (slot == config.getSlotCancel()) {
            event.setCancelled(true);
            player.closeInventory();
            MessageUtils.sendWarning(player, "reinforce.cancelled", "&e⚠ &f强化已取消。");
            return;
        }
        
        // 允许编辑的槽位：武器、保护符、强化符、材料槽
        boolean isEditableSlot = slot == config.getSlotWeapon() 
                || slot == config.getSlotProtectCharm()
                || slot == config.getSlotEnhanceCharm() 
                || Arrays.stream(config.getSlotMaterials()).anyMatch(s -> s == slot);
        
        // 如果不是可编辑槽位，取消事件
        if (!isEditableSlot) {
            event.setCancelled(true);
            return;
        }
        
        // 如果是可编辑槽位，在物品放入后更新确认按钮的lore
        // 使用调度器延迟执行，确保物品已经放入
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory() == topInv) {
                guiHandler.updateConfirmButtonLore(player, topInv);
            }
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();
        Inventory topInv = event.getView().getTopInventory();

        // 检查是否为我们的GUI
        String title = ColorUtils.stripColor(view.getTitle());
        String configTitle = ColorUtils.stripColor(config.getGuiTitle());
        
        if (!title.equals(configTitle)) {
            return;
        }
        
        // 检查是否拖拽到了可编辑槽位
        boolean isEditableSlot = event.getRawSlots().stream().anyMatch(slot -> 
            slot == config.getSlotWeapon() 
            || slot == config.getSlotProtectCharm()
            || slot == config.getSlotEnhanceCharm() 
            || Arrays.stream(config.getSlotMaterials()).anyMatch(s -> s == slot)
        );
        
        if (isEditableSlot) {
            // 在拖拽完成后更新确认按钮的lore
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory().getTopInventory() == topInv) {
                    guiHandler.updateConfirmButtonLore(player, topInv);
                }
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        InventoryView view = event.getView();
        Inventory inv = event.getInventory();

        // 检查是否为我们的GUI（使用 InventoryView 获取标题）
        String title = ColorUtils.stripColor(view.getTitle());
        String configTitle = ColorUtils.stripColor(config.getGuiTitle());
        
        if (title.equals(configTitle)) {
            // 关闭GUI时，将物品返还给玩家
            returnItemsToPlayer(player, inv);
        }
    }
    
    /**
     * 将GUI中的物品返还给玩家
     * 如果背包满了，将物品扔到地上并发出警告
     */
    private void returnItemsToPlayer(Player player, Inventory gui) {
        List<ItemStack> itemsToReturn = new ArrayList<>();
        
        // 收集所有可编辑槽位的物品
        // 武器槽位
        ItemStack weapon = gui.getItem(config.getSlotWeapon());
        if (weapon != null && !weapon.getType().isAir()) {
            itemsToReturn.add(weapon);
        }
        
        // 保护符槽位
        ItemStack protectCharm = gui.getItem(config.getSlotProtectCharm());
        if (protectCharm != null && !protectCharm.getType().isAir()) {
            itemsToReturn.add(protectCharm);
        }
        
        // 强化符槽位
        ItemStack enhanceCharm = gui.getItem(config.getSlotEnhanceCharm());
        if (enhanceCharm != null && !enhanceCharm.getType().isAir()) {
            itemsToReturn.add(enhanceCharm);
        }
        
        // 材料槽位
        for (int materialSlot : config.getSlotMaterials()) {
            ItemStack material = gui.getItem(materialSlot);
            if (material != null && !material.getType().isAir()) {
                itemsToReturn.add(material);
            }
        }
        
        // 如果没有物品需要返还，直接返回
        if (itemsToReturn.isEmpty()) {
            return;
        }
        
        // 尝试将物品添加到玩家背包
        List<ItemStack> droppedItems = new ArrayList<>();
        for (ItemStack item : itemsToReturn) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            
            // 尝试添加到背包
            ItemStack remaining = addItemToInventory(player, item);
            
            // 如果还有剩余物品，说明背包满了
            if (remaining != null && !remaining.getType().isAir()) {
                droppedItems.add(remaining);
            }
        }
        
        // 如果有物品无法放入背包，扔到地上
        if (!droppedItems.isEmpty()) {
            for (ItemStack item : droppedItems) {
                if (item != null && !item.getType().isAir()) {
                    Item droppedItem = player.getWorld().dropItemNaturally(player.getLocation(), item);
                    droppedItem.setPickupDelay(0); // 允许立即拾取
                }
            }
            
            // 发送警告消息
            MessageUtils.sendWarning(player, "gui.inventory-full", 
                "&e⚠ &f背包已满！部分物品已掉落在地上。");
        }
    }
    
    /**
     * 尝试将物品添加到玩家背包
     * @param player 玩家
     * @param item 要添加的物品
     * @return 如果背包满了，返回剩余物品；如果全部添加成功，返回null
     */
    private ItemStack addItemToInventory(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        
        // 尝试添加到背包，返回无法添加的物品（如果有）
        java.util.Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
        
        // 如果map为空，说明全部添加成功
        if (remaining == null || remaining.isEmpty()) {
            return null;
        }
        
        // 返回第一个剩余物品（通常只有一个）
        return remaining.values().iterator().next();
    }
}

