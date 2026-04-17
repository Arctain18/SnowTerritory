package top.arctain.snowTerritory.armor.service;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.data.StringListData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import top.arctain.snowTerritory.armor.config.ArmorConfigManager;
import top.arctain.snowTerritory.armor.data.ArmorQuality;
import top.arctain.snowTerritory.armor.data.ArmorBaseDefinition;
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
        return generateFullSet(player, player, setId, null);
    }

    /**
     * @param target  接收物品的玩家（用于生成逻辑，需在线）
     * @param feedback  提示信息发送给谁（控制台代发时发给控制台）
     */
    public List<ItemStack> generateFullSet(Player target, CommandSender feedback, String setId, int[] qualityWeightOverride) {
        CommandSender to = feedback != null ? feedback : target;
        ArmorSetDefinition set = config.getSet(setId);
        if (set == null) {
            MessageUtils.sendConfigMessage(to, "armor.command-unknown-set",
                    "&c✗ &f未找到套装: {set}", "set", setId);
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<String, ArmorSlot> entry : config.getSlots().entrySet()) {
            String slotId = entry.getKey();
            ArmorSlot slot = entry.getValue();
            ItemStack item = generatePiece(to, set, slotId, slot, qualityWeightOverride);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private ItemStack generatePiece(CommandSender feedback, ArmorSetDefinition set, String slotId, ArmorSlot slot,
                                    int[] qualityWeightOverride) {
        ArmorQuality quality = randomService.rollQuality(qualityWeightOverride);
        ArmorStats stats = randomService.rollStatsForSlot(set, slotId, quality);
        String templateId = slot.getTemplateId();
        if (templateId == null || templateId.isEmpty()) {
            MessageUtils.sendConfigMessage(feedback, "armor.command-missing-mmoitems",
                    "&c✗ &f未找到 MMOItems 模板，无法生成防具");
            return null;
        }
        MMOItem mmoItem = getTemplate(config.getMmoitemsType(), templateId);
        if (mmoItem == null) {
            MessageUtils.sendConfigMessage(feedback, "armor.command-missing-mmoitems",
                    "&c✗ &f未找到 MMOItems 模板，无法生成防具");
            return null;
        }
        ItemStack built = mmoItem.newBuilder().build();
        if (built == null || built.getType().isAir()) {
            MessageUtils.sendConfigMessage(feedback, "armor.command-missing-mmoitems",
                    "&c✗ &f生成 MMOItems 物品失败");
            return null;
        }

        String matOverride = set.getSlotMaterial(slotId);
        if (matOverride != null && !matOverride.isBlank()) {
            Material mat = Material.matchMaterial(matOverride);
            if (mat != null && mat.isItem() && built.getType() != mat) {
                built.setType(mat);
            }
        }

        ItemStack withStats = applyStatsToItemStack(built, stats);
        if (withStats != null) {
            built = withStats;
        }

        // MMOItems 的无限耐久：不要用原版 ItemMeta 的 unbreakable，而是写入 MMOItems 的 UNBREAKABLE Stat
        ItemStack unbreakableApplied = applyUnbreakableIfSupported(built, true);
        if (unbreakableApplied != null) {
            built = unbreakableApplied;
        }

        // 尝试把 base.required-level 写入 MMOItems（如果该字段在当前 MMOItems API 中暴露为 ItemStat）
        ArmorBaseDefinition baseDef = set.getBase();
        if (baseDef != null && baseDef.getRequiredLevel() > 0) {
            ItemStack requiredApplied = applyRequiredLevelIfSupported(built, baseDef.getRequiredLevel());
            if (requiredApplied != null) {
                built = requiredApplied;
            }
        }

        // lore 交给 MMOItems 的 lore-format 处理：写入 MMOItems 的 LORE Stat
        if (baseDef != null && baseDef.getLore() != null && !baseDef.getLore().isEmpty()) {
            ItemStack loreApplied = applyLoreIfSupported(built, baseDef.getLore());
            if (loreApplied != null) {
                built = loreApplied;
            }
        }
        var meta = built.getItemMeta();
        if (meta != null) {
            String slotDisplay = MessageUtils.getConfigMessage("armor.slot-display." + slotId, slotId);
            String suffix = quality != null ? quality.getSuffix() : "";
            String name = set.getDisplayName() + slotDisplay + suffix;
            meta.setDisplayName(MessageUtils.colorize(name));

            ArmorBaseDefinition base = set.getBase();
            if (base != null) {
                if (meta instanceof LeatherArmorMeta leatherMeta) {
                    int[] dye = base.getDyeColor();
                    if (dye != null && dye.length == 3) {
                        leatherMeta.setColor(Color.fromRGB(dye[0], dye[1], dye[2]));
                    }
                }
            }

            built.setItemMeta(meta);
        }
        return built;
    }

    private ItemStack applyRequiredLevelIfSupported(ItemStack item, int requiredLevel) {
        try {
            LiveMMOItem live = new LiveMMOItem(item);
            ItemStat stat = MMOItems.plugin.getStats().get("REQUIRED_LEVEL");
            if (stat == null) {
                stat = MMOItems.plugin.getStats().get("REQUIRED_LEVELS");
            }
            if (stat == null) {
                return null;
            }
            live.setData(stat, new DoubleData(requiredLevel));
            return live.newBuilder().build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private ItemStack applyLoreIfSupported(ItemStack item, List<String> loreLines) {
        try {
            LiveMMOItem live = new LiveMMOItem(item);
            ItemStat stat = MMOItems.plugin.getStats().get("LORE");
            if (stat == null) {
                return null;
            }
            live.setData(stat, new StringListData(loreLines));
            return live.newBuilder().build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private ItemStack applyUnbreakableIfSupported(ItemStack item, boolean unbreakable) {
        try {
            LiveMMOItem live = new LiveMMOItem(item);
            ItemStat stat = MMOItems.plugin.getStats().get("UNBREAKABLE");
            if (stat == null) {
                return null;
            }
            live.setData(stat, new BooleanData(unbreakable));
            return live.newBuilder().build();
        } catch (Exception ignored) {
            return null;
        }
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

