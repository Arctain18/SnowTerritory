package top.arctain.snowTerritory.enderstorage.gui;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
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
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.enderstorage.config.EnderStorageConfigManager;
import top.arctain.snowTerritory.enderstorage.config.MessageProvider;
import top.arctain.snowTerritory.enderstorage.config.WhitelistEntry;
import top.arctain.snowTerritory.enderstorage.service.LootStorageService;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LootStorageGUI {

    public static final NamespacedKey KEY_ITEM = new NamespacedKey("snowterritory", "enderstorage_item");

    private final LootStorageService service;
    private final MessageProvider messages;

    public LootStorageGUI(Main plugin, EnderStorageConfigManager configManager, LootStorageService service) {
        this.service = service;
        String lang = configManager.getMainConfig().getString("features.default-language", "zh_CN");
        this.messages = new MessageProvider(configManager.getMessagePacks(), lang);
    }

    public void open(Player player, int page) {
        Map<String, Integer> data = service.getAll(player.getUniqueId());
        List<String> keys = service.getWhitelistEntries().stream().map(WhitelistEntry::getKey).collect(Collectors.toList());
        int perPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil(keys.size() / (double) perPage));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int start = (currentPage - 1) * perPage;
        int end = Math.min(keys.size(), start + perPage);
        Inventory inv = Bukkit.createInventory(new LootHolder(player.getUniqueId(), currentPage), 54,
                MessageUtils.colorize(messages.get(player, "gui-title", "战利品仓库 (" + currentPage + "/" + totalPages + ")")));

        for (int i = start; i < end; i++) {
            String key = keys.get(i);
            WhitelistEntry entry = service.getWhitelistEntry(key);
            int amount = data.getOrDefault(key, 0);
            ItemStack display = buildDisplayItem(entry, amount);
            inv.setItem(i - start, display);
        }
        // 翻页按钮
        inv.setItem(45, navItem(Material.ARROW, "上一页", currentPage > 1 ? currentPage - 1 : currentPage));
        inv.setItem(53, navItem(Material.ARROW, "下一页", currentPage < totalPages ? currentPage + 1 : currentPage));

        player.openInventory(inv);
    }

    private ItemStack navItem(Material material, String name, int targetPage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.colorize("&e" + name));
            meta.getPersistentDataContainer().set(KEY_ITEM, PersistentDataType.STRING, "page:" + targetPage);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildDisplayItem(WhitelistEntry entry, int amount) {
        ItemStack base = buildRealItem(entry);
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.colorize(entry.getDisplay()));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtils.colorize("&7数量: &e" + amount + " / " + entry.getDefaultMax()));
            lore.add(MessageUtils.colorize("&8| &7左键 ▸ 存入 8"));
            lore.add(MessageUtils.colorize("&8| &7SHIFT+左键 ▸ 存入 64"));
            lore.add(MessageUtils.colorize("&8| &7右键 ▸ 取出 8"));
            lore.add(MessageUtils.colorize("&8| &7SHIFT+右键 ▸ 取出 64"));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(KEY_ITEM, PersistentDataType.STRING, entry.getKey());
            base.setItemMeta(meta);
        }
        return base;
    }

    public ItemStack buildRealItem(WhitelistEntry entry) {
        if (entry.getMmoItemId() != null && !entry.getMmoItemId().isEmpty() && entry.getMmoType() != null && !entry.getMmoType().isEmpty()) {
            try {
                Type type = Type.get(entry.getMmoType());
                if (type != null) {
                    MMOItem item = MMOItems.plugin.getMMOItem(type, entry.getMmoItemId());
                    if (item != null) {
                        ItemStack stack = item.newBuilder().build();
                        stack.setAmount(1);
                        return stack;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return new ItemStack(entry.getMaterial() == null ? Material.STONE : entry.getMaterial());
    }

    public static class LootHolder implements InventoryHolder {
        private final java.util.UUID playerId;
        private final int page;

        public LootHolder(java.util.UUID playerId, int page) {
            this.playerId = playerId;
            this.page = page;
        }

        public java.util.UUID getPlayerId() {
            return playerId;
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

