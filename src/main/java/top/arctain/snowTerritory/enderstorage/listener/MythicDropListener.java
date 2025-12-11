package top.arctain.snowTerritory.enderstorage.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import top.arctain.snowTerritory.enderstorage.config.EnderStorageConfigManager;
import top.arctain.snowTerritory.enderstorage.service.LootStorageService;
import top.arctain.snowTerritory.utils.MessageUtils;

/**
 * 占位监听：后续可替换为 MythicMobs 掉落 API。
 */
public class MythicDropListener implements Listener {

    private final EnderStorageConfigManager configManager;
    private final LootStorageService service;

    public MythicDropListener(EnderStorageConfigManager configManager, LootStorageService service) {
        this.configManager = configManager;
        this.service = service;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        if (!event.getEntity().getKiller().hasPermission("st.loot.auto")) {
            return;
        }
        // 拦截白名单物品
        int slotLimit = service.resolveSlots(event.getEntity().getKiller());
        event.getDrops().removeIf(itemStack -> {
            String key = service.matchItemKey(itemStack);
            if (key == null) return false;
            int perItemMax = service.resolvePerItemMax(event.getEntity().getKiller(), key);
            service.add(event.getEntity().getKiller().getUniqueId(), key, itemStack.getAmount(), perItemMax, slotLimit);
            MessageUtils.sendConfigMessage(event.getEntity().getKiller(), "enderstorage.loot-gained", "&a+" + itemStack.getAmount() + "x " + key + " 已存入战利品仓库", "amount", String.valueOf(itemStack.getAmount()), "item", key);
            return configManager.getMainConfig().getBoolean("features.cancel-entity-drop", true);
        });
    }
}

