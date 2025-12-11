package top.arctain.snowTerritory.reinforce;

import org.bukkit.plugin.PluginManager;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.command.ReinforceCommand;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.reinforce.gui.ReinforceGUI;
import top.arctain.snowTerritory.reinforce.listener.ReinforceGuiListener;
import top.arctain.snowTerritory.utils.MessageUtils;

/**
 * 强化模块入口，负责初始化配置、服务与命令/监听注册。
 */
public class ReinforceModule {

    private final Main plugin;
    private final ReinforceConfigManager configManager;
    private ReinforceGUI reinforceGUI;
    private ReinforceCommand reinforceCommand;

    public ReinforceModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new ReinforceConfigManager(plugin);
    }

    public void enable() {
        configManager.loadAll();
        this.reinforceGUI = new ReinforceGUI(configManager, plugin);
        this.reinforceCommand = new ReinforceCommand(plugin, reinforceGUI);

        registerListeners();

        MessageUtils.logSuccess("Reinforce 模块已启用");
    }

    public void disable() {
        // 可以在这里添加清理逻辑
    }

    public void reload() {
        configManager.reload();
        this.reinforceGUI = new ReinforceGUI(configManager, plugin);
        this.reinforceCommand = new ReinforceCommand(plugin, reinforceGUI);
    }

    private void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();
        if (reinforceGUI != null) {
            pm.registerEvents(new ReinforceGuiListener(configManager, plugin, reinforceGUI), plugin);
        }
    }

    public ReinforceConfigManager getConfigManager() {
        return configManager;
    }

    public ReinforceGUI getReinforceGUI() {
        return reinforceGUI;
    }

    public ReinforceCommand getReinforceCommand() {
        return reinforceCommand;
    }
}

