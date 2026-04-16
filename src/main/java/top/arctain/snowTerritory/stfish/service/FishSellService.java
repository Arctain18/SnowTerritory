package top.arctain.snowTerritory.stfish.service;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.stfish.data.FishDefinition;
import top.arctain.snowTerritory.stfish.data.FishTier;
import top.arctain.snowTerritory.utils.MessageUtils;

/** 处理持鱼右键出售逻辑。 */
public class FishSellService {

    private final StfishConfigManager configManager;
    private final FishMarketService marketService;
    private final EconomyService economyService;
    private final FishItemFactory itemFactory;

    public FishSellService(StfishConfigManager configManager, FishMarketService marketService,
                         EconomyService economyService, FishItemFactory itemFactory) {
        this.configManager = configManager;
        this.marketService = marketService;
        this.economyService = economyService;
        this.itemFactory = itemFactory;
    }

    public SellResult trySell(Player player, ItemStack item) {
        if (!FishItemFactory.isStFish(item)) {
            return SellResult.NOT_FISH;
        }
        if (!economyService.isEnabled()) {
            return SellResult.NO_ECONOMY;
        }

        String fishId = FishItemFactory.getStFishId(item);
        String tierStr = item.getItemMeta().getPersistentDataContainer()
                .get(new org.bukkit.NamespacedKey("snowterritory", "stfish_tier"), org.bukkit.persistence.PersistentDataType.STRING);
        FishTier tier = FishTier.fromDisplayName(tierStr);
        if (tier == null || tier == FishTier.STORM || tier == FishTier.WORLD) {
            return SellResult.STORM_FORBIDDEN;
        }

        FishDefinition def = configManager.getFishById(fishId);
        if (def == null) return SellResult.NOT_FISH;

        String type = FishItemFactory.getStFishSpeciesId(item);
        if (type == null) type = def.type() != null ? def.type() : def.id();

        double length = FishItemFactory.getStFishLength(item);
        double lengthMax = def.lengthMax();

        var quote = marketService.calculatePrice(type, tier, length, lengthMax, player);
        if (!economyService.deposit(player, quote.price())) {
            return SellResult.NO_ECONOMY;
        }

        marketService.recordSale(player.getUniqueId(), type);
        item.setAmount(item.getAmount() - 1);

        String fishDisplayName = itemFactory.getDisplayNameForBroadcast(def, tier);
        MessageUtils.sendConfigMessage(player, "stfish.sell-success", "&a✓ &7出售 {fish} &7成功，获得 &e{price} &7金币",
                "price", String.format("%.2f", quote.price()), "fish", fishDisplayName);
        if (quote.priceDecayed()) {
            MessageUtils.sendConfigMessage(player, "stfish.sell-price-decay",
                    "&e&l! &7该品种出售过多，收购价已下降，请稍后再售");
        }
        return SellResult.SUCCESS;
    }

    public enum SellResult {
        SUCCESS, NOT_FISH, NO_ECONOMY, STORM_FORBIDDEN
    }
}
