package top.arctain.snowTerritory.stvip.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.stvip.config.StvipConfigManager;
import top.arctain.snowTerritory.stvip.service.StvipService;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.List;
import java.util.OptionalInt;

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
        var player = event.getPlayer();
        var tierOpt = service.resolveTier(player);
        if (tierOpt.isEmpty()) {
            return;
        }
        int remoteRemaining = 0;
        int freeRemaining = 0;
        QuestService questService = plugin.getQuestModule() != null ? plugin.getQuestModule().getQuestService() : null;
        if (questService != null) {
            int remoteLimit = service.getQuestDailyRemoteClaimLimit(player);
            int freeLimit = service.getQuestDailyFreeClaimLimit(player);
            remoteRemaining = Math.max(0, remoteLimit - questService.getDailyRemoteClaimUsed(player.getUniqueId()));
            freeRemaining = Math.max(0, freeLimit - questService.getDailyFreeClaimUsed(player.getUniqueId()));
        }
        OptionalInt remainingDays = service.getRemainingVipDays(player);
        List<String> features = service.buildFeatureLines(player, remoteRemaining, freeRemaining);

        String raw = configManager.getJoinMessage();
        boolean showBase = configManager.isJoinMessageEnabled() && raw != null && !raw.isBlank();
        String safeRaw = raw == null ? "" : raw;
        String msg = showBase
                ? safeRaw.replace("{display}", tierOpt.get().getDisplayName())
                .replace("{id}", tierOpt.get().getId())
                .replace("{remote_remaining}", String.valueOf(remoteRemaining))
                .replace("{free_remaining}", String.valueOf(freeRemaining))
                : "";
        boolean needRemainAlert = remainingDays.isPresent() && configManager.getRemainDaysThresholds().contains(remainingDays.getAsInt());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                if (showBase || needRemainAlert) {
                    if (showBase) {
                        player.sendMessage(MessageUtils.colorize(msg));
                    }
                    for (String line : features) {
                        player.sendMessage(MessageUtils.colorize(line));
                    }
                }
                if (needRemainAlert) {
                    player.sendMessage(MessageUtils.colorize("&e⚠ &f你的 VIP 剩余 &e" + remainingDays.getAsInt() + " &f天，请及时续费"));
                    player.sendMessage(MessageUtils.colorize("&7续费提示: &f联系服主可续时长或升级档位"));
                }
            }
        }, 20L);
    }
}
