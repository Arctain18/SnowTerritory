package top.arctain.snowTerritory.enderstorage.service;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.enderstorage.config.EnderStorageConfigManager;
import top.arctain.snowTerritory.enderstorage.config.ProgressionResolver;
import top.arctain.snowTerritory.enderstorage.config.WhitelistEntry;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.Utils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LootStorageServiceImpl implements LootStorageService {

    private final Main plugin;
    private final EnderStorageConfigManager configManager;
    private final Map<UUID, Map<String, Integer>> cache = new ConcurrentHashMap<>();
    private LootStorageDao dao;
    private ProgressionResolver resolver;
    private Map<String, WhitelistEntry> whitelist = new HashMap<>();

    public LootStorageServiceImpl(Main plugin, EnderStorageConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void initialize() {
        FileConfiguration cfg = configManager.getMainConfig();
        String dbType = cfg.getString("database.type", "sqlite").toLowerCase();
        if ("sqlite".equals(dbType)) {
            File db = new File(configManager.getBaseDir(), cfg.getString("database.file", "ender-storage.db"));
            this.dao = new SqliteLootStorageDao(plugin, db);
        } else {
            MessageUtils.logWarning("暂未实现的数据库类型: " + dbType + "，回退至 SQLite");
            File db = new File(configManager.getBaseDir(), "ender-storage.db");
            this.dao = new SqliteLootStorageDao(plugin, db);
        }
        dao.init();
        this.resolver = new ProgressionResolver(configManager.getProgressionConfig());
        this.whitelist = loadWhitelist();
    }

    private Map<String, WhitelistEntry> loadWhitelist() {
        Map<String, WhitelistEntry> result = new HashMap<>();
        FileConfiguration guiCfg = configManager.getGuiConfig();
        if (guiCfg == null) {
            return result;
        }

        // 从 gui.yml 的 gui.materials 节点构建白名单：
        // gui:
        //   materials:
        //     TYPE:
        //       Name: maxStack
        org.bukkit.configuration.ConfigurationSection root = guiCfg.getConfigurationSection("gui.materials");
        if (root == null) {
            return result;
        }

        for (String mmoType : root.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection typeSection = root.getConfigurationSection(mmoType);
            if (typeSection == null) continue;

            for (String mmoItemId : typeSection.getKeys(false)) {
                // 生成key为"mmoType:mmoItemId"格式
                String key = mmoType + ":" + mmoItemId;
                
                // 检查配置是简单格式（数字）还是对象格式
                Object value = typeSection.get(mmoItemId);
                int max;
                java.util.List<String> lore = null;
                String taskCategory = "任务材料";
                String taskLevel = "-";
                String taskLocation = "-";
                
                if (value instanceof org.bukkit.configuration.ConfigurationSection) {
                    // 对象格式: "itemName: {max: 256, lore: [...]}"
                    org.bukkit.configuration.ConfigurationSection itemSection = typeSection.getConfigurationSection(mmoItemId);
                    if (itemSection != null) {
                        max = itemSection.getInt("max", 256);
                        if (itemSection.contains("lore")) {
                            lore = itemSection.getStringList("lore");
                        }
                        taskCategory = itemSection.getString("task-category", "任务材料");
                        taskLevel = itemSection.getString("task-level", "-");
                        taskLocation = itemSection.getString("task-location", "-");
                    } else {
                        max = 256;
                    }
                } else {
                    // 简单格式: "itemName: 256" (支持数字或字符串数字)
                    max = typeSection.getInt(mmoItemId, 256);
                }
                
                // display 使用 mmoItemId，material 设为 null（MMOItems 物品不需要 material）
                result.put(key, new WhitelistEntry(key, mmoItemId, mmoType, mmoItemId, null, max, lore,
                        taskCategory, taskLevel, taskLocation));
            }
        }
        return result;
    }

    @Override
    public void shutdown() {
        cache.forEach(dao::save);
        if (dao != null) {
            dao.close();
        }
        cache.clear();
    }

    @Override
    public void reload() {
        cache.clear();
        initialize();
    }

    @Override
    public int getAmount(UUID playerId, String itemKey) {
        return getPlayerData(playerId).getOrDefault(itemKey, 0);
    }

    @Override
    public boolean consume(UUID playerId, String itemKey, int amount) {
        Map<String, Integer> data = getPlayerData(playerId);
        int current = data.getOrDefault(itemKey, 0);
        if (current < amount) {
            return false;
        }
        data.put(itemKey, current - amount);
        dao.save(playerId, data);
        return true;
    }

    @Override
    public int add(UUID playerId, String itemKey, int amount, int perItemMax, int slotLimit) {
        if (amount <= 0) {
            return 0;
        }
        Map<String, Integer> data = getPlayerData(playerId);
        if (!data.containsKey(itemKey) && slotLimit > 0 && data.size() >= slotLimit) {
            MessageUtils.logWarning("玩家 " + playerId + " 超出仓库槽位限制，物品未存入。");
            return 0;
        }
        int current = data.getOrDefault(itemKey, 0);
        int canStore = Math.max(0, perItemMax - current);
        int stored = Math.min(amount, canStore);
        if (stored <= 0) {
            return 0;
        }
        int newValue = current + stored;
        data.put(itemKey, newValue);
        if (slotLimit > 0 && data.size() > slotLimit) {
            MessageUtils.logWarning("玩家 " + playerId + " 超出仓库槽位限制，部分物品未存入。");
        }
        dao.save(playerId, data);
        return stored;
    }

    @Override
    public Map<String, Integer> getAll(UUID playerId) {
        return Collections.unmodifiableMap(getPlayerData(playerId));
    }

    @Override
    public String matchItemKey(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        boolean isMmo = Utils.isMMOItem(stack);
        if (isMmo) {
            try {
                net.Indyuce.mmoitems.api.Type type = net.Indyuce.mmoitems.MMOItems.getType(stack);
                String id = net.Indyuce.mmoitems.MMOItems.getID(stack);
                if (type != null && id != null) {
                    // 生成key为"mmoType:mmoItemId"格式
                    String key = type.getId() + ":" + id;
                    // 检查白名单中是否存在该key
                    if (whitelist.containsKey(key)) {
                        return key;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        // 如果不是MMOItems物品，检查是否有匹配的Material（向后兼容）
        for (WhitelistEntry entry : whitelist.values()) {
            if (entry.getMaterial() != null && entry.getMaterial() == stack.getType()) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public WhitelistEntry getWhitelistEntry(String key) {
        return whitelist.get(key);
    }

    @Override
    public java.util.Collection<WhitelistEntry> getWhitelistEntries() {
        return whitelist.values();
    }

    @Override
    public int resolveSlots(Player player) {
        int base = resolver.resolveSlots(player);
        var vip = plugin.getStvipService();
        return base + (vip != null ? vip.getLootExtraSlots(player) : 0);
    }

    @Override
    public int resolvePerItemMax(Player player, String itemKey) {
        int configured = Optional.ofNullable(whitelist.get(itemKey))
                .map(WhitelistEntry::getDefaultMax)
                .orElse(256);
        int base = Math.min(configured, resolver.resolvePerItemMax(player));
        var vip = plugin.getStvipService();
        return base + (vip != null ? vip.getLootExtraPerItemMax(player) : 0);
    }

    private Map<String, Integer> getPlayerData(UUID playerId) {
        return cache.computeIfAbsent(playerId, dao::loadAll);
    }
}

