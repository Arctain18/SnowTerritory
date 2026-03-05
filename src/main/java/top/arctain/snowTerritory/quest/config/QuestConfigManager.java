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

/**
 * 管理 quest 配置的加载与默认文件生成。
 * 所有配置放在 plugins/SnowTerritory/quest/ 下。
 */
public class QuestConfigManager {

    private final File baseDir;
    private FileConfiguration mainConfig;
    private Map<String, FileConfiguration> messagePacks = new HashMap<>();
    private FileConfiguration rewardsDefault;
    private FileConfiguration rewardsLevel;
    private FileConfiguration bonusTimeBonus;
    private FileConfiguration materialsWhitelist;
    private FileConfiguration bountyConfig;
    private FileConfiguration tasksMaterial;
    private FileConfiguration tasksKill;

    public QuestConfigManager(Main plugin) {
        this.baseDir = new File(plugin.getDataFolder(), "quest");
    }

    public void loadAll() {
        ensureDefaults();
        loadMainConfig();
        loadMessages();
        loadRewards();
        loadBonus();
        loadMaterials();
        loadBounty();
        loadTasks();
    }

    private void ensureDefaults() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 quest 目录失败: " + baseDir.getAbsolutePath());
        }
        ConfigUtils.copyIfMissing(new File(baseDir, "config.yml"), DefaultFiles.DEFAULT_CONFIG);
        ConfigUtils.copyIfMissing(new File(baseDir, "messages/zh_CN.yml"), DefaultFiles.DEFAULT_MESSAGES_ZH);
        ConfigUtils.copyIfMissing(new File(baseDir, "rewards/default.yml"), DefaultFiles.DEFAULT_REWARDS_DEFAULT);
        ConfigUtils.copyIfMissing(new File(baseDir, "rewards/level.yml"), DefaultFiles.DEFAULT_REWARDS_LEVEL);
        ConfigUtils.copyIfMissing(new File(baseDir, "bonus/time-bonus.yml"), DefaultFiles.DEFAULT_BONUS_TIME_BONUS);
        ConfigUtils.copyIfMissing(new File(baseDir, "materials/whitelist.yml"), DefaultFiles.DEFAULT_MATERIALS_WHITELIST);
        ConfigUtils.copyIfMissing(new File(baseDir, "bounty/config.yml"), DefaultFiles.DEFAULT_BOUNTY_CONFIG);
        ConfigUtils.copyIfMissing(new File(baseDir, "tasks/material.yml"), DefaultFiles.DEFAULT_TASKS_MATERIAL);
        ConfigUtils.copyIfMissing(new File(baseDir, "tasks/kill.yml"), DefaultFiles.DEFAULT_TASKS_KILL);
    }

    private void loadMainConfig() {
        this.mainConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "config.yml"));
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

    private void loadBounty() {
        this.bountyConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "bounty/config.yml"));
    }

    private void loadTasks() {
        this.tasksMaterial = YamlConfiguration.loadConfiguration(new File(baseDir, "tasks/material.yml"));
        this.tasksKill = YamlConfiguration.loadConfiguration(new File(baseDir, "tasks/kill.yml"));
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
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

    public FileConfiguration getBonusTimeBonus() {
        return bonusTimeBonus;
    }

    public FileConfiguration getMaterialsWhitelist() {
        return materialsWhitelist;
    }

    public FileConfiguration getBountyConfig() {
        return bountyConfig;
    }

    public FileConfiguration getTasksMaterial() {
        return tasksMaterial;
    }

    public FileConfiguration getTasksKill() {
        return tasksKill;
    }

    public File getBaseDir() {
        return baseDir;
    }
}

