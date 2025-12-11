package top.arctain.snowTerritory.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 插件主配置类
 * 注意：reinforce相关配置已移至 ReinforceConfigManager
 */
public class PluginConfig {

    private final Main plugin;
    private File configFile;
    private FileConfiguration config;
    
    // 消息配置
    private Map<String, String> messages;  // 消息映射表

    public PluginConfig(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.messages = new HashMap<>();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 加载消息配置
        loadMessages();

        MessageUtils.logSuccess("配置已加载");
    }
    
    /**
     * 加载消息配置
     */
    private void loadMessages() {
        messages.clear();
        if (config.getConfigurationSection("messages") != null) {
            loadMessagesRecursive("messages", config.getConfigurationSection("messages"));
        }
    }
    
    /**
     * 递归加载消息配置
     */
    private void loadMessagesRecursive(String path, org.bukkit.configuration.ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullPath = path + "." + key;
            if (section.isConfigurationSection(key)) {
                loadMessagesRecursive(fullPath, section.getConfigurationSection(key));
            } else {
                messages.put(fullPath, section.getString(key));
            }
        }
    }
    
    /**
     * 获取消息（支持占位符替换）
     */
    public String getMessage(String key, String... placeholders) {
        String message = messages.getOrDefault("messages." + key, "");
        if (message.isEmpty()) {
            return key; // 如果找不到消息，返回key
        }
        
        // 替换占位符 {placeholder}
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        
        return message;
    }
    
    /**
     * 获取消息前缀
     */
    public String getMessagePrefix() {
        return messages.getOrDefault("messages.prefix", "");
    }

    public void reloadConfig() {
        loadConfig();  // 重用加载逻辑进行重载
        MessageUtils.logSuccess("配置已重载");
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            MessageUtils.logError("无法保存配置: " + e.getMessage());
        }
    }
}
