package top.arctain.snowTerritory.life.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.life.config.LifeConfigManager;
import top.arctain.snowTerritory.life.data.LifeDatabaseDao;
import top.arctain.snowTerritory.life.data.LifeSkillProgress;
import top.arctain.snowTerritory.life.data.LifeSkillType;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LifeServiceImpl implements LifeService {

    private static final String CROP_KEY_PREFIX = "CROP:";

    private final LifeConfigManager configManager;
    private final LifeDatabaseDao databaseDao;
    private final QuestService questService;

    public LifeServiceImpl(LifeConfigManager configManager, LifeDatabaseDao databaseDao, QuestService questService) {
        this.configManager = configManager;
        this.databaseDao = databaseDao;
        this.questService = questService;
    }

    @Override
    public int addExperience(UUID playerId, LifeSkillType skillType, int amount) {
        Objects.requireNonNull(playerId, "playerId不能为null");
        Objects.requireNonNull(skillType, "skillType不能为null");
        if (amount <= 0) {
            return databaseDao.getExperience(playerId, skillType);
        }
        int maxLevel = getMaxLevel();
        int currentExp = databaseDao.getExperience(playerId, skillType);
        int maxExp = totalExpToReachLevel(maxLevel);
        int nextExp = Math.min(maxExp, currentExp + amount);
        databaseDao.setExperience(playerId, skillType, nextExp);
        return nextExp;
    }

    @Override
    public LifeSkillProgress getProgress(UUID playerId, LifeSkillType skillType) {
        Objects.requireNonNull(playerId, "playerId不能为null");
        Objects.requireNonNull(skillType, "skillType不能为null");
        int totalExp = databaseDao.getExperience(playerId, skillType);
        return buildProgress(skillType, totalExp);
    }

    @Override
    public List<LifeSkillProgress> getAllProgress(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId不能为null");
        Map<LifeSkillType, Integer> all = databaseDao.getAllExperience(playerId);
        List<LifeSkillProgress> list = new ArrayList<>();
        for (LifeSkillType skillType : LifeSkillType.values()) {
            list.add(buildProgress(skillType, all.getOrDefault(skillType, 0)));
        }
        list.sort(Comparator.comparingInt(this::skillOrder));
        return list;
    }

    @Override
    public int resolveQualityLevel(Player player, String cropType) {
        Objects.requireNonNull(player, "player不能为null");
        LifeSkillProgress progress = getProgress(player.getUniqueId(), LifeSkillType.GATHERING);
        int level = progress.level();
        FileConfiguration config = configManager.getConfig();
        if (config == null) {
            return 1;
        }
        ConfigurationSection weightsRoot = config.getConfigurationSection("gathering.quality-weights");
        if (weightsRoot == null) {
            return 1;
        }
        int[] weights = resolveWeightsByLevel(weightsRoot, level);
        if (level >= config.getInt("gathering.quality1-disabled-level", 16)) {
            weights[0] = 0;
            if (weights[1] <= 0 && weights[2] <= 0) {
                weights[1] = 50;
                weights[2] = 50;
            }
        }
        int quality = rollQuality(weights[0], weights[1], weights[2]);
        if (quality == 1) {
            MessageUtils.sendConfigRaw(player, "life.quality1-warning",
                    "&8*由于你的采集技能不够熟练，你不小心踩坏了这个农田");
        }
        return quality;
    }

    @Override
    public void handleGatheringQuestProgress(Player player, String cropType) {
        if (questService == null || player == null || cropType == null || cropType.isBlank()) {
            return;
        }
        String cropKey = CROP_KEY_PREFIX + cropType.toUpperCase();
        boolean completedBounty = questService.checkCollectQuestProgress(player.getUniqueId(), cropKey, 1);
        if (completedBounty) {
            MessageUtils.sendConfigMessage(player, "quest.bounty-completed",
                    "&a✓ &f悬赏任务完成！使用 &e/sn q complete &f领取奖励");
        }
    }

    private LifeSkillProgress buildProgress(LifeSkillType skillType, int totalExp) {
        int maxLevel = getMaxLevel();
        int cappedExp = Math.max(0, Math.min(totalExp, totalExpToReachLevel(maxLevel)));
        int level = 1;
        int requiredToNext = requiredExpForLevel(1);
        int accumulated = 0;
        while (level < maxLevel) {
            requiredToNext = requiredExpForLevel(level);
            if (cappedExp < accumulated + requiredToNext) {
                break;
            }
            accumulated += requiredToNext;
            level++;
        }
        if (level >= maxLevel) {
            return new LifeSkillProgress(skillType, maxLevel, 0, 0, cappedExp, 1.0);
        }
        int currentExp = Math.max(0, cappedExp - accumulated);
        double ratio = requiredToNext <= 0 ? 1.0 : Math.min(1.0, currentExp / (double) requiredToNext);
        return new LifeSkillProgress(skillType, level, currentExp, requiredToNext, cappedExp, ratio);
    }

    private int totalExpToReachLevel(int level) {
        int total = 0;
        for (int i = 1; i < level; i++) {
            total += requiredExpForLevel(i);
        }
        return Math.max(0, total);
    }

    private int requiredExpForLevel(int level) {
        FileConfiguration config = configManager.getConfig();
        int base = config != null ? config.getInt("exp-function.base", 100) : 100;
        int step = config != null ? config.getInt("exp-function.step", 100) : 100;
        int value = base + Math.max(0, level - 1) * step;
        return Math.max(1, value);
    }

    private int getMaxLevel() {
        FileConfiguration config = configManager.getConfig();
        return config != null ? Math.max(1, config.getInt("max-level", 50)) : 50;
    }

    private int[] resolveWeightsByLevel(ConfigurationSection weightsRoot, int level) {
        for (String key : weightsRoot.getKeys(false)) {
            ConfigurationSection sec = weightsRoot.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            int min = sec.getInt("min-level", 1);
            int max = sec.getInt("max-level", Integer.MAX_VALUE);
            if (level < min || level > max) {
                continue;
            }
            ConfigurationSection weights = sec.getConfigurationSection("weights");
            if (weights == null) {
                continue;
            }
            int w1 = Math.max(0, weights.getInt("1", 0));
            int w2 = Math.max(0, weights.getInt("2", 0));
            int w3 = Math.max(0, weights.getInt("3", 0));
            if (w1 + w2 + w3 > 0) {
                return new int[]{w1, w2, w3};
            }
        }
        return new int[]{60, 30, 10};
    }

    private int rollQuality(int w1, int w2, int w3) {
        int total = Math.max(0, w1) + Math.max(0, w2) + Math.max(0, w3);
        if (total <= 0) {
            return 1;
        }
        int n = ThreadLocalRandom.current().nextInt(total) + 1;
        if (n <= w1) {
            return 1;
        }
        if (n <= w1 + w2) {
            return 2;
        }
        return 3;
    }

    private int skillOrder(LifeSkillProgress progress) {
        return switch (progress.skillType()) {
            case GATHERING -> 1;
            case FISHING -> 2;
            case EXPLORATION -> 3;
        };
    }
}
