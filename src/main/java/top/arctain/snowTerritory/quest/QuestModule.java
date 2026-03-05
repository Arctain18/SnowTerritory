package top.arctain.snowTerritory.quest;

import org.bukkit.plugin.PluginManager;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.quest.command.QuestCommand;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.QuestDatabaseDao;
import top.arctain.snowTerritory.quest.data.SqliteQuestDatabaseDao;
import top.arctain.snowTerritory.quest.listener.QuestListener;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.quest.service.QuestServiceImpl;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;

/**
 * 任务模块入口，负责初始化配置、服务与命令/监听注册。
 */
public class QuestModule {

    private final Main plugin;
    private final QuestConfigManager configManager;
    private final QuestDatabaseDao databaseDao;
    private final QuestService questService;
    private QuestCommand questCommand;
    private QuestListener questListener;

    public QuestModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new QuestConfigManager(plugin);
        
        // 初始化数据库
        File dbFile = new File(plugin.getDataFolder(), "quest/quest_data.db");
        dbFile.getParentFile().mkdirs();
        this.databaseDao = new SqliteQuestDatabaseDao(plugin, dbFile);
        this.databaseDao.init();
        
        this.questService = new QuestServiceImpl(plugin, configManager, databaseDao);
    }

    public void enable() {
        configManager.loadAll();
        questService.initialize();
        
        this.questCommand = new QuestCommand(plugin, configManager, questService, databaseDao);
        this.questListener = new QuestListener(plugin, questService, configManager);

        registerListeners();

        MessageUtils.logSuccess("Quest 模块已启用，配置目录: plugins/SnowTerritory/quest/");
    }

    public void disable() {
        questService.shutdown();
        if (databaseDao != null) {
            databaseDao.close();
        }
    }

    public void reload() {
        configManager.loadAll();
        questService.reload();
        
        this.questCommand = new QuestCommand(plugin, configManager, questService, databaseDao);
        this.questListener = new QuestListener(plugin, questService, configManager);
    }

    private void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(questListener, plugin);
    }

    public QuestService getQuestService() {
        return questService;
    }

    public QuestCommand getQuestCommand() {
        return questCommand;
    }
}

