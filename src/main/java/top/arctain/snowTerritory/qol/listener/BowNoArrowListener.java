package top.arctain.snowTerritory.qol.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import top.arctain.snowTerritory.qol.config.QolConfigManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 允许玩家无箭矢使用弓或弩，并清理临时箭矢与弹体。 */
public class BowNoArrowListener implements Listener {

    private static final int PREFERRED_SLOT = 27;

    private final QolConfigManager config;
    private final ItemStack virtualArrow;
    private final Map<UUID, TempArrowState> temporaryArrowByPlayer = new HashMap<>();
    private final Set<Projectile> trackedProjectiles = new HashSet<>();

    public BowNoArrowListener(QolConfigManager config) {
        this.config = config;
        this.virtualArrow = createVirtualArrow();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearArrow(event.getPlayer());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (trackedProjectiles.remove(event.getEntity())) {
            event.getEntity().remove();
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!config.isNoArrowRequired()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (clearArrow(player) && event.getProjectile() instanceof Projectile projectile) {
            trackedProjectiles.add(projectile);
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        if (!config.isNoArrowRequired()) {
            return;
        }
        ItemStack offHand = event.getPlayer().getInventory().getItemInOffHand();
        if (isBowLike(offHand)) {
            return;
        }
        clearArrow(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClick(PlayerInteractEvent event) {
        if (!config.isNoArrowRequired()) {
            return;
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (!event.getAction().toString().contains("RIGHT")) {
            return;
        }
        if (!event.hasItem() || !isBowLike(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        if (hasAnyArrow(inventory)) {
            return;
        }

        int targetSlot = PREFERRED_SLOT;
        ItemStack replaced = null;
        if (inventory.getItem(targetSlot) != null) {
            targetSlot = inventory.firstEmpty();
            if (targetSlot == -1) {
                targetSlot = PREFERRED_SLOT;
                replaced = inventory.getItem(targetSlot);
            }
        }

        inventory.setItem(targetSlot, virtualArrow.clone());
        temporaryArrowByPlayer.put(player.getUniqueId(), new TempArrowState(targetSlot, replaced));
    }

    private boolean hasAnyArrow(PlayerInventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType().name().contains("ARROW")) {
                return true;
            }
        }
        return false;
    }

    private boolean isBowLike(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.BOW || type.name().equals("CROSSBOW");
    }

    private boolean clearArrow(Player player) {
        TempArrowState state = temporaryArrowByPlayer.remove(player.getUniqueId());
        if (state == null) {
            return false;
        }
        player.getInventory().setItem(state.slot(), state.originalItem());
        return true;
    }

    private ItemStack createVirtualArrow() {
        return new ItemStack(Material.ARROW);
    }

    private record TempArrowState(int slot, ItemStack originalItem) {
    }
}
