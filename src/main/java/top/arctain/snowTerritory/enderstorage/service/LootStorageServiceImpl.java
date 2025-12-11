package top.arctain.snowTerritory.enderstorage.service;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.enderstorage.config.EnderStorageConfigManager;
import top.arctain.snowTerritory.enderstorage.config.ProgressionResolver;
import top.arctain.snowTerritory.enderstorage.config.WhitelistEntry;
import top.arctain.snowTerritory.utils.MessageUtils;

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
        for (String key : cfg.getKeys(false)) {
            String base = key + ".";
            String display = cfg.getString(base + "display", key);
            String mmoId = cfg.getString(base + "id", "");
            String type = cfg.getString(base + "type", "STONE");
            int max = cfg.getInt(base + "default_max", 256);
            try {
                result.put(key, new WhitelistEntry(key, display, mmoId, org.bukkit.Material.valueOf(type), max));
            } catch (IllegalArgumentException ex) {
                MessageUtils.logWarning("白名单物品类型无效: " + type + " @ " + key);
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

