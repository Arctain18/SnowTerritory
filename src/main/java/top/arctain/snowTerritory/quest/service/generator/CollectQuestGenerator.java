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
 * 采集任务生成器
 */
public class CollectQuestGenerator implements QuestGenerator {

    private static final String CROP_KEY_PREFIX = "CROP:";
    private static final int DEFAULT_MIN_AMOUNT = 16;
    private static final int DEFAULT_MAX_AMOUNT = 64;
    private static final int DEFAULT_CROP_LEVEL = 1;
    private static final long DEFAULT_TIME_LIMIT = 3600000L;
    private static final int BOUNTY_FIXED_DIFFICULTY = 16;

    private final QuestConfigManager configManager;
    private final QuestDatabaseDao databaseDao;
    private final Random random;

    public CollectQuestGenerator(QuestConfigManager configManager, QuestDatabaseDao databaseDao) {
        this.configManager = configManager;
        this.databaseDao = databaseDao;
        this.random = new Random();
    }

    CollectQuestGenerator(QuestConfigManager configManager, QuestDatabaseDao databaseDao, Random random) {
        this.configManager = configManager;
        this.databaseDao = databaseDao;
        this.random = random;
    }

    @Override
    public Quest generate(UUID playerId, QuestType type, QuestReleaseMethod releaseMethod) {
        if (!supports(type)) {
            return null;
        }

        FileConfiguration whitelist = configManager.getCropsWhitelist();
        FileConfiguration tasksCollect = configManager.getTasksCollect();
        if (whitelist == null || tasksCollect == null) {
            return null;
        }

        ConfigurationSection cropsSection = whitelist.getConfigurationSection("crops");
        if (cropsSection == null) {
            return null;
        }

        final int maxCropLevel = (playerId != null && releaseMethod == QuestReleaseMethod.NORMAL)
                ? databaseDao.getMaxMaterialLevel(playerId) : 1;

        List<CropEntry> crops = collectCrops(cropsSection);
        if (crops.isEmpty()) {
            return null;
        }

        CropEntry selected;
        if (releaseMethod == QuestReleaseMethod.NORMAL) {
            List<CropEntry> filtered = crops.stream()
                    .filter(c -> c.cropLevel <= maxCropLevel)
                    .toList();
            if (filtered.isEmpty()) {
                return null;
            }
            selected = filtered.get(random.nextInt(filtered.size()));
        } else {
            selected = crops.get(random.nextInt(crops.size()));
        }

        int requiredAmount = calculateRequiredAmount(selected);
        int difficulty = QuestUtils.calculateDifficulty(requiredAmount, selected.min, selected.max);
        if (releaseMethod == QuestReleaseMethod.BOUNTY) {
            difficulty = BOUNTY_FIXED_DIFFICULTY;
            requiredAmount = QuestUtils.calculateRequiredAmount(difficulty, selected.min, selected.max);
        }

        int level = selected.cropLevel;
        long timeLimit = tasksCollect.getLong("collect.default-time-limit", DEFAULT_TIME_LIMIT);
        String materialKey = CROP_KEY_PREFIX + selected.cropType;
        String materialName = selected.displayName;

        return new Quest(
                UUID.randomUUID(),
                playerId,
                QuestType.COLLECT,
                releaseMethod,
                materialKey,
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

    private int calculateRequiredAmount(CropEntry selected) {
        return (int) (selected.min + random.nextInt(selected.max - selected.min + 1) * 0.5 + 0.5);
    }

    @Override
    public boolean supports(QuestType type) {
        return type == QuestType.COLLECT;
    }

    private List<CropEntry> collectCrops(ConfigurationSection cropsSection) {
        List<CropEntry> crops = new ArrayList<>();
        for (String cropType : cropsSection.getKeys(false)) {
            ConfigurationSection section = cropsSection.getConfigurationSection(cropType);
            CropEntry entry = createCropEntry(cropType, section);
            if (entry != null) {
                crops.add(entry);
            }
        }
        return crops;
    }

    private CropEntry createCropEntry(String cropType, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String displayName = section.getString("display-name", cropType);
        String mmoType = section.getString("mmo-type");
        String mmoId = section.getString("mmo-id");
        if (mmoType == null || mmoId == null) {
            return null;
        }
        int min = section.getInt("min", DEFAULT_MIN_AMOUNT);
        int max = section.getInt("max", DEFAULT_MAX_AMOUNT);
        int cropLevel = section.getInt("crop-level", DEFAULT_CROP_LEVEL);
        return new CropEntry(cropType, displayName, mmoType, mmoId, min, max, cropLevel);
    }

    private static class CropEntry {
        final String cropType;
        final String displayName;
        final String mmoType;
        final String mmoId;
        final int min;
        final int max;
        final int cropLevel;

        CropEntry(String cropType, String displayName, String mmoType, String mmoId, int min, int max, int cropLevel) {
            this.cropType = cropType;
            this.displayName = displayName;
            this.mmoType = mmoType;
            this.mmoId = mmoId;
            this.min = min;
            this.max = max;
            this.cropLevel = cropLevel;
        }
    }
}
