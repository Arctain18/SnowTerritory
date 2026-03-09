package top.arctain.snowTerritory.stfish.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.stfish.data.FishDefinition;
import top.arctain.snowTerritory.stfish.data.FishTier;
import top.arctain.snowTerritory.stfish.service.FishItemFactory;
import top.arctain.snowTerritory.stfish.service.FishLootService;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.time.Duration;
import java.util.Map;

/** 监听钓鱼事件，替换掉落为 ST 鱼类，发送 Title 与广播。 */
public class FishingListener implements Listener {

    private final StfishConfigManager configManager;
    private final FishLootService lootService;
    private final FishItemFactory itemFactory;

    public FishingListener(StfishConfigManager configManager, FishLootService lootService, FishItemFactory itemFactory) {
        this.configManager = configManager;
        this.lootService = lootService;
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item caughtItem)) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        FishTier tier = lootService.rollTier(world);
        FishDefinition def = lootService.rollFish(tier);
        if (def == null) return;

        ItemStack fishItem = itemFactory.create(def, tier.getDisplayName());
        caughtItem.setItemStack(fishItem);

        double length = FishItemFactory.getStFishLength(fishItem);
        String lengthStr = String.format("%.2f", length);

        sendFishTitle(player, def.name(), tier.getDisplayName(), lengthStr);
        if (tier == FishTier.STORM || tier == FishTier.WORLD) {
            broadcastFish(player, def.name(), tier.getDisplayName(), lengthStr);
        }
    }

    private void sendFishTitle(Player player, String fishName, String tierName, String lengthStr) {
        long fadeIn = configManager.getTitleFadeIn();
        long stay = configManager.getTitleStay();
        long fadeOut = configManager.getTitleFadeOut();

        String title = MessageUtils.getConfigMessage("stfish.fish-title", "&a钓到了！");
        String subtitle = MessageUtils.getConfigMessage("stfish.fish-subtitle", "&7{fish} &8| &7{length}m",
                "fish", fishName, "tier", tierName, "length", lengthStr);

        Component titleComp = LegacyComponentSerializer.legacySection().deserialize(ColorUtils.colorize(title));
        Component subtitleComp = LegacyComponentSerializer.legacySection().deserialize(ColorUtils.colorize(subtitle));
        player.showTitle(Title.title(titleComp, subtitleComp,
                Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut))));
    }

    private void broadcastFish(Player player, String fishName, String tierName, String lengthStr) {
        String key = tierName.equals("风暴") ? "stfish.broadcast-storm" : "stfish.broadcast-world";
        String msg = MessageUtils.getConfigMessage(key, "&6✦ &e[{tier}] &f{player} &7钓到了 &e{fish} &8| &7长度: {length}m",
                "player", player.getName(), "fish", fishName, "tier", tierName, "length", lengthStr);
        String colored = ColorUtils.colorize(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(colored);
        }
    }
}
