package top.arctain.snowTerritory;

import top.arctain.snowTerritory.commands.SnowTerritoryCommand;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.listeners.ItemEditListener;
import top.arctain.snowTerritory.listeners.PlayerJoinListener;
import top.arctain.snowTerritory.enderstorage.EnderStorageModule;
import top.arctain.snowTerritory.quest.QuestModule;
import top.arctain.snowTerritory.reinforce.ReinforceModule;
import top.arctain.snowTerritory.stocks.StocksModule;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.NBTUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private PluginConfig pluginConfig;
    private EnderStorageModule enderStorageModule;
    private ReinforceModule reinforceModule;
    private QuestModule questModule;
    private StocksModule stocksModule;

    @Override
    public void onEnable() {
        // 初始化工具类
        NBTUtils.initialize(this);
        MessageUtils.initialize(this);

        // 检查依赖
        if (!checkDependencies()) {
            MessageUtils.logError("缺少必要的依赖插件！插件将无法正常工作。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 加载配置
        this.pluginConfig = new PluginConfig(this);
        pluginConfig.loadConfig();
        
        // 设置 MessageUtils 的配置引用
        MessageUtils.setConfig(pluginConfig);

        // 初始化 Reinforce 模块
        this.reinforceModule = new ReinforceModule(this);
        this.reinforceModule.enable();

        // 初始化 EnderStorage 模块
        this.enderStorageModule = new EnderStorageModule(this);
        this.enderStorageModule.enable();

        // 初始化 Quest 模块
        this.questModule = new QuestModule(this);
        this.questModule.enable();

        // 初始化 Stocks 模块
        this.stocksModule = new StocksModule(this);
        this.stocksModule.enable();

        // 注册主命令
        org.bukkit.command.PluginCommand mainCommand = getServer().getPluginCommand("snowterritory");
        if (mainCommand != null) {
            SnowTerritoryCommand commandExecutor = new SnowTerritoryCommand(this, pluginConfig, reinforceModule);
            mainCommand.setExecutor(commandExecutor);
            mainCommand.setTabCompleter(commandExecutor);
            MessageUtils.logSuccess("命令 'snowterritory' 已注册");
        } else {
            MessageUtils.logWarning("命令 'snowterritory' 未在 plugin.yml 中注册！");
        }

        // 注册监听器
        getServer().getPluginManager().registerEvents(new ItemEditListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        MessageUtils.sendStartupBanner(this);
    }

    @Override
    public void onDisable() {
        if (reinforceModule != null) {
            reinforceModule.disable();
        }
        if (enderStorageModule != null) {
            enderStorageModule.disable();
        }
        if (questModule != null) {
            questModule.disable();
        }
        if (stocksModule != null) {
            stocksModule.disable();
        }
        MessageUtils.sendShutdownBanner(this);
    }

    /**
     * 检查必要的依赖插件
     */
    private boolean checkDependencies() {
        boolean hasMMOItems = getServer().getPluginManager().getPlugin("MMOItems") != null;
        if (!hasMMOItems) {
            MessageUtils.logError("未找到 MMOItems 插件！");
            return false;
        }

        // Vault、PlayerPoints 和 MythicMobs 是可选的
        boolean hasVault = getServer().getPluginManager().getPlugin("Vault") != null;
        boolean hasPlayerPoints = getServer().getPluginManager().getPlugin("PlayerPoints") != null;
        boolean hasMythicMobs = getServer().getPluginManager().getPlugin("MythicMobs") != null;

        if (!hasVault) {
            MessageUtils.logWarning("未找到 Vault 插件，金币消耗功能将不可用。");
        }
        if (!hasPlayerPoints) {
            MessageUtils.logWarning("未找到 PlayerPoints 插件，点券消耗功能将不可用。");
        }
        if (hasMythicMobs) {
            String mythicMobsVersion = getServer().getPluginManager().getPlugin("MythicMobs").getDescription().getVersion();
            MessageUtils.logSuccess("已检测到 MythicMobs " + mythicMobsVersion + "，战利品自动拾取功能已启用。");
        } else {
            MessageUtils.logInfo("未找到 MythicMobs 插件，将仅支持原版生物掉落处理。");
        }

        return true;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public ReinforceModule getReinforceModule() {
        return reinforceModule;
    }

    public EnderStorageModule getEnderStorageModule() {
        return enderStorageModule;
    }

    public QuestModule getQuestModule() {
        return questModule;
    }

    public StocksModule getStocksModule() {
        return stocksModule;
    }
}