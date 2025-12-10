package top.arctain.snowTerritory.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PluginConfig {

    private final Main plugin;
    private File configFile;
    private FileConfiguration config;

    // 从配置加载的可修改数据
    private Map<Integer, Double> reinforceSuccessRates;  // 不同等级的成功概率（key: 等级, value: 概率0.0-1.0）
    private double reinforceFailDegradeChance;  // 失败时降级概率（0.0-1.0）
    private double reinforceMaintainChance;  // 维持不变概率（剩余为成功）
    private double attributeBoostPercent;  // 成功时属性增加百分比（例如1.1表示+10%）
    private List<String> reinforceableItems;  // 可强化物品列表（基于NBT的name或lore匹配字符串）
    private List<String> reinforceableAttributes;  // 可强化属性列表（例如: ATTACK_DAMAGE, DEFENSE）
    private int costVaultGold;  // 每次强化消耗的Vault金币
    private int costPlayerPoints;  // 每次强化消耗的PlayerPoints点券
    private int costMaterials;  // 所需强化材料数量（检查GUI中的6个槽位）

    // GUI相关
    private String guiTitle;
    private int guiSize;
    private int slotWeapon;  // 武器槽位
    private int slotProtectCharm;  // 保护符槽位
    private int slotEnhanceCharm;  // 强化符槽位
    private int[] slotMaterials;  // 6个材料槽位数组
    private int slotConfirm;  // 确认按钮槽位
    private int slotCancel;  // 取消按钮槽位
    private String guiConfirmButtonName;
    private String guiCancelButtonName;
    private Map<Integer, ItemConfig> customSlots;  // 自定义槽位（槽位: ItemConfig）
    
    // 消息配置
    private Map<String, String> messages;  // 消息映射表

    public PluginConfig(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.reinforceSuccessRates = new HashMap<>();
        this.customSlots = new HashMap<>();
        this.slotMaterials = new int[6];
        this.messages = new HashMap<>();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 加载强化相关
        reinforceFailDegradeChance = config.getDouble("reinforce.fail-degrade-chance", 0.3);
        reinforceMaintainChance = config.getDouble("reinforce.maintain-chance", 0.2);
        attributeBoostPercent = config.getDouble("reinforce.attribute-boost-percent", 1.1);
        reinforceableItems = config.getStringList("reinforce.reinforceable-items");
        reinforceableAttributes = config.getStringList("reinforce.reinforceable-attributes");
        costVaultGold = config.getInt("reinforce.cost.vault-gold", 1000);
        costPlayerPoints = config.getInt("reinforce.cost.player-points", 50);
        costMaterials = config.getInt("reinforce.cost.materials", 6);

        // 加载不同等级的成功概率（例如 level-1: 0.8）
        if (config.getConfigurationSection("reinforce.success-rates") != null) {
            for (String key : config.getConfigurationSection("reinforce.success-rates").getKeys(false)) {
                if (key.startsWith("level-")) {
                    try {
                        int level = Integer.parseInt(key.substring(6));
                        double rate = config.getDouble("reinforce.success-rates." + key);
                        reinforceSuccessRates.put(level, rate);
                    } catch (NumberFormatException e) {
                        MessageUtils.logWarning("无效的等级配置: " + key);
                    }
                }
            }
        }

        // 加载GUI相关
        guiTitle = config.getString("gui.title", "强化MMO物品");
        guiSize = config.getInt("gui.size", 54);
        slotWeapon = config.getInt("gui.slots.weapon", 10);
        slotProtectCharm = config.getInt("gui.slots.protect-charm", 12);
        slotEnhanceCharm = config.getInt("gui.slots.enhance-charm", 14);
        slotMaterials[0] = config.getInt("gui.slots.materials.1", 28);
        slotMaterials[1] = config.getInt("gui.slots.materials.2", 29);
        slotMaterials[2] = config.getInt("gui.slots.materials.3", 30);
        slotMaterials[3] = config.getInt("gui.slots.materials.4", 37);
        slotMaterials[4] = config.getInt("gui.slots.materials.5", 38);
        slotMaterials[5] = config.getInt("gui.slots.materials.6", 39);
        slotConfirm = config.getInt("gui.slots.confirm", 22);
        slotCancel = config.getInt("gui.slots.cancel", 40);
        guiConfirmButtonName = config.getString("gui.confirm-button", "&a确认强化");
        guiCancelButtonName = config.getString("gui.cancel-button", "&c取消");

        // 加载自定义槽位（支持16进制颜色，例如 &x&F&F&0&0&0&0）
        if (config.getConfigurationSection("gui.custom-slots") != null) {
            for (String key : config.getConfigurationSection("gui.custom-slots").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    String material = config.getString("gui.custom-slots." + key + ".material", "GLASS_PANE");
                    String name = config.getString("gui.custom-slots." + key + ".name", "");
                    try {
                        Material mat = Material.valueOf(material.toUpperCase());
                        customSlots.put(slot, new ItemConfig(mat, name));
                    } catch (IllegalArgumentException e) {
                        MessageUtils.logWarning("无效的材料类型: " + material + " (槽位: " + slot + ")");
                    }
                } catch (NumberFormatException e) {
                    MessageUtils.logWarning("无效的槽位号: " + key);
                }
            }
        }

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

    // 内部类：自定义物品配置
    @Getter
    public static class ItemConfig {
        private final Material material;
        private final String name;

        public ItemConfig(Material material, String name) {
            this.material = material;
            this.name = name;
        }
    }

    // 获取指定等级的成功概率（如果不存在，使用默认或最低）
    public double getSuccessRateForLevel(int level) {
        return reinforceSuccessRates.getOrDefault(level, 0.5);  // 默认0.5，如果未配置
    }
}