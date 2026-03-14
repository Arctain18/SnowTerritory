package top.arctain.snowTerritory.stfish.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
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
import top.arctain.snowTerritory.stfish.util.BiomeNameHelper;
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

        ItemStack fishItem = itemFactory.create(def, tier);
        caughtItem.setItemStack(fishItem);

        double length = FishItemFactory.getStFishLength(fishItem);
        String lengthColored = FishItemFactory.formatLengthColored(length, def.lengthMin(), def.lengthMax());
        String fishDisplayName = itemFactory.getDisplayNameForBroadcast(def, tier);

        sendFishTitle(player, fishDisplayName, lengthColored);
        if (tier == FishTier.STORM || tier == FishTier.WORLD) {
            Biome biome = event.getHook().getLocation().getBlock().getBiome();
            String poem = tier == FishTier.WORLD && def.broadcast() != null ? def.broadcast() : null;
            broadcastFish(player, fishDisplayName, tier, length, def.lengthMin(), def.lengthMax(), biome, poem);
        }
    }

    private void sendFishTitle(Player player, String fishDisplayName, String lengthStr) {
        long fadeIn = configManager.getTitleFadeIn();
        long stay = configManager.getTitleStay();
        long fadeOut = configManager.getTitleFadeOut();

        String title = MessageUtils.parsePlaceholders(player, MessageUtils.getConfigMessage("stfish.fish-title", "&a鱼上钩了！"));
        String subtitle = MessageUtils.parsePlaceholders(player, MessageUtils.getConfigMessage("stfish.fish-subtitle", "&7{fish} &8| {length}&7cm",
                "fish", fishDisplayName, "length", lengthStr));

        Component titleComp = LegacyComponentSerializer.legacySection().deserialize(ColorUtils.colorize(title));
        Component subtitleComp = LegacyComponentSerializer.legacySection().deserialize(ColorUtils.colorize(subtitle));
        player.showTitle(Title.title(titleComp, subtitleComp,
                Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut))));
    }

    private void broadcastFish(Player player, String fishDisplayName, FishTier tier, double lengthM,
            double lengthMin, double lengthMax, Biome biome, String poem) {
        String location = BiomeNameHelper.getDisplayName(biome);
        String lengthCm = FishItemFactory.formatLengthColored(lengthM, lengthMin, lengthMax);
        String tierModifier = tier == FishTier.STORM ? "&{#42a5f5}&l风暴般的 " : "&{#8c00ff}&l世界般的 ";

        String key = tier == FishTier.STORM ? "stfish.broadcast-storm" : "stfish.broadcast-world";
        String poemPlaceholder = poem != null ? poem : "";
        String msg = MessageUtils.getConfigMessage(key,
                "{#4d99eb}◈ {#cccccc}玩家 {#FFFF00}{player} {#cccccc}于 {#3c79cf>}&l{location}{#3341ff<} {#cccccc}钓上了 {length_cm}&7cm {tier_modifier}{fish_name} {#cccccc}!\n{poem}",
                "player", player.getName(), "location", location, "length_cm", lengthCm, "tier_modifier", tierModifier, "fish_name", fishDisplayName, "poem", poemPlaceholder);

        for (Player p : Bukkit.getOnlinePlayers()) {
            String parsed = MessageUtils.parsePlaceholders(p, msg);
            p.sendMessage(ColorUtils.colorize(parsed));
        }
    }
}
