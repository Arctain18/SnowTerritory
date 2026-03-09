package top.arctain.snowTerritory.stfish.util;

import org.bukkit.block.Biome;

import java.util.HashMap;
import java.util.Map;

/** 生物群系到中文显示名称的映射。 */
public final class BiomeNameHelper {

    private static final Map<Biome, String> NAMES = new HashMap<>();

    static {
        NAMES.put(Biome.RIVER, "清澜渡");
        NAMES.put(Biome.FROZEN_RIVER, "冰河");
        NAMES.put(Biome.OCEAN, "碧海");
        NAMES.put(Biome.DEEP_OCEAN, "深海");
        NAMES.put(Biome.COLD_OCEAN, "冰海");
        NAMES.put(Biome.DEEP_COLD_OCEAN, "深冰海");
        NAMES.put(Biome.FROZEN_OCEAN, "冰封之海");
        NAMES.put(Biome.DEEP_FROZEN_OCEAN, "深冰封之海");
        NAMES.put(Biome.LUKEWARM_OCEAN, "暖海");
        NAMES.put(Biome.DEEP_LUKEWARM_OCEAN, "深暖海");
        NAMES.put(Biome.WARM_OCEAN, "暖洋");
        NAMES.put(Biome.BEACH, "海滩");
        NAMES.put(Biome.SNOWY_BEACH, "雪滩");
        NAMES.put(Biome.STONY_SHORE, "石岸");
        NAMES.put(Biome.MANGROVE_SWAMP, "红树林");
        NAMES.put(Biome.SWAMP, "沼泽");
        NAMES.put(Biome.LUSH_CAVES, "繁茂洞穴");
        NAMES.put(Biome.DRIPSTONE_CAVES, "滴水石洞穴");
        NAMES.put(Biome.DEEP_DARK, "深暗之域");
        NAMES.put(Biome.MEADOW, "草甸");
        NAMES.put(Biome.PLAINS, "平原");
        NAMES.put(Biome.SUNFLOWER_PLAINS, "向日葵平原");
        NAMES.put(Biome.SNOWY_PLAINS, "雪原");
        NAMES.put(Biome.ICE_SPIKES, "冰刺之地");
        NAMES.put(Biome.DESERT, "沙漠");
        NAMES.put(Biome.FOREST, "森林");
        NAMES.put(Biome.FLOWER_FOREST, "繁花森林");
        NAMES.put(Biome.BIRCH_FOREST, "桦木林");
        NAMES.put(Biome.OLD_GROWTH_BIRCH_FOREST, "原始桦木林");
        NAMES.put(Biome.DARK_FOREST, "黑森林");
        NAMES.put(Biome.TAIGA, "针叶林");
        NAMES.put(Biome.SNOWY_TAIGA, "雪林");
        NAMES.put(Biome.OLD_GROWTH_PINE_TAIGA, "原始松林");
        NAMES.put(Biome.OLD_GROWTH_SPRUCE_TAIGA, "原始云杉林");
        NAMES.put(Biome.JUNGLE, "丛林");
        NAMES.put(Biome.SPARSE_JUNGLE, "稀疏丛林");
        NAMES.put(Biome.BAMBOO_JUNGLE, "竹林");
        NAMES.put(Biome.CHERRY_GROVE, "樱花林");
        NAMES.put(Biome.SAVANNA, "热带草原");
        NAMES.put(Biome.SAVANNA_PLATEAU, "热带高原");
        NAMES.put(Biome.WINDSWEPT_SAVANNA, "风袭草原");
        NAMES.put(Biome.BADLANDS, "恶地");
        NAMES.put(Biome.WOODED_BADLANDS, "疏林恶地");
        NAMES.put(Biome.ERODED_BADLANDS, "风蚀恶地");
        NAMES.put(Biome.GROVE, "雪林");
        NAMES.put(Biome.SNOWY_SLOPES, "雪坡");
        NAMES.put(Biome.FROZEN_PEAKS, "冰封山峰");
        NAMES.put(Biome.JAGGED_PEAKS, "尖峭山峰");
        NAMES.put(Biome.STONY_PEAKS, "裸岩山峰");
        NAMES.put(Biome.WINDSWEPT_HILLS, "风袭丘陵");
        NAMES.put(Biome.WINDSWEPT_GRAVELLY_HILLS, "风袭沙砾丘陵");
        NAMES.put(Biome.WINDSWEPT_FOREST, "风袭森林");
        NAMES.put(Biome.MUSHROOM_FIELDS, "蘑菇岛");
    }

    /** 获取生物群系的中文显示名，未映射则返回 key 的友好形式。 */
    public static String getDisplayName(Biome biome) {
        if (biome == null) return "水域";
        return NAMES.getOrDefault(biome, formatKey(biome.name()));
    }

    private static String formatKey(String key) {
        return key.toLowerCase().replace("_", " ");
    }
}
