package top.arctain.snowTerritory.stfish.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.stfish.data.FishDefinition;
import top.arctain.snowTerritory.stfish.data.FishTier;
import top.arctain.snowTerritory.stfish.service.FishItemFactory;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

/** 鱼类图鉴 GUI，分页展示所有鱼种。 */
public class FishAtlasGUI {

    public static final NamespacedKey KEY_ACTION = new NamespacedKey("snowterritory", "stfish_atlas");
    private static final int CONTENT_SLOTS = 45;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final StfishConfigManager configManager;
    private final FishItemFactory itemFactory;

    public FishAtlasGUI(StfishConfigManager configManager, FishItemFactory itemFactory) {
        this.configManager = configManager;
        this.itemFactory = itemFactory;
    }

    public void open(Player player, int page) {
        List<FishDefinition> allFish = configManager.getAllFishOrdered();
        int totalPages = Math.max(1, (int) Math.ceil(allFish.size() / (double) CONTENT_SLOTS));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int start = (currentPage - 1) * CONTENT_SLOTS;
        int end = Math.min(allFish.size(), start + CONTENT_SLOTS);

        String title = MessageUtils.getConfigMessage("stfish.atlas-title", "&6鱼类图鉴 &8- &7第 {page}/{total} 页",
                "page", String.valueOf(currentPage), "total", String.valueOf(totalPages));
        Inventory inv = Bukkit.createInventory(new FishAtlasHolder(currentPage), 54, ColorUtils.colorize(title));

        for (int i = start; i < end; i++) {
            FishDefinition def = allFish.get(i);
            FishTier tier = getTierForFish(def);
            if (tier == null) continue;
            double midLength = (def.lengthMin() + def.lengthMax()) / 2;
            ItemStack display = itemFactory.create(def, midLength, tier);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
                meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "fish:" + def.id());
                display.setItemMeta(meta);
            }
            inv.setItem(i - start, display);
        }

        if (currentPage > 1) {
            inv.setItem(PREV_SLOT, navItem(Material.ARROW, "&e上一页", "page:" + (currentPage - 1)));
        }
        if (currentPage < totalPages) {
            inv.setItem(NEXT_SLOT, navItem(Material.ARROW, "&e下一页", "page:" + (currentPage + 1)));
        }

        player.openInventory(inv);
    }

    private FishTier getTierForFish(FishDefinition def) {
        for (var e : configManager.getFishByTier().entrySet()) {
            if (e.getValue().stream().anyMatch(f -> f.id().equals(def.id()))) {
                return e.getKey();
            }
        }
        return null;
    }

    private ItemStack navItem(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class FishAtlasHolder implements InventoryHolder {
        private final int page;

        public FishAtlasHolder(int page) {
            this.page = page;
        }

        public int getPage() {
            return page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
