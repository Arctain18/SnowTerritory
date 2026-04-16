package top.arctain.snowTerritory.stfish.service;

import org.bukkit.entity.Player;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.stfish.data.FishTier;
import top.arctain.snowTerritory.stvip.service.StvipService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 管理品种收购价与玩家短时出售历史，用于计算衰减。 */
public class FishMarketService {

    private final StfishConfigManager configManager;
    private final StvipService stvipService;
    private final Map<UUID, List<SaleRecord>> playerSales = new ConcurrentHashMap<>();

    public FishMarketService(StfishConfigManager configManager, StvipService stvipService) {
        this.configManager = configManager;
        this.stvipService = stvipService;
    }

    public record SellQuote(double price, boolean priceDecayed) {}

    public SellQuote calculatePrice(String speciesId, FishTier tier, double lengthM, double lengthMax, Player player) {
        UUID playerId = player.getUniqueId();
        double base = configManager.getMarketBasePrice(speciesId);
        double tierMult = configManager.getTierMultiplier(tier);
        double lengthFactor = lengthMax > 0 ? Math.min(1.0, lengthM / lengthMax) : 1.0;
        double price = base * tierMult * (0.5 + 0.5 * lengthFactor);

        int recentCount = countRecentSales(playerId, speciesId);
        boolean decayed = recentCount >= configManager.getDecayThreshold();
        if (decayed) {
            price *= configManager.getDecayFactor();
        }
        if (stvipService != null) {
            price *= stvipService.getFishSellMultiplier(player);
        }
        return new SellQuote(Math.max(0.01, price), decayed);
    }

    public void recordSale(UUID playerId, String speciesId) {
        long now = System.currentTimeMillis();
        long windowMs = configManager.getDecayWindowMinutes() * 60L * 1000;
        playerSales.computeIfAbsent(playerId, k -> new ArrayList<>()).add(new SaleRecord(speciesId, now));
        pruneOldRecords(playerId, now - windowMs);
    }

    private void pruneOldRecords(UUID playerId, long cutoffMs) {
        List<SaleRecord> list = playerSales.get(playerId);
        if (list == null) return;
        list.removeIf(r -> r.timestamp < cutoffMs);
        if (list.isEmpty()) {
            playerSales.remove(playerId);
        }
    }

    private int countRecentSales(UUID playerId, String speciesId) {
        long windowMs = configManager.getDecayWindowMinutes() * 60L * 1000;
        long cutoff = System.currentTimeMillis() - windowMs;
        List<SaleRecord> list = playerSales.get(playerId);
        if (list == null) return 0;
        return (int) list.stream().filter(r -> r.timestamp >= cutoff && speciesId.equals(r.speciesId)).count();
    }

    private record SaleRecord(String speciesId, long timestamp) {}
}
