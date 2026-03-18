package top.arctain.snowTerritory.stfish.service;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.concurrent.ThreadLocalRandom;

/** 天气召唤服务，扣款并设置主世界天气。 */
public class WeatherService {

    private final StfishConfigManager configManager;
    private final EconomyService economyService;

    public WeatherService(StfishConfigManager configManager, EconomyService economyService) {
        this.configManager = configManager;
        this.economyService = economyService;
    }

    /** 召唤天气，返回实际召唤的天气名称，失败返回 null。 */
    public String summon(Player player) {
        if (!economyService.isEnabled()) return null;

        World targetWorld = resolveTargetWorld();
        if (targetWorld == null) {
            return null;
        }

        double cost = configManager.getSummonCost();
        if (economyService.getBalance(player) < cost) return null;

        double stormChance = configManager.getStormChance();
        boolean toStorm = ThreadLocalRandom.current().nextDouble() < stormChance;

        economyService.withdraw(player, cost);

        targetWorld.setStorm(true);
        targetWorld.setThundering(toStorm);

        String weatherName = toStorm ? "暴风雨" : "雨天";
        String msg = MessageUtils.getConfigMessage("stfish.weather-summon-broadcast",
                "&6✦ &e[天气] &f{player} &7消耗金币召唤了 &e{weather} &7天气！",
                "player", player.getName(), "weather", weatherName);
        String colored = ColorUtils.colorize(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(colored);
        }

        return weatherName;
    }

    private World resolveTargetWorld() {
        String configured = configManager.getWeatherWorldName();
        if (configured.isBlank()) {
            return Bukkit.getWorlds().stream()
                    .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                    .findFirst()
                    .orElse(null);
        }

        World world = Bukkit.getWorld(configured);
        if (world != null) return world;

        MessageUtils.logWarning("天气世界未找到或未加载: " + configured);
        return null;
    }
}
