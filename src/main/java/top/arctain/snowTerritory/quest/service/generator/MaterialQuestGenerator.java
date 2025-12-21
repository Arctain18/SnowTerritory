package top.arctain.snowTerritory.quest.service.generator;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestReleaseMethod;
import top.arctain.snowTerritory.quest.data.QuestStatus;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.utils.QuestUtils;

import java.util.*;

/**
 * 材料收集任务生成器
 */
public class MaterialQuestGenerator implements QuestGenerator {
    
    private static final int DEFAULT_MIN_AMOUNT = 16;
    private static final int DEFAULT_MAX_AMOUNT = 256;
    private static final int DEFAULT_MATERIAL_LEVEL = 1;
    private static final long DEFAULT_TIME_LIMIT = 3600000L;
    
    private final QuestConfigManager configManager;
    private final Random random;
    
    public MaterialQuestGenerator(QuestConfigManager configManager) {
        this.configManager = configManager;
        this.random = new Random();
    }
    
    // 用于测试的构造函数
    MaterialQuestGenerator(QuestConfigManager configManager, Random random) {
        this.configManager = configManager;
        this.random = random;
    }
    
    @Override
    public Quest generate(UUID playerId, QuestType type, QuestReleaseMethod releaseMethod) {
        if (!supports(type)) {
            return null;
        }
        
        FileConfiguration whitelist = configManager.getMaterialsWhitelist();
        FileConfiguration tasksMaterial = configManager.getTasksMaterial();
        
        if (whitelist == null || tasksMaterial == null) {
            return null;
        }
        
        ConfigurationSection materialsSection = whitelist.getConfigurationSection("materials");
        if (materialsSection == null) {
            return null;
        }
        
        List<MaterialEntry> materials = collectMaterials(materialsSection);
        if (materials.isEmpty()) {
            return null;
        }
        
        MaterialEntry selected = materials.get(random.nextInt(materials.size()));
        int requiredAmount = selected.min + random.nextInt(selected.max - selected.min + 1);
        int level = QuestUtils.calculateQuestLevel(selected.materialLevel, requiredAmount);
        long timeLimit = tasksMaterial.getLong("material.default-time-limit", DEFAULT_TIME_LIMIT);

        // 从 key (TYPE:NAME) 中解析出 type 和 name，然后获取 MMOItem 的展示名称
        String materialName = QuestUtils.getMMOItemDisplayName(selected.key);
        
        return new Quest(
                UUID.randomUUID(),
                playerId,
                QuestType.MATERIAL,
                releaseMethod,
                selected.key,
                materialName,
                requiredAmount,
                0,
                System.currentTimeMillis(),
                timeLimit,
                level,
                QuestStatus.ACTIVE
        );
    }
    
    @Override
    public boolean supports(QuestType type) {
        return type == QuestType.MATERIAL;
    }
    
    private List<MaterialEntry> collectMaterials(ConfigurationSection materialsSection) {
        List<MaterialEntry> materials = new ArrayList<>();
        
        for (String type : materialsSection.getKeys(false)) {
            ConfigurationSection typeSection = materialsSection.getConfigurationSection(type);
            if (typeSection == null) {
                continue;
            }
            
            for (String name : typeSection.getKeys(false)) {
                ConfigurationSection itemSection = typeSection.getConfigurationSection(name);
                materials.add(createMaterialEntry(type, name, itemSection));
            }
        }
        
        return materials;
    }
    
    private MaterialEntry createMaterialEntry(String type, String name, ConfigurationSection section) {
        String key = type + ":" + name;
        int min = section != null ? section.getInt("min", DEFAULT_MIN_AMOUNT) : DEFAULT_MIN_AMOUNT;
        int max = section != null ? section.getInt("max", DEFAULT_MAX_AMOUNT) : DEFAULT_MAX_AMOUNT;
        int materialLevel = section != null ? section.getInt("material-level", DEFAULT_MATERIAL_LEVEL) : DEFAULT_MATERIAL_LEVEL;
        return new MaterialEntry(key, name, min, max, materialLevel);
    }
    
    /**
     * 材料条目信息
     */
    private static class MaterialEntry {
        final String key;
        final String name;
        final int min;
        final int max;
        final int materialLevel;
        
        MaterialEntry(String key, String name, int min, int max, int materialLevel) {
            this.key = key;
            this.name = name;
            this.min = min;
            this.max = max;
            this.materialLevel = materialLevel;
        }
    }
}

