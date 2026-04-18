package top.arctain.snowTerritory.quest.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.ConfigUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理 quest 配置的加载与默认文件生成。
 * 所有配置放在 plugins/SnowTerritory/quest/ 下。
 */
public class QuestConfigManager {

    private final Main plugin;
    private final File baseDir;
    private FileConfiguration mainConfig;
    private Map<String, FileConfiguration> messagePacks = new HashMap<>();
    private FileConfiguration rewardsDefault;
    private FileConfiguration rewardsLevel;
    private FileConfiguration bonusTimeBonus;
    private FileConfiguration materialsWhitelist;
    private FileConfiguration cropsWhitelist;
    private FileConfiguration bountyConfig;
    private FileConfiguration tasksMaterial;
    private FileConfiguration tasksCollect;
    private FileConfiguration tasksKill;
    private QuestListProgressConfig listProgressConfig = QuestListProgressConfig.defaults();

    public QuestConfigManager(Main plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "quest");
    }

    public void loadAll() {
        ensureDefaults();
        loadMainConfig();
        loadMessages();
        loadRewards();
        loadBonus();
        loadMaterials();
        loadCrops();
        loadBounty();
        loadTasks();
    }

    private void ensureDefaults() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 quest 目录失败: " + baseDir.getAbsolutePath());
        }
        ConfigUtils.copyResourceIfMissing(plugin, "quest/config.yml", new File(baseDir, "config.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/messages/zh_CN.yml", new File(baseDir, "messages/zh_CN.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/rewards/default.yml", new File(baseDir, "rewards/default.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/rewards/level.yml", new File(baseDir, "rewards/level.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/bonus/time-bonus.yml", new File(baseDir, "bonus/time-bonus.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/materials/whitelist.yml", new File(baseDir, "materials/whitelist.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/crops/whitelist.yml", new File(baseDir, "crops/whitelist.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/bounty/config.yml", new File(baseDir, "bounty/config.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/tasks/material.yml", new File(baseDir, "tasks/material.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/tasks/collect.yml", new File(baseDir, "tasks/collect.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "quest/tasks/kill.yml", new File(baseDir, "tasks/kill.yml"));
    }

    private void loadMainConfig() {
        this.mainConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "config.yml"));
        this.listProgressConfig = QuestListProgressConfig.fromMainConfig(this.mainConfig);
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

    private void loadRewards() {
        this.rewardsDefault = YamlConfiguration.loadConfiguration(new File(baseDir, "rewards/default.yml"));
        this.rewardsLevel = YamlConfiguration.loadConfiguration(new File(baseDir, "rewards/level.yml"));
    }

    private void loadBonus() {
        this.bonusTimeBonus = YamlConfiguration.loadConfiguration(new File(baseDir, "bonus/time-bonus.yml"));
    }

    private void loadMaterials() {
        this.materialsWhitelist = YamlConfiguration.loadConfiguration(new File(baseDir, "materials/whitelist.yml"));
    }

    private void loadCrops() {
        this.cropsWhitelist = YamlConfiguration.loadConfiguration(new File(baseDir, "crops/whitelist.yml"));
    }

    private void loadBounty() {
        this.bountyConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "bounty/config.yml"));
    }

    private void loadTasks() {
        this.tasksMaterial = YamlConfiguration.loadConfiguration(new File(baseDir, "tasks/material.yml"));
        this.tasksCollect = YamlConfiguration.loadConfiguration(new File(baseDir, "tasks/collect.yml"));
        this.tasksKill = YamlConfiguration.loadConfiguration(new File(baseDir, "tasks/kill.yml"));
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public QuestListProgressConfig getListProgressConfig() {
        return listProgressConfig;
    }

    public Map<String, FileConfiguration> getMessagePacks() {
        return Collections.unmodifiableMap(messagePacks);
    }

    public FileConfiguration getRewardsDefault() {
        return rewardsDefault;
    }

    public FileConfiguration getRewardsLevel() {
        return rewardsLevel;
    }

    /** 有效材料任务等级集合，来自 rewards/level.yml 的 level 键 */
    public Set<Integer> getValidMaterialLevels() {
        if (rewardsLevel == null) return Set.of(1);
        var section = rewardsLevel.getConfigurationSection("level");
        if (section == null) return Set.of(1);
        return section.getKeys(false).stream()
                .map(k -> {
                    try {
                        return Integer.parseInt(k);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(i -> i != null)
                .collect(Collectors.toSet());
    }

    public FileConfiguration getBonusTimeBonus() {
        return bonusTimeBonus;
    }

    public FileConfiguration getMaterialsWhitelist() {
        return materialsWhitelist;
    }

    public FileConfiguration getCropsWhitelist() {
        return cropsWhitelist;
    }

    public FileConfiguration getBountyConfig() {
        return bountyConfig;
    }

    public FileConfiguration getTasksMaterial() {
        return tasksMaterial;
    }

    public FileConfiguration getTasksCollect() {
        return tasksCollect;
    }

    public FileConfiguration getTasksKill() {
        return tasksKill;
    }

    public File getBaseDir() {
        return baseDir;
    }

    /** 获取任务模块消息用于合并到主配置（供 MessageUtils 使用） */
    public Map<String, String> getMessagesForMerge() {
        FileConfiguration pack = messagePacks.get("zh_CN");
        if (pack == null && !messagePacks.isEmpty()) {
            pack = messagePacks.values().iterator().next();
        }
        if (pack == null) return Map.of();
        return ConfigUtils.loadMessagesRecursive("messages.quest", pack);
    }
}

