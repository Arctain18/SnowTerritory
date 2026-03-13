package top.arctain.snowTerritory.stfish.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.stfish.service.FishItemFactory;
import top.arctain.snowTerritory.stfish.service.FishSellService;
import top.arctain.snowTerritory.utils.MessageUtils;

/** 监听持鱼右键出售。 */
public class FishSellListener implements Listener {

    private final FishSellService sellService;

    public FishSellListener(FishSellService sellService) {
        this.sellService = sellService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!FishItemFactory.isStFish(mainHand)) {
            return;
        }

        var result = sellService.trySell(player, mainHand);
        if (result == FishSellService.SellResult.SUCCESS) {
            event.setCancelled(true);
        } else if (result == FishSellService.SellResult.NO_ECONOMY) {
            MessageUtils.sendConfigMessage(player, "stfish.sell-no-economy", "&c✗ &f经济系统未启用，无法出售");
            event.setCancelled(true);
        }
    }
}
