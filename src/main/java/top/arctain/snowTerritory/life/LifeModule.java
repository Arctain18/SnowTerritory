package top.arctain.snowTerritory.life;

import org.bukkit.event.HandlerList;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.life.command.LifeCommand;
import top.arctain.snowTerritory.life.config.LifeConfigManager;
import top.arctain.snowTerritory.life.data.LifeDatabaseDao;
import top.arctain.snowTerritory.life.data.SqliteLifeDatabaseDao;
import top.arctain.snowTerritory.life.listener.LifeGatheringListener;
import top.arctain.snowTerritory.life.service.LifeService;
import top.arctain.snowTerritory.life.service.LifeServiceImpl;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;

public class LifeModule {

    private final Main plugin;
    private final LifeConfigManager configManager;
    private final LifeDatabaseDao databaseDao;
    private final LifeService lifeService;
    private LifeCommand lifeCommand;
    private LifeGatheringListener gatheringListener;

    public LifeModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new LifeConfigManager(plugin);
        File dbFile = new File(plugin.getDataFolder(), "life/life_data.db");
        dbFile.getParentFile().mkdirs();
        this.databaseDao = new SqliteLifeDatabaseDao(plugin, dbFile);
        this.databaseDao.init();
        this.lifeService = new LifeServiceImpl(configManager, databaseDao,
                plugin.getQuestModule() != null ? plugin.getQuestModule().getQuestService() : null);
    }

    public void enable() {
        configManager.loadAll();
        MessageUtils.registerModuleMessages("life", configManager.getMessagesForMerge());
        this.lifeCommand = new LifeCommand(lifeService);
        this.gatheringListener = new LifeGatheringListener(configManager, lifeService);
        plugin.getServer().getPluginManager().registerEvents(gatheringListener, plugin);
        MessageUtils.logSuccess("Life 模块已启用");
    }

    public void disable() {
        MessageUtils.unregisterModuleMessages("life");
        if (gatheringListener != null) {
            HandlerList.unregisterAll(gatheringListener);
            gatheringListener = null;
        }
        databaseDao.close();
    }

    public void reload() {
        if (gatheringListener != null) {
            HandlerList.unregisterAll(gatheringListener);
        }
        configManager.loadAll();
        MessageUtils.registerModuleMessages("life", configManager.getMessagesForMerge());
        this.gatheringListener = new LifeGatheringListener(configManager, lifeService);
        plugin.getServer().getPluginManager().registerEvents(gatheringListener, plugin);
    }

    public LifeCommand getLifeCommand() {
        return lifeCommand;
    }

    public LifeService getLifeService() {
        return lifeService;
    }

    public LifeConfigManager getConfigManager() {
        return configManager;
    }
}
