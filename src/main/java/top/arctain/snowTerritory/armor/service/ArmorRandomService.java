package top.arctain.snowTerritory.armor.service;

import top.arctain.snowTerritory.armor.config.ArmorConfigManager;
import top.arctain.snowTerritory.armor.data.ArmorQuality;
import top.arctain.snowTerritory.armor.data.ArmorSetDefinition;
import top.arctain.snowTerritory.armor.data.ArmorStatRange;
import top.arctain.snowTerritory.armor.data.ArmorStats;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ArmorRandomService {

    private static final String[] QUALITY_ROLL_ORDER = {"common", "rare", "epic"};

    private final ArmorConfigManager config;
    private final Random random = new Random();

    public ArmorRandomService(ArmorConfigManager config) {
        this.config = config;
    }

    public ArmorQuality rollQuality() {
        return rollQuality(null);
    }

    public ArmorQuality rollQuality(int[] weightOverride) {
        if (weightOverride != null && weightOverride.length == 3) {
            ArmorQuality rolled = rollQualityOrderedWeights(weightOverride);
            if (rolled != null) {
                return rolled;
            }
        }
        Map<String, ArmorQuality> qualities = config.getQualities();
        if (qualities.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (ArmorQuality q : qualities.values()) {
            totalWeight += Math.max(0, q.getWeight());
        }
        if (totalWeight <= 0) {
            return qualities.values().iterator().next();
        }
        int roll = random.nextInt(totalWeight);
        int sum = 0;
        for (ArmorQuality q : qualities.values()) {
            sum += Math.max(0, q.getWeight());
            if (roll < sum) {
                return q;
            }
        }
        return qualities.values().iterator().next();
    }

    private ArmorQuality rollQualityOrderedWeights(int[] weights) {
        Map<String, ArmorQuality> qualities = config.getQualities();
        int total = 0;
        for (int i = 0; i < QUALITY_ROLL_ORDER.length; i++) {
            ArmorQuality q = qualities.get(QUALITY_ROLL_ORDER[i]);
            if (q == null) {
                continue;
            }
            total += Math.max(0, weights[i]);
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        int sum = 0;
        for (int i = 0; i < QUALITY_ROLL_ORDER.length; i++) {
            ArmorQuality q = qualities.get(QUALITY_ROLL_ORDER[i]);
            if (q == null) {
                continue;
            }
            sum += Math.max(0, weights[i]);
            if (roll < sum) {
                return q;
            }
        }
        for (String id : QUALITY_ROLL_ORDER) {
            ArmorQuality q = qualities.get(id);
            if (q != null) {
                return q;
            }
        }
        return null;
    }

    public ArmorStats rollStatsForSlot(ArmorSetDefinition set, String slotId, ArmorQuality quality) {
        Map<String, Double> result = new HashMap<>();
        double slotRatio = set.getSlotRatio(slotId);
        if (slotRatio <= 0) {
            return new ArmorStats(result);
        }
        double qualityMul = quality != null ? quality.getValueMultiplier() : 1.0;
        for (Map.Entry<String, Double> entry : set.getBaseStats().entrySet()) {
            String statKey = entry.getKey();
            double base = entry.getValue();
            double baseForSlot = base * slotRatio * qualityMul;
            ArmorStatRange range = set.getStatRange(statKey);
            double minMul = range != null ? range.getMinMultiplier() : config.getGlobalMinMultiplier();
            double maxMul = range != null ? range.getMaxMultiplier() : config.getGlobalMaxMultiplier();
            double value = baseForSlot;
            if (config.isNormalEnabled()) {
                value = applyNormalRandom(baseForSlot, minMul, maxMul, config.getSigmaFactor());
            } else {
                double factor = minMul + random.nextDouble() * (maxMul - minMul);
                value = baseForSlot * factor;
            }
            result.put(statKey, Math.max(0.0, value));
        }
        return new ArmorStats(result);
    }

    private double applyNormalRandom(double base, double minMul, double maxMul, double sigmaFactor) {
        double mean = base;
        double sigma = Math.abs(base) * Math.abs(sigmaFactor);
        if (sigma <= 0) {
            double clampedMul = clamp(1.0, minMul, maxMul);
            return base * clampedMul;
        }
        double gaussian = random.nextGaussian();
        double value = mean + gaussian * sigma;
        double mul = value / (base == 0 ? 1.0 : base);
        mul = clamp(mul, minMul, maxMul);
        return base * mul;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}

