package top.arctain.snowTerritory.reinforce.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.data.CostConfig;
import top.arctain.snowTerritory.reinforce.service.ExpressionService;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * 消耗配置管理器
 * 扫描Reinforce/目录下所有yml文件，解析物品消耗配置
 */
public class CostConfigManager {

    private final Main plugin;
    private final File baseDir;
    
    @Getter
    private final Map<String, ItemCostConfig> itemCostConfigs = new HashMap<>();
    
    private final ExpressionService expressionService;

    public CostConfigManager(Main plugin, ExpressionService expressionService) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "reinforce");
        this.expressionService = expressionService;
    }

    /**
     * 加载所有配置
     */
    public void loadAll() {
        itemCostConfigs.clear();
        
        // 确保目录存在
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 reinforce 目录失败: " + baseDir.getAbsolutePath());
            return;
        }

        // 自动生成示例配置文件（如果不存在）
        ensureExampleFile();

        // 扫描并加载所有yml文件（排除config.yml和example-cost.yml）
        File[] files = baseDir.listFiles((dir, name) -> 
            name.endsWith(".yml") && !name.equals("config.yml") && !name.equals("example-cost.yml")
        );
        
        if (files != null) {
            for (File file : files) {
                loadItemConfigFile(file);
            }
        }
        
        MessageUtils.logSuccess("已加载 " + itemCostConfigs.size() + " 个物品的消耗配置");
    }

    /**
     * 确保示例配置文件存在
     */
    private void ensureExampleFile() {
        File exampleFile = new File(baseDir, "example-cost.yml");
        if (!exampleFile.exists()) {
            try {
                if (exampleFile.getParentFile() != null) {
                    exampleFile.getParentFile().mkdirs();
                }
                Files.writeString(exampleFile.toPath(), DefaultFiles.DEFAULT_ITEM_COST_EXAMPLE);
                MessageUtils.logInfo("已创建物品消耗配置示例文件: example-cost.yml");
            } catch (IOException e) {
                MessageUtils.logError("创建示例配置文件失败: " + e.getMessage());
            }
        }
    }

    /**
     * 加载物品配置文件
     */
    private void loadItemConfigFile(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        for (String itemId : config.getKeys(false)) {
            ConfigurationSection itemSection = config.getConfigurationSection(itemId);
            if (itemSection == null) continue;
            
            ItemCostConfig itemConfig = new ItemCostConfig();
            itemConfig.setEnabled(itemSection.getBoolean("enabled", true));
            
            // 加载金币消耗配置
            ConfigurationSection goldSection = itemSection.getConfigurationSection("gold-cost");
            if (goldSection != null) {
                CostConfig goldConfig = new CostConfig();
                goldConfig.setEnabled(goldSection.getBoolean("enabled", false));
                goldConfig.setBaseExpression(goldSection.getString("base-expression", ""));
                goldConfig.setLevelMultiplier(goldSection.getString("level-multiplier", "1"));
                itemConfig.setGoldCost(goldConfig);
            }
            
            // 加载点券消耗配置
            ConfigurationSection pointsSection = itemSection.getConfigurationSection("points-cost");
            if (pointsSection != null) {
                CostConfig pointsConfig = new CostConfig();
                pointsConfig.setEnabled(pointsSection.getBoolean("enabled", false));
                pointsConfig.setBaseExpression(pointsSection.getString("base-expression", ""));
                pointsConfig.setLevelMultiplier(pointsSection.getString("level-multiplier", "1"));
                itemConfig.setPointsCost(pointsConfig);
            }
            
            itemCostConfigs.put(itemId, itemConfig);
        }
    }

    /**
     * 根据物品ID获取消耗配置
     */
    public ItemCostConfig getItemCostConfig(String itemId) {
        return itemCostConfigs.get(itemId);
    }

    /**
     * 重载配置
     */
    public void reload() {
        loadAll();
    }

    /**
     * 物品消耗配置
     */
    @Getter
    public static class ItemCostConfig {
        private boolean enabled = true;
        private CostConfig goldCost;
        private CostConfig pointsCost;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public CostConfig getGoldCost() {
            return goldCost;
        }
        
        public void setGoldCost(CostConfig goldCost) {
            this.goldCost = goldCost;
        }
        
        public CostConfig getPointsCost() {
            return pointsCost;
        }
        
        public void setPointsCost(CostConfig pointsCost) {
            this.pointsCost = pointsCost;
        }
    }
}
