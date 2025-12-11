package top.arctain.snowTerritory.enderstorage.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理 ender-storage 配置的加载与默认文件生成。
 * 所有配置放在 plugins/SnowTerritory/ender-storage/ 下。
 */
public class EnderStorageConfigManager {

    private final Main plugin;
    private final File baseDir;
    private FileConfiguration mainConfig;
    private FileConfiguration whitelistConfig;
    private Map<String, FileConfiguration> messagePacks = new HashMap<>();
    private ProgressionConfig progressionConfig;

    public EnderStorageConfigManager(Main plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "ender-storage");
    }

    public void loadAll() {
        ensureDefaults();
        loadMainConfig();
        loadWhitelist();
        loadMessages();
        loadProgression();
    }

    private void ensureDefaults() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 ender-storage 目录失败: " + baseDir.getAbsolutePath());
        }
        copyIfMissing(new File(baseDir, "config.yml"), DefaultFiles.DEFAULT_CONFIG);
        copyIfMissing(new File(baseDir, "loot/whitelist.yml"), DefaultFiles.DEFAULT_WHITELIST);
        copyIfMissing(new File(baseDir, "progression/size.yml"), DefaultFiles.DEFAULT_SIZE);
        copyIfMissing(new File(baseDir, "progression/stack.yml"), DefaultFiles.DEFAULT_STACK);
        copyIfMissing(new File(baseDir, "messages/zh_CN.yml"), DefaultFiles.DEFAULT_MESSAGES_ZH);
    }

    private void copyIfMissing(File target, String content) {
        try {
            if (!target.exists()) {
                if (target.getParentFile() != null) {
                    target.getParentFile().mkdirs();
                }
                Files.writeString(target.toPath(), content);
            }
        } catch (IOException e) {
            MessageUtils.logError("写入默认配置失败: " + target.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    private void loadMainConfig() {
        this.mainConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "config.yml"));
    }

    private void loadWhitelist() {
        this.whitelistConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "loot/whitelist.yml"));
    }

    private void loadMessages() {
        File msgDir = new File(baseDir, "messages");
        this.messagePacks = new HashMap<>();
        File[] files = msgDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String key = file.getName().replace(".yml", "");
                messagePacks.put(key, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    private void loadProgression() {
        this.progressionConfig = new ProgressionConfig(
                YamlConfiguration.loadConfiguration(new File(baseDir, "progression/size.yml")),
                YamlConfiguration.loadConfiguration(new File(baseDir, "progression/stack.yml"))
        );
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public FileConfiguration getWhitelistConfig() {
        return whitelistConfig;
    }

    public Map<String, FileConfiguration> getMessagePacks() {
        return Collections.unmodifiableMap(messagePacks);
    }

    public ProgressionConfig getProgressionConfig() {
        return progressionConfig;
    }

    public File getBaseDir() {
        return baseDir;
    }
}

