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

    /**
     * 根据物品匹配白名单 key，未匹配返回 null。
     */
    String matchItemKey(org.bukkit.inventory.ItemStack stack);

    /**
     * 取白名单定义。
     */
    top.arctain.snowTerritory.enderstorage.config.WhitelistEntry getWhitelistEntry(String key);

    java.util.Collection<top.arctain.snowTerritory.enderstorage.config.WhitelistEntry> getWhitelistEntries();

    int resolveSlots(Player player);

    int resolvePerItemMax(Player player, String itemKey);

    /**
     * 提供命令描述，便于主命令转发时校验。
     */
    default String usage() {
        return "/sn es [reload|give <player> <itemKey> <amount>]";
    }
}

