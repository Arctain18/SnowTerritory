package top.arctain.snowTerritory.enderstorage.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.enderstorage.config.EnderStorageConfigManager;
import top.arctain.snowTerritory.enderstorage.service.LootStorageService;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.Utils;

/**
 * 监听 MythicMobs 和原版生物的掉落事件，自动将白名单物品存入战利品仓库。
 * 适配 MythicMobs 5.11.0
 * 
 * 注意：由于 MythicMobs 依赖是 optional，如果编译时不可用，
 * 请先运行 mvn clean install 或刷新 IDE 的 Maven 依赖
 */
public class MythicDropListener implements Listener {

    private final EnderStorageConfigManager configManager;
    private final LootStorageService service;
    private final boolean mythicMobsEnabled;

    public MythicDropListener(EnderStorageConfigManager configManager, LootStorageService service) {
        this.configManager = configManager;
        this.service = service;
        this.mythicMobsEnabled = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
    }

    /**
     * 监听 MythicMobs 生物死亡事件（优先处理）
     * 直接使用 MythicMobs API
     */
    @EventHandler(priority = EventPriority.HIGH)
    @SuppressWarnings("unused")
    public void onMythicMobDeath(Object event) {
        if (!mythicMobsEnabled) {
            return;
        }

        // 使用反射方式处理（因为依赖是 optional，编译时可能不可用）
        // 如果 Maven 依赖已正确解析，可以取消下面的注释，直接使用：
        /*
        try {
            io.lumine.mythic.bukkit.events.MythicMobDeathEvent mythicEvent = 
                (io.lumine.mythic.bukkit.events.MythicMobDeathEvent) event;
            
            Player killer = mythicEvent.getKiller();
            if (killer == null || !killer.hasPermission("st.loot.auto")) {
                return;
            }

            int slotLimit = service.resolveSlots(killer);
            mythicEvent.getDrops().removeIf(itemStack -> processItem(killer, itemStack, slotLimit));
            return;
        } catch (ClassCastException e) {
            // 不是 MythicMobs 事件，忽略
            return;
        }
        */
        
        // 使用反射方式（向后兼容）
        handleMythicMobDeathByReflection(event);
    }

    /**
     * 使用反射方式处理 MythicMobs 事件（向后兼容）
     */
    private void handleMythicMobDeathByReflection(Object event) {
        try {
            Class<?> eventClass = Class.forName("io.lumine.mythic.bukkit.events.MythicMobDeathEvent");
            if (!eventClass.isInstance(event)) {
                return;
            }

            Player killer = (Player) eventClass.getMethod("getKiller").invoke(event);
            if (killer == null || !killer.hasPermission("st.loot.auto")) {
                return;
            }

            @SuppressWarnings("unchecked")
            java.util.List<ItemStack> drops = (java.util.List<ItemStack>) eventClass.getMethod("getDrops").invoke(event);

            int slotLimit = service.resolveSlots(killer);
            drops.removeIf(itemStack -> processItem(killer, itemStack, slotLimit));
        } catch (Exception e) {
            // 处理失败，静默忽略
        }
    }

    /**
     * 监听原版生物死亡事件（作为后备）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        // 如果 MythicMobs 已处理，跳过原版事件
        if (mythicMobsEnabled && event.getEntity().hasMetadata("mythicmob")) {
            return;
        }

        if (event.getEntity().getKiller() == null) {
            return;
        }
        if (!event.getEntity().getKiller().hasPermission("st.loot.auto")) {
            return;
        }

        // 拦截白名单物品
        int slotLimit = service.resolveSlots(event.getEntity().getKiller());
        event.getDrops().removeIf(itemStack -> processItem(event.getEntity().getKiller(), itemStack, slotLimit));
    }

    /**
     * 处理单个物品，检查是否为白名单物品并存入仓库
     * @param player 玩家
     * @param itemStack 物品
     * @param slotLimit 槽位限制
     * @return 是否应该移除该物品（取消掉落）
     */
    private boolean processItem(Player player, ItemStack itemStack, int slotLimit) {
        // 必须先检查是否是MMOItems物品
        if (!Utils.isMMOItem(itemStack)) {
            return false;
        }
        // 然后检查是否在whitelist中
        String key = service.matchItemKey(itemStack);
        if (key == null) {
            return false;
        }
        int perItemMax = service.resolvePerItemMax(player, key);
        service.add(player.getUniqueId(), key, itemStack.getAmount(), perItemMax, slotLimit);
        MessageUtils.sendConfigMessage(player, "enderstorage.loot-gained",
                "&a+{amount}x {item} 已存入战利品仓库",
                "amount", String.valueOf(itemStack.getAmount()), "item", key);
        return configManager.getMainConfig().getBoolean("features.cancel-entity-drop", true);
    }
}

