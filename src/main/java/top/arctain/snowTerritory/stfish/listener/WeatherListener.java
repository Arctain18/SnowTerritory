package top.arctain.snowTerritory.stfish.listener;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.Plugin;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

/** 监听天气变化，主世界 rain/storm 时全服广播。 */
public class WeatherListener implements Listener {

    private static final long VERIFY_DELAY_TICKS = 20L;

    private final Plugin plugin;
    private final StfishConfigManager configManager;

    public WeatherListener(Plugin plugin, StfishConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        World world = event.getWorld();
        if (!isTargetWorld(world)) return;
        if (!event.toWeatherState()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isTargetWorld(world)) return;
            if (!world.hasStorm()) return;
            String msg = MessageUtils.getConfigMessage("stfish.weather-rain-broadcast",
                    "&6✦ &e[天气] &7主世界开始降雨，钓鱼品质概率提升！");
            broadcast(ColorUtils.colorize(msg));
        }, VERIFY_DELAY_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {
        World world = event.getWorld();
        if (!isTargetWorld(world)) return;
        if (!event.toThunderState()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isTargetWorld(world)) return;
            if (!world.hasStorm() || !world.isThundering()) return;
            String msg = MessageUtils.getConfigMessage("stfish.weather-storm-broadcast",
                    "&6✦ &e[天气] &7主世界迎来暴风雨，钓鱼品质概率大幅提升！");
            broadcast(ColorUtils.colorize(msg));
        }, VERIFY_DELAY_TICKS);
    }

    private boolean isTargetWorld(World world) {
        if (world == null) return false;
        String name = configManager.getWeatherWorldName();
        if (name.isBlank()) {
            World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            return defaultWorld != null && world.getUID().equals(defaultWorld.getUID());
        }
        return world.getName().equalsIgnoreCase(name);
    }

    private void broadcast(String message) {
        for (var player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
}
