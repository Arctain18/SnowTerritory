package top.arctain.snowTerritory.armor.service;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 管理制式防具生成确认会话。 */
public class ArmorConfirmSessionService {

    private static final long EXPIRE_MS = 30_000L;

    private final Map<String, PendingGenerate> byToken = new ConcurrentHashMap<>();
    private final Map<UUID, String> byPlayer = new ConcurrentHashMap<>();

    public PendingGenerate create(Player player, String setId, String setDisplay, String profile, int[] weights,
                                  ArmorCostService.CostPreview costPreview) {
        clearExpired();
        String oldToken = byPlayer.remove(player.getUniqueId());
        if (oldToken != null) {
            byToken.remove(oldToken);
        }
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        PendingGenerate pending = new PendingGenerate(
                token,
                player.getUniqueId(),
                player.getName(),
                setId,
                setDisplay,
                profile,
                new int[]{weights[0], weights[1], weights[2]},
                costPreview,
                System.currentTimeMillis() + EXPIRE_MS
        );
        byToken.put(token, pending);
        byPlayer.put(player.getUniqueId(), token);
        return pending;
    }

    public PendingGenerate get(String token, Player player) {
        clearExpired();
        PendingGenerate pending = byToken.get(token);
        if (pending == null) {
            return null;
        }
        if (!pending.playerId().equals(player.getUniqueId())) {
            return null;
        }
        return pending;
    }

    public PendingGenerate consume(String token, Player player) {
        PendingGenerate pending = get(token, player);
        if (pending == null) {
            return null;
        }
        byToken.remove(token);
        byPlayer.remove(player.getUniqueId(), token);
        return pending;
    }

    public boolean cancel(String token, Player player) {
        PendingGenerate pending = get(token, player);
        if (pending == null) {
            return false;
        }
        byToken.remove(token);
        byPlayer.remove(player.getUniqueId(), token);
        return true;
    }

    private void clearExpired() {
        long now = System.currentTimeMillis();
        byToken.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().expireAt() < now;
            if (expired) {
                byPlayer.remove(entry.getValue().playerId(), entry.getKey());
            }
            return expired;
        });
    }

    public record PendingGenerate(
            String token,
            UUID playerId,
            String playerName,
            String setId,
            String setDisplay,
            String profile,
            int[] weights,
            ArmorCostService.CostPreview costPreview,
            long expireAt
    ) {
    }
}
