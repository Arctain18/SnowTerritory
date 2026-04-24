package top.arctain.snowTerritory.life.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.ConfigUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.util.Map;

public class LifeConfigManager {

    private final Main plugin;
    private final File baseDir;
    private FileConfiguration config;

    public LifeConfigManager(Main plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "life");
    }

    public void loadAll() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 life 目录失败: " + baseDir.getAbsolutePath());
        }
        ConfigUtils.copyResourceIfMissing(plugin, "life/config.yml", new File(baseDir, "config.yml"));
        ConfigUtils.copyResourceIfMissing(plugin, "life/messages/zh_CN.yml", new File(baseDir, "messages/zh_CN.yml"));
        this.config = YamlConfiguration.loadConfiguration(new File(baseDir, "config.yml"));
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public Map<String, String> getMessagesForMerge() {
        File messagePack = new File(baseDir, "messages/zh_CN.yml");
        if (!messagePack.exists()) {
            return Map.of();
        }
        return ConfigUtils.loadMessagesRecursive("messages.life", YamlConfiguration.loadConfiguration(messagePack));
    }
}
