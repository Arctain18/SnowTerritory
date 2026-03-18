package top.arctain.snowTerritory.stfish;

import org.bukkit.plugin.PluginManager;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stfish.command.StfishCommand;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.stfish.gui.FishAtlasGUI;
import top.arctain.snowTerritory.stfish.listener.FishingListener;
import top.arctain.snowTerritory.stfish.listener.FishAtlasListener;
import top.arctain.snowTerritory.stfish.listener.FishSellListener;
import top.arctain.snowTerritory.stfish.listener.WeatherListener;
import top.arctain.snowTerritory.stfish.service.EconomyService;
import top.arctain.snowTerritory.stfish.service.FishItemFactory;
import top.arctain.snowTerritory.stfish.service.FishLootService;
import top.arctain.snowTerritory.stfish.service.FishMarketService;
import top.arctain.snowTerritory.stfish.service.FishSellService;
import top.arctain.snowTerritory.stfish.service.WeatherService;
import top.arctain.snowTerritory.utils.MessageUtils;

/** stfish 模块入口，负责钓鱼掉落重做与天气系统。 */
public class StfishModule {

    private final Main plugin;
    private final StfishConfigManager configManager;
    private final EconomyService economyService;
    private final FishLootService lootService;
    private final FishItemFactory itemFactory;
    private final WeatherService weatherService;
    private final FishAtlasGUI atlasGUI;
    private final StfishCommand stfishCommand;
    private final FishSellListener fishSellListener;

    public StfishModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new StfishConfigManager(plugin);
        this.economyService = new EconomyService();
        this.lootService = new FishLootService(configManager);
        this.itemFactory = new FishItemFactory(plugin, configManager);
        this.weatherService = new WeatherService(configManager, economyService);
        this.atlasGUI = new FishAtlasGUI(configManager, itemFactory);
        FishMarketService marketService = new FishMarketService(configManager);
        FishSellService sellService = new FishSellService(configManager, marketService, economyService, itemFactory);
        this.stfishCommand = new StfishCommand(configManager, weatherService, economyService, atlasGUI, itemFactory);
        this.fishSellListener = new FishSellListener(sellService);
    }

    public void enable() {
        configManager.loadAll();
        MessageUtils.registerModuleMessages("stfish", configManager.getMessagesForMerge());

        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new FishingListener(configManager, lootService, itemFactory), plugin);
        pm.registerEvents(new WeatherListener(configManager), plugin);
        pm.registerEvents(new FishAtlasListener(plugin, atlasGUI), plugin);
        pm.registerEvents(fishSellListener, plugin);

        MessageUtils.logSuccess("ST Fish 模块已启用");
    }

    public void disable() {
        MessageUtils.unregisterModuleMessages("stfish");
    }

    public void reload() {
        configManager.loadAll();
        MessageUtils.registerModuleMessages("stfish", configManager.getMessagesForMerge());
    }

    public StfishCommand getStfishCommand() {
        return stfishCommand;
    }

    public StfishConfigManager getConfigManager() {
        return configManager;
    }
}
