package top.arctain.snowTerritory.commands;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 调试重置配置的确认处理器，需玩家在聊天框输入 yes 才执行。 */
public class DebugResetConfirmHandler {

    private static final long TIMEOUT_MS = 30_000;

    private final JavaPlugin plugin;
    private final Map<UUID, PendingReset> pending = new ConcurrentHashMap<>();

    public DebugResetConfirmHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, Runnable action, String scopeDesc) {
        pending.put(player.getUniqueId(), new PendingReset(action, System.currentTimeMillis() + TIMEOUT_MS, scopeDesc));
    }

    public boolean tryConfirm(Player player, String message) {
        PendingReset p = pending.get(player.getUniqueId());
        if (p == null) return false;
        if (System.currentTimeMillis() > p.expireAt) {
            pending.remove(player.getUniqueId());
            return false;
        }
        if (!message.trim().equalsIgnoreCase("yes")) {
            return false;
        }
        pending.remove(player.getUniqueId());
        plugin.getServer().getScheduler().runTask(plugin, p.action);
        return true;
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
    }

    private record PendingReset(Runnable action, long expireAt, String scopeDesc) {}
}
