package top.arctain.snowTerritory.enderstorage.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import top.arctain.snowTerritory.enderstorage.config.WhitelistEntry;
import top.arctain.snowTerritory.enderstorage.gui.LootStorageGUI;
import top.arctain.snowTerritory.enderstorage.gui.LootStorageGUI.LootHolder;
import top.arctain.snowTerritory.enderstorage.service.LootStorageService;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.Utils;

public class LootGuiListener implements Listener {

    private final LootStorageGUI gui;
    private final LootStorageService service;
    private final org.bukkit.plugin.Plugin plugin;

    public LootGuiListener(org.bukkit.plugin.Plugin plugin, LootStorageGUI gui, LootStorageService service) {
        this.plugin = plugin;
        this.gui = gui;
        this.service = service;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof LootHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) {
            return;
        }
        String key = clicked.getItemMeta().getPersistentDataContainer().get(LootStorageGUI.KEY_ITEM, PersistentDataType.STRING);
        if (key == null) {
            return;
        }
        // 翻页
        if (key.startsWith("page:")) {
            int target = Integer.parseInt(key.split(":")[1]);
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, target));
            return;
        }

        WhitelistEntry entry = service.getWhitelistEntry(key);
        if (entry == null) {
            return;
        }
        int amount = clickAmount(event.getClick());
        if (amount == 0) {
            return;
        }

        if (isDeposit(event.getClick())) {
            int moved = removeFromInventory(player, key, amount);
            if (moved <= 0) {
                MessageUtils.sendConfigMessage(player, "enderstorage.no-items-to-deposit", "&e没有可存入的物品");
                return;
            }
            int perItemMax = service.resolvePerItemMax(player, key);
            int slotLimit = service.resolveSlots(player);
            service.add(player.getUniqueId(), key, moved, perItemMax, slotLimit);
            MessageUtils.sendConfigMessage(player, "enderstorage.deposit-success", "&a✓ &f存入 " + moved + "x " + entry.getDisplay(), "amount", String.valueOf(moved), "item", entry.getDisplay());
        } else {
            // 取出
            int current = service.getAmount(player.getUniqueId(), key);
            if (current <= 0) {
                MessageUtils.sendConfigMessage(player, "enderstorage.no-items-in-storage", "&e仓库中没有该物品");
                return;
            }
            int take = Math.min(amount, current);
            ItemStack give = guiBuild(entry, take);
            if (give == null) return;
            service.consume(player.getUniqueId(), key, take);
            player.getInventory().addItem(give);
            MessageUtils.sendConfigMessage(player, "enderstorage.withdraw-success", "&a✓ &f取出 " + take + "x " + entry.getDisplay(), "amount", String.valueOf(take), "item", entry.getDisplay());
        }
        Bukkit.getScheduler().runTask(plugin, () -> gui.open(player, holder.getPage()));
    }

    private ItemStack guiBuild(WhitelistEntry entry, int amount) {
        ItemStack stack = gui.buildRealItem(entry);
        if (stack == null) return null;
        stack.setAmount(Math.min(amount, stack.getMaxStackSize()));
        return stack;
    }

    private boolean isDeposit(ClickType type) {
        return type == ClickType.LEFT || type == ClickType.SHIFT_LEFT;
    }

    private int clickAmount(ClickType type) {
        if (type == ClickType.LEFT || type == ClickType.RIGHT) {
            return 8;
        }
        if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT) {
            return 64;
        }
        return 0;
    }

    private int removeFromInventory(Player player, String key, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            // 必须先检查是否是MMOItems物品
            if (!Utils.isMMOItem(item)) {
                continue;
            }
            // 然后检查是否在whitelist中
            String match = service.matchItemKey(item);
            if (match == null || !match.equals(key)) continue;
            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            remaining -= take;
            if (remaining <= 0) break;
        }
        return amount - remaining;
    }
}

