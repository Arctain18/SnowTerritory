package top.arctain.snowTerritory.armor.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ArmorSetDefinition {

    private final String id;
    private final String displayName;
    private final Map<String, Double> baseStats;
    private final Map<String, Double> slotRatios;
    private final Map<String, ArmorStatRange> statRanges;
    private final ArmorBaseDefinition base;
    private final Map<String, String> slotMaterials;
    private final ArmorGenerationCost generationCost;

    public ArmorSetDefinition(String id,
                              String displayName,
                              Map<String, Double> baseStats,
                              Map<String, Double> slotRatios,
                              Map<String, ArmorStatRange> statRanges,
                              ArmorBaseDefinition base,
                              Map<String, String> slotMaterials,
                              ArmorGenerationCost generationCost) {
        this.id = id;
        this.displayName = displayName;
        this.baseStats = new HashMap<>(baseStats);
        this.slotRatios = new HashMap<>(slotRatios);
        this.statRanges = new HashMap<>(statRanges);
        this.base = base;
        this.slotMaterials = slotMaterials != null ? new HashMap<>(slotMaterials) : new HashMap<>();
        this.generationCost = generationCost;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Map<String, Double> getBaseStats() {
        return Collections.unmodifiableMap(baseStats);
    }

    public Map<String, Double> getSlotRatios() {
        return Collections.unmodifiableMap(slotRatios);
    }

    public Map<String, ArmorStatRange> getStatRanges() {
        return Collections.unmodifiableMap(statRanges);
    }

    public double getSlotRatio(String slotId) {
        return slotRatios.getOrDefault(slotId, 0.0);
    }

    public ArmorStatRange getStatRange(String statKey) {
        return statRanges.get(statKey);
    }

    public ArmorBaseDefinition getBase() {
        return base;
    }

    public String getSlotMaterial(String slotId) {
        if (slotId == null) return null;
        return slotMaterials.get(slotId);
    }

    public Map<String, String> getSlotMaterials() {
        return Collections.unmodifiableMap(slotMaterials);
    }

    public ArmorGenerationCost getGenerationCost() {
        return generationCost;
    }
}

