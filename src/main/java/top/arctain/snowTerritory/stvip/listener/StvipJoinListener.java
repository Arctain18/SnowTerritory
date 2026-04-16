package top.arctain.snowTerritory.stvip.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stvip.config.StvipConfigManager;
import top.arctain.snowTerritory.stvip.service.StvipService;
import top.arctain.snowTerritory.utils.MessageUtils;

/** VIP 进服提示（默认关闭，见 stvip/config.yml）。 */
public class StvipJoinListener implements Listener {

    private final Main plugin;
    private final StvipConfigManager configManager;
    private final StvipService service;

    public StvipJoinListener(Main plugin, StvipConfigManager configManager, StvipService service) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!configManager.isJoinMessageEnabled()) {
            return;
        }
        var player = event.getPlayer();
        var tierOpt = service.resolveTier(player);
        if (tierOpt.isEmpty()) {
            return;
        }
        String raw = configManager.getJoinMessage();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String msg = raw.replace("{display}", tierOpt.get().getDisplayName()).replace("{id}", tierOpt.get().getId());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(MessageUtils.colorize(msg));
            }
        }, 20L);
    }
}
