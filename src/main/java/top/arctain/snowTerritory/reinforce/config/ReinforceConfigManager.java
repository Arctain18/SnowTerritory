package top.arctain.snowTerritory.reinforce.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.service.ExpressionService;
import top.arctain.snowTerritory.utils.MessageUtils;

import top.arctain.snowTerritory.utils.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class ReinforceConfigManager {

    private final Main plugin;
    private final File baseDir;
    private File configFile;
    private FileConfiguration config;
    
    // 新增：消耗配置管理器
    @Getter
    private CostConfigManager costConfigManager;

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
    
    // 保护符和强化符配置
    private String preservationTokenType;  // 保护符的MMOItems type
    private String upgradeTokenType;  // 强化符的MMOItems type

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
    private Map<Integer, top.arctain.snowTerritory.utils.GuiSlotUtils.SlotItem> customSlots;  // 自定义槽位（槽位: Item配置）
    
    // 确认按钮Lore配置
    private String confirmButtonLoreClickHint;
    private String confirmButtonLoreSeparator;
    private String confirmButtonLoreCurrentLevel;
    private String confirmButtonLoreSuccessRate;
    private String confirmButtonLoreFailDegradeChance;
    private String confirmButtonLoreProtectCharmExpired;
    private String confirmButtonLoreProtectCharmInvalid;
    private String confirmButtonLoreEnhanceCharmExpired;
    private String confirmButtonLoreEnhanceCharmInvalid;
    private String confirmButtonLoreCostTitle;
    private String confirmButtonLoreCostGold;
    private String confirmButtonLoreCostPoints;
    private String confirmButtonLoreCostMaterials;
    private String confirmButtonLoreNextCostTitle;
    
    // 玩家信息显示配置
    private boolean playerInfoEnabled;
    private int playerInfoSlot;
    private List<String> playerInfoFormat;
    
    // 消息配置
    private Map<String, String> messages;  // 消息映射表

    public ReinforceConfigManager(Main plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "reinforce");
        this.configFile = new File(baseDir, "config.yml");
        this.reinforceSuccessRates = new HashMap<>();
        this.customSlots = new HashMap<>();
        this.slotMaterials = new int[6];
        this.messages = new HashMap<>();
        
        // 初始化ExpressionService和CostConfigManager
        ExpressionService expressionService = new ExpressionService();
        this.costConfigManager = new CostConfigManager(plugin, expressionService);
    }

    public void loadAll() {
        ensureDefaults();
        loadMainConfig();

        // 加载强化相关
        reinforceFailDegradeChance = config.getDouble("reinforce.fail-degrade-chance", 0.3);
        reinforceMaintainChance = config.getDouble("reinforce.maintain-chance", 0.2);
        attributeBoostPercent = config.getDouble("reinforce.attribute-boost-percent", 1.1);
        reinforceableItems = config.getStringList("reinforce.reinforceable-items");
        reinforceableAttributes = config.getStringList("reinforce.reinforceable-attributes");
        costVaultGold = config.getInt("reinforce.cost.vault-gold", 1000);
        costPlayerPoints = config.getInt("reinforce.cost.player-points", 50);
        costMaterials = config.getInt("reinforce.cost.materials", 6);
        
        // 加载保护符和强化符配置
        preservationTokenType = config.getString("reinforce.preservation-token.type", "PRESERVATION_TOKEN");
        upgradeTokenType = config.getString("reinforce.upgrade-token.type", "UPGRADE_TOKEN");

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
        guiTitle = config.getString("gui.title", "&{#4a95ff}ST <#C9AE59>Reinforce");
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

        // 加载确认按钮Lore配置
        confirmButtonLoreClickHint = config.getString("gui.confirm-button-lore.click-hint", "&7点击确认强化");
        confirmButtonLoreSeparator = config.getString("gui.confirm-button-lore.separator", "<#d6fff6>&m-<#fffad6>&m=<#FFFFFF>&m---------------<#fffad6>&m=<#d6fff6>&m-");
        confirmButtonLoreCurrentLevel = config.getString("gui.confirm-button-lore.current-level", "&7当前等级: &e+{currentLevel} &7→ &a+{nextLevel}");
        confirmButtonLoreSuccessRate = config.getString("gui.confirm-button-lore.success-rate", "&7成功率: &a{successRate}%{bonus}");
        confirmButtonLoreFailDegradeChance = config.getString("gui.confirm-button-lore.fail-degrade-chance", "&7失败降级概率: &c{chance}%{protect}");
        confirmButtonLoreProtectCharmExpired = config.getString("gui.confirm-button-lore.protect-charm-expired", "&c&o保护符失效 (最高保护等级: +{maxLevel})");
        confirmButtonLoreProtectCharmInvalid = config.getString("gui.confirm-button-lore.protect-charm-invalid", "&c&o保护符无效 (类型不匹配)");
        confirmButtonLoreEnhanceCharmExpired = config.getString("gui.confirm-button-lore.enhance-charm-expired", "&c&o强化符失效 (最高强化等级: +{maxLevel})");
        confirmButtonLoreEnhanceCharmInvalid = config.getString("gui.confirm-button-lore.enhance-charm-invalid", "&c&o强化符无效 (类型不匹配或概率解析失败)");
        confirmButtonLoreCostTitle = config.getString("gui.confirm-button-lore.cost-title", "&7消耗资源:");
        confirmButtonLoreCostGold = config.getString("gui.confirm-button-lore.cost-gold", "&7金币: {color}{amount}");
        confirmButtonLoreCostPoints = config.getString("gui.confirm-button-lore.cost-points", "&7点券: {color}{amount}");
        confirmButtonLoreCostMaterials = config.getString("gui.confirm-button-lore.cost-materials", "&7材料: {color}{current}&7/{required}");
        confirmButtonLoreNextCostTitle = config.getString("gui.confirm-button-lore.next-cost-title", "&7下一级升级预估消耗:");

        // 加载玩家信息显示配置
        ConfigurationSection playerInfoSection = config.getConfigurationSection("gui.player-info");
        if (playerInfoSection != null) {
            playerInfoEnabled = playerInfoSection.getBoolean("enabled", true);
            playerInfoSlot = playerInfoSection.getInt("slot", 49); // 54格GUI最下面一排中间
            playerInfoFormat = playerInfoSection.getStringList("format");
            if (playerInfoFormat.isEmpty()) {
                // 默认格式
                playerInfoFormat = List.of(
                    "&7玩家: &e{playerName}",
                    "&7等级: &a{playerLevel}",
                    "&7职业: &b{className} &7(Lv.{classLevel})",
                    "&7金币: &6{gold}",
                    "&7点券: &d{points}"
                );
            }
        } else {
            playerInfoEnabled = false;
            playerInfoSlot = 49;
            playerInfoFormat = List.of();
        }

        // 加载消耗配置
        costConfigManager.loadAll();

        // 获取所有功能槽位集合（自定义槽位不能覆盖这些槽位）
        Set<Integer> functionalSlots = getFunctionalSlots();
        this.customSlots = top.arctain.snowTerritory.utils.GuiSlotUtils.loadSlotItems(
                config,
                "gui.custom-slots",
                functionalSlots,
                guiSize
        );

        // 加载消息配置
        loadMessages();

        MessageUtils.logSuccess("Reinforce 配置已加载，配置目录: plugins/SnowTerritory/reinforce/");
    }
    
    /**
     * 确保默认配置文件存在
     */
    private void ensureDefaults() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 reinforce 目录失败: " + baseDir.getAbsolutePath());
        }
        ConfigUtils.copyIfMissing(configFile, DefaultFiles.DEFAULT_CONFIG);
    }
    
    /**
     * 加载主配置文件
     */
    private void loadMainConfig() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    /**
     * 获取所有功能槽位的集合
     * 这些槽位不能被自定义槽位覆盖
     * 
     * @return 功能槽位集合
     */
    private Set<Integer> getFunctionalSlots() {
        Set<Integer> functionalSlots = new HashSet<>();
        
        // 添加武器槽位
        functionalSlots.add(slotWeapon);
        
        // 添加保护符槽位
        functionalSlots.add(slotProtectCharm);
        
        // 添加强化符槽位
        functionalSlots.add(slotEnhanceCharm);
        
        // 添加材料槽位（6个）
        for (int materialSlot : slotMaterials) {
            functionalSlots.add(materialSlot);
        }
        
        // 添加确认按钮槽位
        functionalSlots.add(slotConfirm);
        
        // 添加取消按钮槽位
        functionalSlots.add(slotCancel);
        
        // 添加玩家信息显示槽位（如果启用）
        if (playerInfoEnabled) {
            functionalSlots.add(playerInfoSlot);
        }
        
        return functionalSlots;
    }
    
    /**
     * 加载消息配置
     */
    private void loadMessages() {
        messages.clear();
        if (config.getConfigurationSection("messages") != null) {
            messages.putAll(ConfigUtils.loadMessagesRecursive("messages", config.getConfigurationSection("messages")));
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

    public void reload() {
        loadAll();  // 重用加载逻辑进行重载
        costConfigManager.reload();  // 重载消耗配置
        MessageUtils.logSuccess("Reinforce 配置已重载");
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            MessageUtils.logError("无法保存配置: " + e.getMessage());
        }
    }

    public File getBaseDir() {
        return baseDir;
    }

    public double getSuccessRateForLevel(int level) {
        return reinforceSuccessRates.getOrDefault(level, 0.5);  // 默认0.5，如果未配置
    }
}

