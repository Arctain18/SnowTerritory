package top.arctain.snowTerritory.enderstorage.config;

import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

public class ProgressionResolver {

    private final List<ProgressionLevel> sizeLevels;
    private final List<ProgressionLevel> stackLevels;

    public ProgressionResolver(ProgressionConfig config) {
        this.sizeLevels = config.getSizeLevels();
        this.stackLevels = config.getStackLevels();
    }

    public int resolveSlots(Player player) {
        return resolve(player, sizeLevels, 9);
    }

    public int resolvePerItemMax(Player player) {
        return resolve(player, stackLevels, 256);
    }

    private int resolve(Player player, List<ProgressionLevel> levels, int defaultValue) {
        return levels.stream()
                .sorted(Comparator.comparingInt(ProgressionLevel::getValue))
                .filter(l -> player.hasPermission(l.getPermission()))
                .map(ProgressionLevel::getValue)
                .reduce((first, second) -> second)
                .orElse(defaultValue);
    }
}

