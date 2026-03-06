package top.arctain.snowTerritory.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** 配置工具：默认文件从资源复制、消息递归加载。资源路径相对于 default-configs/ */
public final class ConfigUtils {

    private static final String DEFAULT_CONFIGS_PREFIX = "default-configs/";

    private ConfigUtils() {
    }

    /** 从 JAR 资源复制默认配置到目标文件，若目标已存在则跳过。resourcePath 相对于 default-configs/，如 reinforce/config.yml */
    public static void copyResourceIfMissing(JavaPlugin plugin, String resourcePath, File target) {
        if (target.exists()) return;
        String fullPath = DEFAULT_CONFIGS_PREFIX + resourcePath;
        try (InputStream in = plugin.getResource(fullPath)) {
            if (in == null) {
                MessageUtils.logError("默认配置资源不存在: " + fullPath);
                return;
            }
            if (target.getParentFile() != null) {
                target.getParentFile().mkdirs();
            }
            Files.copy(in, target.toPath());
        } catch (IOException e) {
            MessageUtils.logError("复制默认配置失败: " + target.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    public static Map<String, String> loadMessagesRecursive(String path, ConfigurationSection section) {
        Map<String, String> result = new HashMap<>();
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            String fullPath = path + "." + key;
            if (section.isConfigurationSection(key)) {
                result.putAll(loadMessagesRecursive(fullPath, section.getConfigurationSection(key)));
            } else {
                result.put(fullPath, section.getString(key));
            }
        }
        return result;
    }

    public static FileConfiguration loadConfig(File file) {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                MessageUtils.logError("创建配置文件失败: " + file.getAbsolutePath() + " - " + e.getMessage());
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public static void saveConfig(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            MessageUtils.logError("保存配置失败: " + file.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    /** 删除目录下所有配置文件，保留 .db 数据库文件。返回删除的文件数。 */
    public static int deleteConfigFilesExcludingDatabase(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return 0;
        int count = 0;
        File[] children = dir.listFiles();
        if (children == null) return 0;
        for (File f : children) {
            if (f.isDirectory()) {
                count += deleteConfigFilesExcludingDatabase(f);
                if (f.list() == null || f.list().length == 0) {
                    f.delete();
                }
            } else if (!f.getName().toLowerCase().endsWith(".db")) {
                if (f.delete()) count++;
            }
        }
        return count;
    }
}
