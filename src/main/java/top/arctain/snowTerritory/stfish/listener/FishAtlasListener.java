package top.arctain.snowTerritory.stfish.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import top.arctain.snowTerritory.stfish.gui.FishAtlasGUI;

/** 图鉴 GUI 点击监听，仅处理翻页。 */
public class FishAtlasListener implements Listener {

    private final FishAtlasGUI gui;
    private final org.bukkit.plugin.Plugin plugin;

    public FishAtlasListener(org.bukkit.plugin.Plugin plugin, FishAtlasGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FishAtlasGUI.FishAtlasHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String action = clicked.getItemMeta().getPersistentDataContainer()
                .get(FishAtlasGUI.KEY_ACTION, PersistentDataType.STRING);
        if (action == null) return;

        if (action.startsWith("page:")) {
            int targetPage = Integer.parseInt(action.substring(5));
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, targetPage));
        }
    }
}
