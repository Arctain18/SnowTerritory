package top.arctain.snowTerritory.stvip;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stvip.config.StvipConfigManager;
import top.arctain.snowTerritory.stvip.listener.StvipJoinListener;
import top.arctain.snowTerritory.stvip.papi.StvipPlaceholderExpansion;
import top.arctain.snowTerritory.stvip.service.StvipService;
import top.arctain.snowTerritory.utils.MessageUtils;

/** ST VIP：权限分档、强化/卖鱼/战利品权益与 PAPI。 */
public class StvipModule {

    private final Main plugin;
    private final StvipConfigManager configManager;
    private final StvipService service;
    private StvipJoinListener joinListener;
    private StvipPlaceholderExpansion placeholderExpansion;

    public StvipModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new StvipConfigManager(plugin);
        this.service = new StvipService(plugin, configManager);
    }

    public void enable() {
        configManager.loadAll();
        MessageUtils.registerModuleMessages("stvip", configManager.getMessagesForMerge());
        registerJoinListener();
        registerPlaceholderApi();
        MessageUtils.logSuccess("ST VIP 模块已启用");
    }

    public void disable() {
        MessageUtils.unregisterModuleMessages("stvip");
        if (joinListener != null) {
            HandlerList.unregisterAll(joinListener);
            joinListener = null;
        }
        unregisterPlaceholderApi();
    }

    public void reload() {
        configManager.loadAll();
        MessageUtils.registerModuleMessages("stvip", configManager.getMessagesForMerge());
        MessageUtils.logSuccess("ST VIP 配置已重载");
    }

    private void registerJoinListener() {
        if (joinListener != null) {
            HandlerList.unregisterAll(joinListener);
        }
        this.joinListener = new StvipJoinListener(plugin, configManager, service);
        plugin.getServer().getPluginManager().registerEvents(joinListener, plugin);
    }

    private void registerPlaceholderApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        unregisterPlaceholderApi();
        this.placeholderExpansion = new StvipPlaceholderExpansion(plugin, service);
        if (placeholderExpansion.register()) {
            MessageUtils.logSuccess("PlaceholderAPI 扩展 stvip 已注册");
        } else {
            this.placeholderExpansion = null;
            MessageUtils.logWarning("PlaceholderAPI 扩展 stvip 注册失败");
        }
    }

    private void unregisterPlaceholderApi() {
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
            } catch (Throwable ignored) {
            }
            placeholderExpansion = null;
        }
    }

    public StvipConfigManager getConfigManager() {
        return configManager;
    }

    public StvipService getService() {
        return service;
    }
}
