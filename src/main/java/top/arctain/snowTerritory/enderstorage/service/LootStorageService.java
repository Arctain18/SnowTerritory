package top.arctain.snowTerritory.enderstorage.service;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface LootStorageService {
    void initialize();

    void shutdown();

    void reload();

    int getAmount(UUID playerId, String itemKey);

    boolean consume(UUID playerId, String itemKey, int amount);

    void add(UUID playerId, String itemKey, int amount, int perItemMax, int slotLimit);

    Map<String, Integer> getAll(UUID playerId);

    int resolveSlots(Player player);

    int resolvePerItemMax(Player player, String itemKey);
}

