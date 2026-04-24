package top.arctain.snowTerritory.life.listener;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Effect;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.life.config.LifeConfigManager;
import top.arctain.snowTerritory.life.data.LifeSkillType;
import top.arctain.snowTerritory.life.service.LifeService;
import top.arctain.snowTerritory.quest.utils.QuestUtils;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.time.Duration;
import java.util.Map;

public class LifeGatheringListener implements Listener {

    private final LifeConfigManager configManager;
    private final LifeService lifeService;

    public LifeGatheringListener(LifeConfigManager configManager, LifeService lifeService) {
        this.configManager = configManager;
        this.lifeService = lifeService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable) || ageable.getAge() != ageable.getMaximumAge()) {
            return;
        }
        FileConfiguration config = configManager.getConfig();
        if (config == null) {
            return;
        }
        String cropType = block.getType().name();
        ConfigurationSection cropConfig = config.getConfigurationSection("gathering.crops." + cropType);
        if (cropConfig == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        int quality = lifeService.resolveQualityLevel(player, cropType);
        String mmoKey = cropConfig.getString("quality-mmoitems." + quality, "");
        if (mmoKey.isBlank()) {
            MessageUtils.logWarning("life 作物缺少品质映射: " + cropType + " / " + quality);
            return;
        }
        String[] split = mmoKey.split(":", 2);
        if (split.length != 2) {
            MessageUtils.logWarning("life 作物 MMOItems 键格式错误: " + mmoKey);
            return;
        }

        block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getBlockData());
        ageable.setAge(0);
        block.setBlockData(ageable);
        player.incrementStatistic(Statistic.MINE_BLOCK, block.getType());

        ItemStack item = createMMOItem(split[0], split[1]);
        if (item != null) {
            giveItemToPlayer(player, item);
            String itemName = QuestUtils.getMMOItemDisplayName(mmoKey);
            sendCollectTitle(player, itemName == null ? cropType : itemName);
        }

        lifeService.addExperience(player.getUniqueId(), LifeSkillType.GATHERING, 1);
        lifeService.handleGatheringQuestProgress(player, cropType);
    }

    private void sendCollectTitle(Player player, String cropName) {
        FileConfiguration config = configManager.getConfig();
        long fadeIn = config != null ? config.getLong("gathering.title.fade-in", 300) : 300;
        long stay = config != null ? config.getLong("gathering.title.stay", 1000) : 1000;
        long fadeOut = config != null ? config.getLong("gathering.title.fade-out", 300) : 300;
        String title = MessageUtils.parsePlaceholders(player, MessageUtils.getConfigMessage("life.collect-success-title", "&a采集成功"));
        String subtitle = MessageUtils.parsePlaceholders(player, MessageUtils.getConfigMessage("life.collect-success-subtitle", "&7获得: {crop}", "crop", cropName));
        Component titleComp = LegacyComponentSerializer.legacySection().deserialize(ColorUtils.colorize(title));
        Component subtitleComp = LegacyComponentSerializer.legacySection().deserialize(ColorUtils.colorize(subtitle));
        player.showTitle(Title.title(titleComp, subtitleComp,
                Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut))));
    }

    private ItemStack createMMOItem(String mmoType, String mmoId) {
        try {
            var type = MMOItems.plugin.getTypes().get(mmoType);
            if (type == null) {
                return null;
            }
            MMOItem mmoItem = MMOItems.plugin.getMMOItem(type, mmoId);
            if (mmoItem == null) {
                return null;
            }
            return mmoItem.newBuilder().build();
        } catch (Exception e) {
            MessageUtils.logWarning("life 采集 MMOItems 物品不存在: " + mmoType + " " + mmoId);
            return null;
        }
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
        if (remaining != null && !remaining.isEmpty()) {
            for (ItemStack drop : remaining.values()) {
                if (drop != null && !drop.getType().isAir()) {
                    var entity = player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    entity.setPickupDelay(0);
                }
            }
            MessageUtils.sendConfigMessage(player, "life.collect-inventory-full",
                    "&e⚠ &f背包已满！部分物品已掉落在地上。");
        }
    }
}
