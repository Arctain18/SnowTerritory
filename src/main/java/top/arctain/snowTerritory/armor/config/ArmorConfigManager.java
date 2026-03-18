package top.arctain.snowTerritory.armor.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.armor.data.ArmorQuality;
import top.arctain.snowTerritory.armor.data.ArmorSetDefinition;
import top.arctain.snowTerritory.armor.data.ArmorSlot;
import top.arctain.snowTerritory.armor.data.ArmorStatRange;
import top.arctain.snowTerritory.utils.ConfigUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ArmorConfigManager {

    private final Main plugin;
    private final File baseDir;
    private FileConfiguration mainConfig;
    private FileConfiguration setsConfig;
    private Map<String, FileConfiguration> messagePacks = new HashMap<>();

    private String mmoitemsType;
    private Map<String, ArmorSlot> slots = new HashMap<>();
    private Map<String, ArmorQuality> qualities = new HashMap<>();
    private boolean normalEnabled;
    private double sigmaFactor;
    private double globalMinMultiplier;
    private double globalMaxMultiplier;
    private Map<String, String> statMapping = new HashMap<>();
    private Map<String, ArmorSetDefinition> sets = new HashMap<>();

    public ArmorConfigManager(Main plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "armor");
    }

    public void loadAll() {
        ensureDefaults();
        loadMainConfig();
        loadSetsConfig();
        loadMessages();
        MessageUtils.logSuccess("Armor 配置已加载，配置目录: plugins/SnowTerritory/armor/");
    }

    private void ensureDefaults() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 armor 目录失败: " + baseDir.getAbsolutePath());
        }
        ConfigUtils.copyResourceIfMissing(plugin, "armor/config.yml", new File(baseDir, "config.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "armor/sets.yml", new File(baseDir, "sets.yml"));
        File msgDir = new File(baseDir, "messages");
        if (!msgDir.exists() && !msgDir.mkdirs()) {
            MessageUtils.logWarning("创建 armor/messages 目录失败: " + msgDir.getAbsolutePath());
        }
        ConfigUtils.copyResourceIfMissing(plugin, "armor/messages/zh_CN.yml", new File(msgDir, "zh_CN.yml"));
    }

    private void loadMainConfig() {
        this.mainConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "config.yml"));
        ConfigurationSection armor = mainConfig.getConfigurationSection("armor");
        if (armor == null) {
            MessageUtils.logWarning("armor/config.yml 中缺少 armor 节点");
            return;
        }
        this.mmoitemsType = armor.getString("mmoitems-type", "ARMOR");

        slots.clear();
        ConfigurationSection slotSection = armor.getConfigurationSection("slots");
        if (slotSection != null) {
            for (String key : slotSection.getKeys(false)) {
                ConfigurationSection s = slotSection.getConfigurationSection(key);
                if (s == null) continue;
                String slotKey = s.getString("slot-key", key.toUpperCase());
                String templateId = s.getString("template-id");
                slots.put(key, new ArmorSlot(key, slotKey, templateId));
            }
        }

        qualities.clear();
        ConfigurationSection qSection = armor.getConfigurationSection("qualities");
        if (qSection != null) {
            for (String id : qSection.getKeys(false)) {
                ConfigurationSection qs = qSection.getConfigurationSection(id);
                if (qs == null) continue;
                String displayName = qs.getString("display-name", id);
                String suffix = qs.getString("suffix", "");
                int weight = qs.getInt("weight", 1);
                double multiplier = qs.getDouble("value-multiplier", 1.0);
                qualities.put(id, new ArmorQuality(id, displayName, suffix, weight, multiplier));
            }
        }

        ConfigurationSection random = armor.getConfigurationSection("random");
        this.normalEnabled = random != null && random.getBoolean("normal-enabled", true);
        this.sigmaFactor = random != null ? random.getDouble("sigma-factor", 0.1) : 0.1;
        this.globalMinMultiplier = random != null ? random.getDouble("min-multiplier", 0.8) : 0.8;
        this.globalMaxMultiplier = random != null ? random.getDouble("max-multiplier", 1.2) : 1.2;

        statMapping.clear();
        ConfigurationSection mapping = armor.getConfigurationSection("stat-mapping");
        if (mapping != null) {
            for (String statKey : mapping.getKeys(false)) {
                String mmoStat = mapping.getString(statKey);
                if (mmoStat != null) {
                    statMapping.put(statKey, mmoStat);
                }
            }
        }
    }

    private void loadSetsConfig() {
        this.setsConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "sets.yml"));
        sets.clear();
        ConfigurationSection setsRoot = setsConfig.getConfigurationSection("sets");
        if (setsRoot == null) {
            return;
        }
        for (String id : setsRoot.getKeys(false)) {
            ConfigurationSection section = setsRoot.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String displayName = section.getString("display-name", id);

            Map<String, Double> baseStats = new HashMap<>();
            ConfigurationSection baseStatsSec = section.getConfigurationSection("base-stats");
            if (baseStatsSec != null) {
                for (String statKey : baseStatsSec.getKeys(false)) {
                    baseStats.put(statKey, baseStatsSec.getDouble(statKey, 0.0));
                }
            }

            Map<String, Double> slotRatios = new HashMap<>();
            ConfigurationSection slotRatiosSec = section.getConfigurationSection("slot-ratios");
            if (slotRatiosSec != null) {
                for (String slotId : slotRatiosSec.getKeys(false)) {
                    slotRatios.put(slotId, slotRatiosSec.getDouble(slotId, 0.0));
                }
            }

            Map<String, ArmorStatRange> statRanges = new HashMap<>();
            ConfigurationSection statRangesSec = section.getConfigurationSection("stat-ranges");
            if (statRangesSec != null) {
                for (String statKey : statRangesSec.getKeys(false)) {
                    ConfigurationSection rs = statRangesSec.getConfigurationSection(statKey);
                    if (rs == null) {
                        continue;
                    }
                    double min = rs.getDouble("min-multiplier", globalMinMultiplier);
                    double max = rs.getDouble("max-multiplier", globalMaxMultiplier);
                    statRanges.put(statKey, new ArmorStatRange(min, max));
                }
            }

            ArmorSetDefinition def = new ArmorSetDefinition(id, displayName, baseStats, slotRatios, statRanges);
            sets.put(id.toLowerCase(), def);
        }
    }

    private void loadMessages() {
        File msgDir = new File(baseDir, "messages");
        this.messagePacks = new HashMap<>();
        if (!msgDir.exists()) {
            return;
        }
        File[] files = msgDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String key = file.getName().replace(".yml", "");
            messagePacks.put(key, YamlConfiguration.loadConfiguration(file));
        }
    }

    public Map<String, String> getMessagesForMerge() {
        FileConfiguration pack = messagePacks.get("zh_CN");
        if (pack == null && !messagePacks.isEmpty()) {
            pack = messagePacks.values().iterator().next();
        }
        if (pack == null) {
            return Map.of();
        }
        return ConfigUtils.loadMessagesRecursive("messages.armor", pack);
    }

    public String getMmoitemsType() {
        return mmoitemsType;
    }

    public Map<String, ArmorSlot> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public Map<String, ArmorQuality> getQualities() {
        return Collections.unmodifiableMap(qualities);
    }

    public boolean isNormalEnabled() {
        return normalEnabled;
    }

    public double getSigmaFactor() {
        return sigmaFactor;
    }

    public double getGlobalMinMultiplier() {
        return globalMinMultiplier;
    }

    public double getGlobalMaxMultiplier() {
        return globalMaxMultiplier;
    }

    public Map<String, String> getStatMapping() {
        return Collections.unmodifiableMap(statMapping);
    }

    public ArmorSetDefinition getSet(String id) {
        if (id == null) {
            return null;
        }
        return sets.get(id.toLowerCase());
    }

    public Map<String, ArmorSetDefinition> getAllSets() {
        return Collections.unmodifiableMap(sets);
    }

    public File getBaseDir() {
        return baseDir;
    }
}

