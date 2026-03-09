package top.arctain.snowTerritory.stfish.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stfish.data.FishDefinition;
import top.arctain.snowTerritory.stfish.data.FishTier;
import top.arctain.snowTerritory.utils.ConfigUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 管理 stfish 模块配置。 */
public class StfishConfigManager {

    private final Main plugin;
    private final File baseDir;
    private FileConfiguration mainConfig;
    private FileConfiguration fishConfig;
    private Map<String, FileConfiguration> messagePacks = new HashMap<>();
    private Map<FishTier, List<FishDefinition>> fishByTier = new HashMap<>();

    public StfishConfigManager(Main plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "stfish");
    }

    public void loadAll() {
        ensureDefaults();
        loadMainConfig();
        loadFishConfig();
        loadMessages();
    }

    private void ensureDefaults() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 stfish 目录失败: " + baseDir.getAbsolutePath());
        }
        ConfigUtils.copyResourceIfMissing(plugin, "stfish/config.yml", new File(baseDir, "config.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "stfish/fish.yml", new File(baseDir, "fish.yml"));
        File msgDir = new File(baseDir, "messages");
        if (!msgDir.exists()) msgDir.mkdirs();
        ConfigUtils.copyResourceIfMissing(plugin, "stfish/messages/zh_CN.yml", new File(msgDir, "zh_CN.yml"));
    }

    private void loadMainConfig() {
        this.mainConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "config.yml"));
    }

    private void loadFishConfig() {
        this.fishConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "fish.yml"));
        fishByTier.clear();
        for (FishTier tier : FishTier.values()) {
            List<FishDefinition> list = loadFishForTier(tier.name().toLowerCase());
            fishByTier.put(tier, list);
        }
    }

    private List<FishDefinition> loadFishForTier(String tierKey) {
        List<Map<?, ?>> raw = fishConfig.getMapList(tierKey);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<FishDefinition> result = new ArrayList<>();
        for (Map<?, ?> map : raw) {
            String id = (String) map.get("id");
            String name = (String) map.get("name");
            String desc = (String) map.get("description");
            Object lenMin = map.get("length-min");
            Object lenMax = map.get("length-max");
            String matStr = (String) map.get("material");
            if (id == null || name == null || matStr == null) continue;
            double lMin = toDouble(lenMin, 0.5);
            double lMax = toDouble(lenMax, 1.0);
            if (lMin > lMax) lMax = lMin;
            Material mat = Material.matchMaterial(matStr);
            if (mat == null || !mat.isItem()) mat = Material.COD;
            result.add(new FishDefinition(id, name, desc != null ? desc : "", lMin, lMax, mat));
        }
        return result;
    }

    private double toDouble(Object o, double def) {
        if (o == null) return def;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void loadMessages() {
        File msgDir = new File(baseDir, "messages");
        this.messagePacks = new HashMap<>();
        if (msgDir.exists()) {
            File[] files = msgDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String key = file.getName().replace(".yml", "");
                    messagePacks.put(key, YamlConfiguration.loadConfiguration(file));
                }
            }
        }
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public Map<FishTier, List<FishDefinition>> getFishByTier() {
        return Collections.unmodifiableMap(fishByTier);
    }

    public FishDefinition getFishById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (List<FishDefinition> list : fishByTier.values()) {
            for (FishDefinition def : list) {
                if (id.equalsIgnoreCase(def.id())) return def;
            }
        }
        return null;
    }

    public List<FishDefinition> getAllFishOrdered() {
        List<FishDefinition> result = new ArrayList<>();
        for (FishTier tier : FishTier.values()) {
            result.addAll(fishByTier.getOrDefault(tier, Collections.emptyList()));
        }
        return result;
    }

    public double getSummonCost() {
        ConfigurationSection section = mainConfig.getConfigurationSection("weather");
        return section != null ? section.getDouble("summon-cost", 1000) : 1000;
    }

    public double getStormChance() {
        ConfigurationSection section = mainConfig.getConfigurationSection("weather");
        return section != null ? section.getDouble("storm-chance", 0.1) : 0.1;
    }

    public Map<String, Integer> getQualityWeights(String weatherKey) {
        ConfigurationSection section = mainConfig.getConfigurationSection("quality-weights." + weatherKey);
        if (section == null) return defaultWeights(weatherKey);
        Map<String, Integer> map = new HashMap<>();
        for (String k : section.getKeys(false)) {
            map.put(k, section.getInt(k, 1));
        }
        return map;
    }

    private Map<String, Integer> defaultWeights(String weatherKey) {
        return switch (weatherKey) {
            case "storm" -> Map.of("common", 20, "rare", 25, "epic", 25, "legendary", 15, "storm", 10, "world", 5);
            case "rain" -> Map.of("common", 35, "rare", 30, "epic", 18, "legendary", 10, "storm", 5, "world", 2);
            default -> Map.of("common", 50, "rare", 30, "epic", 12, "legendary", 5, "storm", 2, "world", 1);
        };
    }

    public long getTitleFadeIn() {
        ConfigurationSection section = mainConfig.getConfigurationSection("title");
        return section != null ? section.getLong("fade-in", 300) : 300;
    }

    public long getTitleStay() {
        ConfigurationSection section = mainConfig.getConfigurationSection("title");
        return section != null ? section.getLong("stay", 1500) : 1500;
    }

    public long getTitleFadeOut() {
        ConfigurationSection section = mainConfig.getConfigurationSection("title");
        return section != null ? section.getLong("fade-out", 500) : 500;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public Map<String, String> getMessagesForMerge() {
        FileConfiguration pack = messagePacks.get("zh_CN");
        if (pack == null && !messagePacks.isEmpty()) {
            pack = messagePacks.values().iterator().next();
        }
        if (pack == null) return Map.of();
        return ConfigUtils.loadMessagesRecursive("messages.stfish", pack);
    }
}
