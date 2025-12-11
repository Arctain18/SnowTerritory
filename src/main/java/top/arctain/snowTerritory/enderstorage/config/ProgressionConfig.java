package top.arctain.snowTerritory.enderstorage.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgressionConfig {

    private final List<ProgressionLevel> sizeLevels;
    private final List<ProgressionLevel> stackLevels;

    public ProgressionConfig(FileConfiguration size, FileConfiguration stack) {
        this.sizeLevels = parse(size);
        this.stackLevels = parse(stack);
    }

    private List<ProgressionLevel> parse(FileConfiguration config) {
        List<ProgressionLevel> result = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("levels");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String path = "levels." + key;
                String perm = config.getString(path + ".perm", "");
                int value = config.getInt(path + ".slots", config.getInt(path + ".per_item_max", 0));
                result.add(new ProgressionLevel(perm, value));
            }
        } else if (config.isList("levels")) {
            List<?> list = config.getList("levels");
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Object o = list.get(i);
                    if (o instanceof ConfigurationSection cs) {
                        String perm = cs.getString("perm", "");
                        int value = cs.getInt("slots", cs.getInt("per_item_max", 0));
                        result.add(new ProgressionLevel(perm, value));
                    }
                }
            }
        }
        return result;
    }

    public List<ProgressionLevel> getSizeLevels() {
        return Collections.unmodifiableList(sizeLevels);
    }

    public List<ProgressionLevel> getStackLevels() {
        return Collections.unmodifiableList(stackLevels);
    }
}

