package top.arctain.snowTerritory.armor.service;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.armor.config.ArmorConfigManager;
import top.arctain.snowTerritory.armor.data.ArmorQuality;
import top.arctain.snowTerritory.armor.data.ArmorSetDefinition;
import top.arctain.snowTerritory.armor.data.ArmorSlot;
import top.arctain.snowTerritory.armor.data.ArmorStats;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArmorGenerateService {

    private final ArmorConfigManager config;
    private final ArmorRandomService randomService;

    public ArmorGenerateService(ArmorConfigManager config, ArmorRandomService randomService) {
        this.config = config;
        this.randomService = randomService;
    }

    public List<ItemStack> generateFullSet(Player player, String setId) {
        ArmorSetDefinition set = config.getSet(setId);
        if (set == null) {
            MessageUtils.sendConfigMessage(player, "armor.command-unknown-set",
                    "&c✗ &f未找到套装: {set}", "set", setId);
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<String, ArmorSlot> entry : config.getSlots().entrySet()) {
            String slotId = entry.getKey();
            ArmorSlot slot = entry.getValue();
            ItemStack item = generatePiece(player, set, slotId, slot);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private ItemStack generatePiece(Player player, ArmorSetDefinition set, String slotId, ArmorSlot slot) {
        ArmorQuality quality = randomService.rollQuality();
        ArmorStats stats = randomService.rollStatsForSlot(set, slotId, quality);
        String templateId = slot.getTemplateId();
        if (templateId == null || templateId.isEmpty()) {
            MessageUtils.sendConfigMessage(player, "armor.command-missing-mmoitems",
                    "&c✗ &f未找到 MMOItems 模板，无法生成防具");
            return null;
        }
        MMOItem mmoItem = getTemplate(config.getMmoitemsType(), templateId);
        if (mmoItem == null) {
            MessageUtils.sendConfigMessage(player, "armor.command-missing-mmoitems",
                    "&c✗ &f未找到 MMOItems 模板，无法生成防具");
            return null;
        }
        ItemStack built = mmoItem.newBuilder().build();
        if (built == null || built.getType().isAir()) {
            MessageUtils.sendConfigMessage(player, "armor.command-missing-mmoitems",
                    "&c✗ &f生成 MMOItems 物品失败");
            return null;
        }

        ItemStack withStats = applyStatsToItemStack(built, stats);
        if (withStats != null) {
            built = withStats;
        }
        if (quality != null) {
            var meta = built.getItemMeta();
            if (meta != null) {
                String display = meta.hasDisplayName() ? meta.getDisplayName() : built.getType().name();
                String name = display + quality.getSuffix();
                meta.setDisplayName(MessageUtils.colorize(name));
                built.setItemMeta(meta);
            }
        }
        return built;
    }

    private MMOItem getTemplate(String typeId, String templateId) {
        try {
            net.Indyuce.mmoitems.api.Type type = MMOItems.plugin.getTypes().get(typeId);
            if (type == null) {
                return null;
            }
            return MMOItems.plugin.getMMOItem(type, templateId);
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack applyStatsToItemStack(ItemStack item, ArmorStats stats) {
        try {
            LiveMMOItem live = new LiveMMOItem(item);
            for (Map.Entry<String, Double> entry : stats.asMap().entrySet()) {
                String internalKey = entry.getKey();
                String statId = config.getStatMapping().get(internalKey);
                if (statId == null || statId.isEmpty()) {
                    continue;
                }
                @SuppressWarnings("rawtypes")
                ItemStat itemStat = MMOItems.plugin.getStats().get(statId);
                if (itemStat == null) {
                    continue;
                }
                double value = entry.getValue();
                live.setData(itemStat, new DoubleData(value));
            }
            return live.newBuilder().build();
        } catch (Exception e) {
            return null;
        }
    }
}

