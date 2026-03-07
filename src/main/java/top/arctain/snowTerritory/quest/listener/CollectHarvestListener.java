package top.arctain.snowTerritory.quest.listener;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestReleaseMethod;
import top.arctain.snowTerritory.quest.data.QuestStatus;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 采集任务监听器
 * 监听玩家右键采集完全成熟农作物，发放 MMOItems 并更新任务进度
 */
public class CollectHarvestListener implements Listener {

    private static final String CROP_KEY_PREFIX = "CROP:";

    private final QuestService questService;
    private final QuestConfigManager configManager;

    public CollectHarvestListener(QuestService questService, QuestConfigManager configManager) {
        this.questService = questService;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() != ageable.getMaximumAge()) {
            return;
        }

        String cropType = block.getType().name();
        ConfigurationSection cropConfig = getCropConfig(cropType);
        if (cropConfig == null) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        String displayName = cropConfig.getString("display-name", cropType);
        String mmoType = cropConfig.getString("mmo-type");
        String mmoId = cropConfig.getString("mmo-id");
        if (mmoType == null || mmoId == null) {
            return;
        }

        ageable.setAge(0);
        block.setBlockData(ageable);

        sendCollectTitle(player, displayName);
        player.incrementStatistic(Statistic.MINE_BLOCK, block.getType());

        ItemStack item = createMMOItem(mmoType, mmoId);
        if (item != null) {
            giveItemToPlayer(player, item);
        }

        String cropKey = CROP_KEY_PREFIX + cropType;
        boolean updated = questService.updateQuestProgress(player.getUniqueId(), cropKey, 1);
        if (updated) {
            handleQuestCompletion(player, cropKey);
        }
    }

    private ConfigurationSection getCropConfig(String cropType) {
        FileConfiguration whitelist = configManager.getCropsWhitelist();
        if (whitelist == null) {
            return null;
        }
        ConfigurationSection cropsSection = whitelist.getConfigurationSection("crops");
        if (cropsSection == null) {
            return null;
        }
        return cropsSection.getConfigurationSection(cropType);
    }

    private void sendCollectTitle(Player player, String cropName) {
        String title = MessageUtils.getConfigMessage("quest.collect-success-title", "&a采集成功");
        String subtitle = MessageUtils.getConfigMessage("quest.collect-success-subtitle", "&7+1 {crop}", "crop", cropName);
        Component titleComp = LegacyComponentSerializer.legacySection().deserialize(ColorUtils.colorize(title));
        Component subtitleComp = LegacyComponentSerializer.legacySection().deserialize(ColorUtils.colorize(subtitle));
        player.showTitle(Title.title(titleComp, subtitleComp,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))));
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
            MessageUtils.logWarning("采集任务 MMOItems 物品不存在: " + mmoType + " " + mmoId);
            return null;
        }
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
        if (remaining != null && !remaining.isEmpty()) {
            for (ItemStack drop : remaining.values()) {
                if (drop != null && !drop.getType().isAir()) {
                    var entity = player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    entity.setPickupDelay(0);
                }
            }
            MessageUtils.sendConfigMessage(player, "quest.collect-inventory-full",
                    "&e⚠ &f背包已满！部分物品已掉落在地上。");
        }
    }

    private void handleQuestCompletion(Player player, String cropKey) {
        for (Quest quest : questService.getActiveBountyQuests()) {
            if (quest.getType() == QuestType.COLLECT && cropKey.equals(quest.getMaterialKey()) && quest.isCompleted()) {
                quest.setStatus(QuestStatus.COMPLETED);
                MessageUtils.sendConfigMessage(player, "quest.bounty-completed",
                        "&a✓ &f悬赏任务完成！使用 &e/sn q complete &f领取奖励");
                return;
            }
        }
    }
}
