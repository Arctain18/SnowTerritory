package top.arctain.snowTerritory;

import top.arctain.snowTerritory.commands.SnowTerritoryCommand;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.listeners.GUIListener;
import top.arctain.snowTerritory.listeners.ItemEditListener;
import top.arctain.snowTerritory.listeners.PlayerJoinListener;
import top.arctain.snowTerritory.enderstorage.EnderStorageModule;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.NBTUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private PluginConfig pluginConfig;
    private EnderStorageModule enderStorageModule;

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

        // 注册主命令
        org.bukkit.command.PluginCommand mainCommand = getServer().getPluginCommand("snowterritory");
        if (mainCommand != null) {
            SnowTerritoryCommand commandExecutor = new SnowTerritoryCommand(this, pluginConfig);
            mainCommand.setExecutor(commandExecutor);
            mainCommand.setTabCompleter(commandExecutor);
            MessageUtils.logSuccess("命令 'snowterritory' 已注册");
        } else {
            MessageUtils.logWarning("命令 'snowterritory' 未在 plugin.yml 中注册！");
        }

        // 注册监听器
        getServer().getPluginManager().registerEvents(new GUIListener(pluginConfig, this), this);
        getServer().getPluginManager().registerEvents(new ItemEditListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // 初始化 EnderStorage 模块
        this.enderStorageModule = new EnderStorageModule(this);
        this.enderStorageModule.enable();

        MessageUtils.sendStartupBanner(this);
    }

    @Override
    public void onDisable() {
        if (enderStorageModule != null) {
            enderStorageModule.disable();
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

        // Vault 和 PlayerPoints 是可选的
        boolean hasVault = getServer().getPluginManager().getPlugin("Vault") != null;
        boolean hasPlayerPoints = getServer().getPluginManager().getPlugin("PlayerPoints") != null;

        if (!hasVault) {
            MessageUtils.logWarning("未找到 Vault 插件，金币消耗功能将不可用。");
        }
        if (!hasPlayerPoints) {
            MessageUtils.logWarning("未找到 PlayerPoints 插件，点券消耗功能将不可用。");
        }

        return true;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public EnderStorageModule getEnderStorageModule() {
        return enderStorageModule;
    }
}