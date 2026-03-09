package top.arctain.snowTerritory.stfish.service;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import top.arctain.snowTerritory.stfish.data.FishDefinition;
import top.arctain.snowTerritory.utils.ColorUtils;

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

    public ItemStack create(FishDefinition def, String tierDisplayName) {
        double length;
        if (def.lengthMin() >= def.lengthMax()) {
            length = def.lengthMin();
        } else {
            length = ThreadLocalRandom.current().nextDouble(def.lengthMin(), def.lengthMax());
        }
        return create(def, length, tierDisplayName);
    }

    public ItemStack create(FishDefinition def, double length, String tierDisplayName) {
        ItemStack item = new ItemStack(def.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ColorUtils.colorize("&f" + def.name()));
        List<String> lore = new ArrayList<>();
        if (def.description() != null && !def.description().isEmpty()) {
            lore.add(ColorUtils.colorize("&7" + def.description()));
        }
        lore.add(ColorUtils.colorize("&8品质: &7" + tierDisplayName));
        lore.add(ColorUtils.colorize("&8长度: &7" + formatLength(length) + "m"));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, def.id());
        meta.getPersistentDataContainer().set(KEY_LENGTH, PersistentDataType.DOUBLE, length);
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.STRING, tierDisplayName);

        item.setItemMeta(meta);
        return item;
    }

    private String formatLength(double length) {
        return String.format("%.2f", length);
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
}
