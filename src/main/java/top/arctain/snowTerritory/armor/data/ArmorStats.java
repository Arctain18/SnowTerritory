package top.arctain.snowTerritory.armor.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ArmorStats {

    private final Map<String, Double> values;

    public ArmorStats(Map<String, Double> values) {
        this.values = new HashMap<>(values);
    }

    public double get(String key) {
        return values.getOrDefault(key, 0.0);
    }

    public Map<String, Double> asMap() {
        return Collections.unmodifiableMap(values);
    }
}

