package top.arctain.snowTerritory.qol;

import org.bukkit.plugin.PluginManager;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.qol.config.QolConfigManager;
import top.arctain.snowTerritory.qol.listener.FarmlandTrampleListener;
import top.arctain.snowTerritory.utils.MessageUtils;

/** 体验优化模块入口，聚合各类轻量 QoL 行为。 */
public class QolModule {

    private final Main plugin;
    private final QolConfigManager configManager;

    public QolModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new QolConfigManager(plugin);
    }

    public void enable() {
        configManager.loadAll();
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new FarmlandTrampleListener(configManager), plugin);
        MessageUtils.logSuccess("QOL 模块已启用，配置目录: plugins/SnowTerritory/qol/");
    }

    public void disable() {
        // 监听器随插件卸载，无持久资源
    }

    public void reload() {
        configManager.loadAll();
    }

    public QolConfigManager getConfigManager() {
        return configManager;
    }
}
