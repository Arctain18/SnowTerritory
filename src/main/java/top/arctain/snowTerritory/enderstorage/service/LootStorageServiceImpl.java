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
        FileConfiguration cfg = configManager.getWhitelistConfig();
        // 第一级key是mmoType
        for (String mmoType : cfg.getKeys(false)) {
            if (!cfg.isConfigurationSection(mmoType)) {
                continue;
            }
            org.bukkit.configuration.ConfigurationSection typeSection = cfg.getConfigurationSection(mmoType);
            if (typeSection == null) {
                continue;
            }
            // 第二级key是mmoItemId
            for (String mmoItemId : typeSection.getKeys(false)) {
                if (!typeSection.isConfigurationSection(mmoItemId)) {
                    continue;
                }
                org.bukkit.configuration.ConfigurationSection itemSection = typeSection.getConfigurationSection(mmoItemId);
                if (itemSection == null) {
                    continue;
                }
                // 生成key为"mmoType:mmoItemId"格式
                String key = mmoType + ":" + mmoItemId;
                int max = itemSection.getInt("default_max", 256);
                // 不需要type和display，保持和对应物品相同
                // display使用mmoItemId，material设为null（因为MMOItems物品不需要material）
                result.put(key, new WhitelistEntry(key, mmoItemId, mmoType, mmoItemId, null, max));
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
    public void add(UUID playerId, String itemKey, int amount, int perItemMax, int slotLimit) {
        Map<String, Integer> data = getPlayerData(playerId);
        int current = data.getOrDefault(itemKey, 0);
        int newValue = Math.min(current + amount, perItemMax);
        data.put(itemKey, newValue);
        if (data.size() > slotLimit) {
            MessageUtils.logWarning("玩家 " + playerId + " 超出仓库槽位限制，部分物品未存入。");
        }
        dao.save(playerId, data);
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
        return resolver.resolveSlots(player);
    }

    @Override
    public int resolvePerItemMax(Player player, String itemKey) {
        int configured = Optional.ofNullable(whitelist.get(itemKey))
                .map(WhitelistEntry::getDefaultMax)
                .orElse(256);
        return Math.max(configured, resolver.resolvePerItemMax(player));
    }

    private Map<String, Integer> getPlayerData(UUID playerId) {
        return cache.computeIfAbsent(playerId, dao::loadAll);
    }
}

