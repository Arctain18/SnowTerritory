package top.arctain.snowTerritory.stfish.data;

import org.bukkit.Material;

/** 鱼种数据，包含名称、描述、长度范围与显示材质。 */
public record FishDefinition(
        String id,
        String name,
        String description,
        double lengthMin,
        double lengthMax,
        Material material
) {
}
