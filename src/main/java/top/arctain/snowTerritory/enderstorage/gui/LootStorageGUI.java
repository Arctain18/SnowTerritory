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
import top.arctain.snowTerritory.config.ModuleMessageProvider;
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
    private final ModuleMessageProvider messages;
    private final EnderStorageConfigManager configManager;

    public LootStorageGUI(Main plugin, EnderStorageConfigManager configManager, LootStorageService service) {
        this.service = service;
        this.configManager = configManager;
        String lang = configManager.getMainConfig().getString("features.default-language", "zh_CN");
        this.messages = new ModuleMessageProvider(configManager.getMessagePacks(), lang);
    }

    public void open(Player player, int page) {
        Map<String, Integer> data = service.getAll(player.getUniqueId());
        List<String> orderedKeys = configManager.getGuiMaterialKeys();

        // 如果 gui.yml 未配置 materials，则退回到白名单顺序
        if (orderedKeys.isEmpty()) {
            orderedKeys = service.getWhitelistEntries().stream().map(WhitelistEntry::getKey).collect(Collectors.toList());
        }

        List<Integer> materialSlots = configManager.getMaterialSlots();
        int perPage = materialSlots.size();
        if (perPage <= 0) {
            perPage = 45; // 安全兜底
        }

        int totalPages = Math.max(1, (int) Math.ceil(orderedKeys.size() / (double) perPage));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int start = (currentPage - 1) * perPage;
        int end = Math.min(orderedKeys.size(), start + perPage);

        int size = configManager.getGuiSize();
        String title = configManager.getGuiTitle().replace("{page}", String.valueOf(currentPage));
        Inventory inv = Bukkit.createInventory(new LootHolder(player.getUniqueId(), currentPage), size,
                MessageUtils.colorize(title));

        // 1. 放置翻页按钮
        int prevSlot = configManager.getPreviousPageSlot();
        int nextSlot = configManager.getNextPageSlot();
        inv.setItem(prevSlot, navItem(Material.ARROW, "上一页", currentPage > 1 ? currentPage - 1 : currentPage));
        inv.setItem(nextSlot, navItem(Material.ARROW, "下一页", currentPage < totalPages ? currentPage + 1 : currentPage));

        // 2. 物品栏位本身只是“功能槽位”，不需要预放物品，这里无需写入

        // 3. 放置装饰栏位（不会覆盖翻页和后续物品）
        applyDecorationSlots(inv);

        // 4. 按配置文件的物品列表，将物品按顺序放入物品栏位（不覆盖已有物品）
        int index = 0;
        for (int i = start; i < end && index < materialSlots.size(); i++) {
            String key = orderedKeys.get(i);
            WhitelistEntry entry = service.getWhitelistEntry(key);
            if (entry == null) {
                continue;
            }
            int amount = data.getOrDefault(key, 0);
            ItemStack display = buildDisplayItem(entry, amount);

            // 找到下一个空的物品槽位（不覆盖装饰或翻页）
            while (index < materialSlots.size()) {
                int slot = materialSlots.get(index++);
                ItemStack existing = inv.getItem(slot);
                if (existing == null || existing.getType().isAir()) {
                    inv.setItem(slot, display);
                    break;
                }
            }
        }

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

    private void applyDecorationSlots(Inventory inv) {
        Map<Integer, top.arctain.snowTerritory.utils.GuiSlotUtils.SlotItem> decorations = configManager.getDecorationSlots();
        top.arctain.snowTerritory.utils.GuiSlotUtils.applySlotItems(inv, decorations, true);
    }

    private ItemStack buildDisplayItem(WhitelistEntry entry, int amount) {
        ItemStack base = buildRealItem(entry);
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            // 保持物品本身的显示名称，不覆盖
            // 如果物品没有显示名称，则使用entry.getDisplay()作为后备
            String displayName = meta.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = entry.getDisplay();
            }
            
            // 构建 lore：先添加自定义 lore（如果有），然后添加默认 lore
            List<String> lore = new ArrayList<>();
            
            // 1. 如果有自定义 lore，先添加到上方
            if (entry.getLore() != null && !entry.getLore().isEmpty()) {
                for (String line : entry.getLore()) {
                    String processed = line
                            .replace("{amount}", String.valueOf(amount))
                            .replace("{max}", String.valueOf(entry.getDefaultMax()));
                    lore.add(MessageUtils.colorize(processed));
                }
            }
            
            // 2. 然后添加默认 lore 模板（显示在自定义 lore 下方）
            for (String line : configManager.getDefaultItemLoreTemplate()) {
                String processed = line
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{max}", String.valueOf(entry.getDefaultMax()));
                lore.add(MessageUtils.colorize(processed));
            }
            
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

