package top.arctain.snowTerritory.qol.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import top.arctain.snowTerritory.qol.config.QolConfigManager;

/** 阻止任意实体踩踏耕地使其变为泥土。 */
public class FarmlandTrampleListener implements Listener {

    private final QolConfigManager config;

    public FarmlandTrampleListener(QolConfigManager config) {
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onFarmlandTrample(EntityChangeBlockEvent event) {
        if (!config.isPreventFarmlandTrample()) {
            return;
        }
        if (event.getBlock().getType() != Material.FARMLAND) {
            return;
        }
        if (event.getBlockData().getMaterial() != Material.DIRT) {
            return;
        }
        event.setCancelled(true);
    }
}
