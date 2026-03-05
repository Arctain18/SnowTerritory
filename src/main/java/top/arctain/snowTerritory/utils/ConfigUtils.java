package top.arctain.snowTerritory.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** 配置工具：默认文件写入、消息递归加载。 */
public final class ConfigUtils {

    private ConfigUtils() {
    }

    public static void copyIfMissing(File target, String content) {
        try {
            if (!target.exists()) {
                if (target.getParentFile() != null) {
                    target.getParentFile().mkdirs();
                }
                java.nio.file.Files.writeString(target.toPath(), content);
            }
        } catch (IOException e) {
            MessageUtils.logError("写入默认配置失败: " + target.getAbsolutePath() + " - " + e.getMessage());
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
