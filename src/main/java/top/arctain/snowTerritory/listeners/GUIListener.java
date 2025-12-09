package top.arctain.snowTerritory.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.gui.ItemEditorGUI;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.Arrays;

public class GUIListener implements Listener {

    private final PluginConfig config;
    private final ItemEditorGUI guiHandler;

    public GUIListener(PluginConfig config) {
        this.config = config;
        this.guiHandler = new ItemEditorGUI(config);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        InventoryView view = event.getView();
        Inventory inv = event.getInventory();

        // 检查是否为我们的GUI（使用 InventoryView 获取标题）
        String title = ChatColor.stripColor(view.getTitle());
        String configTitle = ChatColor.stripColor(config.getGuiTitle());
        
        if (!title.equals(configTitle)) {
            return;
        }

        int slot = event.getRawSlot();
        
        // 允许编辑的槽位：武器、保护符、强化符、材料槽
        boolean isEditableSlot = slot == config.getSlotWeapon() 
                || slot == config.getSlotProtectCharm()
                || slot == config.getSlotEnhanceCharm() 
                || Arrays.stream(config.getSlotMaterials()).anyMatch(s -> s == slot);
        
        // 如果不是可编辑槽位且在GUI范围内，取消事件
        if (!isEditableSlot && slot < config.getGuiSize()) {
            event.setCancelled(true);
        }

        // 处理按钮点击
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        
        String itemName = ChatColor.stripColor(meta.getDisplayName());
        String confirmName = ChatColor.stripColor(config.getGuiConfirmButtonName());
        String cancelName = ChatColor.stripColor(config.getGuiCancelButtonName());

        if (itemName.equals(confirmName)) {
            guiHandler.applyReinforce(player, inv);
            event.setCancelled(true);
        } else if (itemName.equals(cancelName)) {
            player.closeInventory();
            MessageUtils.sendWarning(player, "reinforce.cancelled", "&e⚠ &f强化已取消。");
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
        String title = ChatColor.stripColor(view.getTitle());
        String configTitle = ChatColor.stripColor(config.getGuiTitle());
        
        if (title.equals(configTitle)) {
            // 关闭GUI时，将物品返还给玩家（可选功能）
            // 这里可以添加物品返还逻辑
        }
    }
}