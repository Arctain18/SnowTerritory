package top.arctain.snowTerritory.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.gui.ItemEditorGUI;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.Arrays;

public class GUIListener implements Listener {

    private final PluginConfig config;
    private final ItemEditorGUI guiHandler;

    public GUIListener(PluginConfig config) {
        this.config = config;
        this.guiHandler = new ItemEditorGUI(config);
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
            // 关闭GUI时，将物品返还给玩家（可选功能）
            // 这里可以添加物品返还逻辑
        }
    }
}