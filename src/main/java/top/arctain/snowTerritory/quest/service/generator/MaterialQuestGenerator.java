package top.arctain.snowTerritory.quest.service.generator;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestDatabaseDao;
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
    private static final int BOUNTY_FIXED_DIFFICULTY = 16; // 悬赏任务固定难度
    
    private final QuestConfigManager configManager;
    private final QuestDatabaseDao databaseDao;
    private final Random random;
    
    public MaterialQuestGenerator(QuestConfigManager configManager, QuestDatabaseDao databaseDao) {
        this.configManager = configManager;
        this.databaseDao = databaseDao;
        this.random = new Random();
    }
    
    // 用于测试的构造函数
    MaterialQuestGenerator(QuestConfigManager configManager, QuestDatabaseDao databaseDao, Random random) {
        this.configManager = configManager;
        this.databaseDao = databaseDao;
        this.random = random;
    }
    
    @Override
    public Quest generate(UUID playerId, QuestType type, QuestReleaseMethod releaseMethod, QuestGenerationContext context) {
        if (!supports(type) || context == null) {
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
        
        final int maxMaterialLevel = (playerId != null && releaseMethod == QuestReleaseMethod.NORMAL)
                ? databaseDao.getMaxMaterialLevel(playerId) : 1;
        
        List<MaterialEntry> materials = collectMaterials(materialsSection);
        if (materials.isEmpty()) {
            return null;
        }

        if (releaseMethod == QuestReleaseMethod.NORMAL) {
            materials.removeIf(material -> material.materialLevel > maxMaterialLevel);
            if (materials.isEmpty()) {
                return null;
            }
        }

        MaterialEntry selected = materials.get(random.nextInt(materials.size()));

        int requiredAmount;
        int difficulty;
        if (releaseMethod == QuestReleaseMethod.BOUNTY) {
            difficulty = BOUNTY_FIXED_DIFFICULTY;
            requiredAmount = QuestUtils.calculateRequiredAmount(difficulty, selected.min, selected.max);
        } else {
            int dMin = clampDifficulty(context.getMinDifficultyInclusive());
            int dMax = clampDifficulty(context.getMaxDifficultyInclusive());
            if (dMin > dMax) {
                int t = dMin;
                dMin = dMax;
                dMax = t;
            }
            if (dMin < 1) dMin = 1;
            if (dMax > QuestGenerationContext.MAX_DIFFICULTY) {
                dMax = QuestGenerationContext.MAX_DIFFICULTY;
            }
            if (dMin > dMax) {
                return null;
            }
            int d = dMin + (dMax > dMin ? random.nextInt(dMax - dMin + 1) : 0);
            difficulty = d;
            requiredAmount = QuestUtils.calculateRequiredAmount(d, selected.min, selected.max);
        }
        
        // 任务等级 = 材料等级（从配置读取）
        int level = selected.materialLevel;
        
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
                difficulty,
                QuestStatus.ACTIVE
        );
    }

    private static int clampDifficulty(int d) {
        if (d < 1) {
            return 1;
        }
        if (d > QuestGenerationContext.MAX_DIFFICULTY) {
            return QuestGenerationContext.MAX_DIFFICULTY;
        }
        return d;
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
                MaterialEntry entry = createMaterialEntry(type, name, itemSection);
                materials.add(entry);
                
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

