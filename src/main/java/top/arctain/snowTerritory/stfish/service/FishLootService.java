package top.arctain.snowTerritory.stfish.service;

import org.bukkit.World;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.stfish.data.FishDefinition;
import top.arctain.snowTerritory.stfish.data.FishTier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** 根据天气与权重抽取品质与鱼种。 */
public class FishLootService {

    private final StfishConfigManager configManager;

    public FishLootService(StfishConfigManager configManager) {
        this.configManager = configManager;
    }

    public String getWeatherKey(World world) {
        if (world == null) return "sun";
        if (world.isThundering()) return "storm";
        if (world.hasStorm()) return "rain";
        return "sun";
    }

    public FishTier rollTier(World world) {
        String weatherKey = getWeatherKey(world);
        Map<String, Integer> weights = configManager.getQualityWeights(weatherKey);
        int total = 0;
        for (Integer w : weights.values()) {
            total += Math.max(0, w);
        }
        if (total <= 0) return FishTier.COMMON;
        int roll = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (FishTier tier : FishTier.values()) {
            int w = weights.getOrDefault(tier.name().toLowerCase(), 0);
            if (w <= 0) continue;
            acc += w;
            if (roll < acc) return tier;
        }
        return FishTier.COMMON;
    }

    public FishDefinition rollFish(FishTier tier) {
        List<FishDefinition> list = configManager.getFishByTier().get(tier);
        if (list == null || list.isEmpty()) return null;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
