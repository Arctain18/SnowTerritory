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

/** 根据鱼种配置创建鱼物品，长度在范围内按正态分布随机。 */
public class FishItemFactory {

    private static final String NAMESPACE = "snowterritory";
    private static final int LENGTH_POINTS = 20;
    private static final double MIN_PROBABILITY = 0.02;
    private static final double LENGTH_SIGMA = 3.0;
    private static final NamespacedKey KEY_ID = new NamespacedKey(NAMESPACE, "stfish_id");
    private static final NamespacedKey KEY_LENGTH = new NamespacedKey(NAMESPACE, "stfish_length");
    private static final NamespacedKey KEY_TIER = new NamespacedKey(NAMESPACE, "stfish_tier");
    private static final NamespacedKey KEY_SPECIES = new NamespacedKey(NAMESPACE, "stfish_species");
    private final Plugin plugin;

    public FishItemFactory(Plugin plugin, top.arctain.snowTerritory.stfish.config.StfishConfigManager configManager) {
        this.plugin = plugin;
    }

    public ItemStack create(FishDefinition def, FishTier tier) {
        double length;
        if (def.lengthMin() >= def.lengthMax()) {
            length = def.lengthMin();
        } else {
            length = sampleLengthNormal(def.lengthMin(), def.lengthMax());
        }
        return create(def, length, tier);
    }

    /** 从 [min, max] 的 20 个等分点中按标准正态分布采样，两端概率 2%，中间最高。 */
    private double sampleLengthNormal(double min, double max) {
        double[] weights = new double[LENGTH_POINTS];
        double wMin = Double.POSITIVE_INFINITY;
        double sum = 0;
        for (int i = 0; i < LENGTH_POINTS; i++) {
            double z = (i - (LENGTH_POINTS - 1) / 2.0) / LENGTH_SIGMA;
            weights[i] = Math.exp(-0.5 * z * z);
            wMin = Math.min(wMin, weights[i]);
            sum += weights[i];
        }
        double scale = (1 - LENGTH_POINTS * MIN_PROBABILITY) / (sum - LENGTH_POINTS * wMin);
        double[] probs = new double[LENGTH_POINTS];
        for (int i = 0; i < LENGTH_POINTS; i++) {
            probs[i] = (weights[i] - wMin) * scale + MIN_PROBABILITY;
        }
        double r = ThreadLocalRandom.current().nextDouble();
        double acc = 0;
        for (int i = 0; i < LENGTH_POINTS; i++) {
            acc += probs[i];
            if (r < acc) {
                double t = (double) i / (LENGTH_POINTS - 1);
                return min + (max - min) * t;
            }
        }
        return max;
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
        if (def.broadcast() != null && !def.broadcast().isEmpty() && tier == FishTier.WORLD) {
            lore.add(ColorUtils.colorize(def.broadcast()));
        }
        String tierColor = getTierColorForLore(tier);
        String qualityLore = MessageUtils.getConfigMessage("stfish.lore-quality", "&8品质: {color}{tier}",
                "color", tierColor, "tier", tierDisplayName);
        String lengthColorHex = getLengthColorHex(length, def.lengthMin(), def.lengthMax());
        String lengthLore = MessageUtils.getConfigMessage("stfish.lore-length", "&8长度: {length}{unit}",
                "length", "&{#" + lengthColorHex + "}" + formatLengthCm(length), "unit", "&7cm");
        lore.add(ColorUtils.colorize(qualityLore));
        lore.add(ColorUtils.colorize(lengthLore));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, def.id());
        meta.getPersistentDataContainer().set(KEY_LENGTH, PersistentDataType.DOUBLE, length);
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.STRING, tierDisplayName);
        String type = def.type() != null ? def.type() : (tier == FishTier.COMMON ? def.id() : null);
        if (type != null) {
            meta.getPersistentDataContainer().set(KEY_SPECIES, PersistentDataType.STRING, type);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String formatLengthCm(double lengthM) {
        return String.format("%.1f", lengthM * 100);
    }

    /** 品质 lore 颜色：普通白、稀有浅绿、史诗粉、传说浅蓝、风暴深蓝、潮汐紫色。 */
    private String getTierColorForLore(FishTier tier) {
        return switch (tier) {
            case COMMON -> "&f";
            case RARE -> "&a";
            case EPIC -> "&d";
            case LEGENDARY -> "&b";
            case STORM -> "&1&l";
            case WORLD -> "&5";
        };
    }

    /** 返回带颜色代码的长度字符串，供 subtitle/广播 使用，与 lore 一致。 */
    public static String formatLengthColored(double lengthM, double lengthMin, double lengthMax) {
        String hex = getLengthColorHex(lengthM, lengthMin, lengthMax);
        return "&{#" + hex + "}" + String.format("%.1f", lengthM * 100);
    }

    /** 根据长度在范围内的位置返回颜色 hex，白->黄->红。 */
    private static String getLengthColorHex(double length, double min, double max) {
        double t = (max > min) ? (length - min) / (max - min) : 0.5;
        t = Math.max(0, Math.min(1, t));
        int r = 255, g, b;
        if (t <= 0.5) {
            double u = t * 2;
            g = 255;
            b = (int) Math.round(255 * (1 - u));
        } else {
            double u = (t - 0.5) * 2;
            g = (int) Math.round(255 * (1 - u));
            b = 0;
        }
        return String.format("%02X%02X%02X", r, Math.min(255, g), Math.min(255, b));
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
    public String getDisplayNameForBroadcast(FishDefinition def, FishTier tier) {
        if (tier.getNameColor() != null) {
            return tier.getNameColor() + ColorUtils.stripColor(def.name());
        }
        return def.name();
    }

    public static String getStFishSpeciesId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_SPECIES, PersistentDataType.STRING);
    }
}
