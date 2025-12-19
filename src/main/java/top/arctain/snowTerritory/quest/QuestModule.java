package top.arctain.snowTerritory.quest;

import org.bukkit.plugin.PluginManager;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.quest.command.QuestCommand;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.listener.QuestListener;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.quest.service.QuestServiceImpl;
import top.arctain.snowTerritory.utils.MessageUtils;

/**
 * 任务模块入口，负责初始化配置、服务与命令/监听注册。
 */
public class QuestModule {

    private final Main plugin;
    private final QuestConfigManager configManager;
    private final QuestService questService;
    private QuestCommand questCommand;
    private QuestListener questListener;

    public QuestModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new QuestConfigManager(plugin);
        this.questService = new QuestServiceImpl(plugin, configManager);
    }

    public void enable() {
        configManager.loadAll();
        questService.initialize();
        
        this.questCommand = new QuestCommand(plugin, configManager, questService);
        this.questListener = new QuestListener(plugin, questService, configManager, null);

        registerListeners();

        MessageUtils.logSuccess("Quest 模块已启用，配置目录: plugins/SnowTerritory/quest/");
    }

    public void disable() {
        questService.shutdown();
    }

    public void reload() {
        configManager.loadAll();
        questService.reload();
        
        this.questCommand = new QuestCommand(plugin, configManager, questService);
        this.questListener = new QuestListener(plugin, questService, configManager, null);
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

