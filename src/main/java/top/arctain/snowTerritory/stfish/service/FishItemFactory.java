package top.arctain.snowTerritory.stfish.service;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import top.arctain.snowTerritory.stfish.data.FishDefinition;
import top.arctain.snowTerritory.stfish.data.FishTier;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 根据鱼种配置创建鱼物品，长度在范围内随机。 */
public class FishItemFactory {

    private static final String NAMESPACE = "snowterritory";
    private static final NamespacedKey KEY_ID = new NamespacedKey(NAMESPACE, "stfish_id");
    private static final NamespacedKey KEY_LENGTH = new NamespacedKey(NAMESPACE, "stfish_length");
    private static final NamespacedKey KEY_TIER = new NamespacedKey(NAMESPACE, "stfish_tier");
    private final Plugin plugin;

    public FishItemFactory(Plugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack create(FishDefinition def, FishTier tier) {
        double length;
        if (def.lengthMin() >= def.lengthMax()) {
            length = def.lengthMin();
        } else {
            length = ThreadLocalRandom.current().nextDouble(def.lengthMin(), def.lengthMax());
        }
        return create(def, length, tier);
    }

    public ItemStack create(FishDefinition def, double length, FishTier tier) {
        return create(def, length, tier.getDisplayName(), tier);
    }

    public ItemStack create(FishDefinition def, double length, String tierDisplayName, FishTier tier) {
        ItemStack item = new ItemStack(def.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = tier.getNameColor() != null
                ? tier.getNameColor() + ColorUtils.stripColor(def.name())
                : def.name();
        meta.setDisplayName(ColorUtils.colorize(displayName));

        List<String> lore = new ArrayList<>();
        if (def.description() != null && !def.description().isEmpty()) {
            lore.add(ColorUtils.colorize(def.description()));
        }
        String qualityLore = MessageUtils.getConfigMessage("stfish.lore-quality", "&7品质: {tier}", "tier", tierDisplayName);
        String lengthLore = MessageUtils.getConfigMessage("stfish.lore-length", "&7长度: {length}cm", "length", formatLengthCm(length));
        lore.add(ColorUtils.colorize(qualityLore));
        lore.add(ColorUtils.colorize(lengthLore));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, def.id());
        meta.getPersistentDataContainer().set(KEY_LENGTH, PersistentDataType.DOUBLE, length);
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.STRING, tierDisplayName);

        item.setItemMeta(meta);
        return item;
    }

    private String formatLengthCm(double lengthM) {
        return String.format("%.1f", lengthM * 100);
    }

    public static boolean isStFish(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_ID, PersistentDataType.STRING);
    }

    public static String getStFishId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_ID, PersistentDataType.STRING);
    }

    public static double getStFishLength(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Double d = item.getItemMeta().getPersistentDataContainer().get(KEY_LENGTH, PersistentDataType.DOUBLE);
        return d != null ? d : 0;
    }

    /** 获取用于广播/标题的鱼名称（含颜色，世界鱼保留渐变）。 */
    public static String getDisplayNameForBroadcast(FishDefinition def, FishTier tier) {
        if (tier.getNameColor() != null) {
            return tier.getNameColor() + ColorUtils.stripColor(def.name());
        }
        return def.name();
    }
}
