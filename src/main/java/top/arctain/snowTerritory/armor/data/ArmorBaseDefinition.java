package top.arctain.snowTerritory.armor.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArmorBaseDefinition {

    private final String set;
    private final int requiredLevel;
    private final List<String> lore;
    private final int[] dyeColor; // [r,g,b]

    public ArmorBaseDefinition(String set,
                                int requiredLevel,
                                List<String> lore,
                                int[] dyeColor) {
        this.set = set;
        this.requiredLevel = requiredLevel;
        this.lore = lore != null ? new ArrayList<>(lore) : List.of();
        this.dyeColor = dyeColor != null ? dyeColor.clone() : null;
    }

    public String getSet() {
        return set;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public int[] getDyeColor() {
        return dyeColor != null ? dyeColor.clone() : null;
    }
}

