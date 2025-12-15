package top.arctain.snowTerritory.enderstorage;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.enderstorage.command.EnderStorageCommand;
import top.arctain.snowTerritory.enderstorage.config.EnderStorageConfigManager;
import top.arctain.snowTerritory.enderstorage.listener.MythicDropListener;
import top.arctain.snowTerritory.enderstorage.listener.LootGuiListener;
import top.arctain.snowTerritory.enderstorage.gui.LootStorageGUI;
import top.arctain.snowTerritory.enderstorage.service.LootStorageService;
import top.arctain.snowTerritory.enderstorage.service.LootStorageServiceImpl;
import top.arctain.snowTerritory.utils.MessageUtils;

/**
 * 末影存储模块入口，负责初始化配置、服务与命令/监听注册。
 */
public class EnderStorageModule {

    private final Main plugin;
    private final EnderStorageConfigManager configManager;
    private final LootStorageService lootStorageService;
    private LootStorageGUI lootStorageGUI;
    private EnderStorageCommand enderCommand;
    private MythicDropListener mythicDropListener;

    public EnderStorageModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new EnderStorageConfigManager(plugin);
        this.lootStorageService = new LootStorageServiceImpl(plugin, configManager);
    }

    public void enable() {
        configManager.loadAll();
        lootStorageService.initialize();
        this.lootStorageGUI = new LootStorageGUI(plugin, configManager, lootStorageService);
        this.enderCommand = new EnderStorageCommand(plugin, configManager, lootStorageService, lootStorageGUI);

        registerCommand();
        registerListeners();

        MessageUtils.logSuccess("EnderStorage 模块已启用，配置目录: plugins/SnowTerritory/ender-storage/");
    }

    public void disable() {
        lootStorageService.shutdown();
    }

    public void reload() {
        configManager.loadAll();
        lootStorageService.reload();
        this.lootStorageGUI = new LootStorageGUI(plugin, configManager, lootStorageService);
        this.enderCommand = new EnderStorageCommand(plugin, configManager, lootStorageService, lootStorageGUI);
    }

    private void registerCommand() {
        PluginCommand command = plugin.getCommand("snowterritoryenderstorage");
        if (command != null) {
            command.setExecutor(enderCommand);
            command.setTabCompleter(enderCommand);
        } else {
            MessageUtils.logWarning("命令 'snowterritoryenderstorage' 未在 plugin.yml 中注册！");
        }
    }

    private void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();
        this.mythicDropListener = new MythicDropListener(configManager, lootStorageService);
        pm.registerEvents(mythicDropListener, plugin);
        
        if (lootStorageGUI != null) {
            pm.registerEvents(new LootGuiListener(plugin, lootStorageGUI, lootStorageService), plugin);
        }
    }

    public LootStorageService getLootStorageService() {
        return lootStorageService;
    }

    public EnderStorageCommand getEnderCommand() {
        return enderCommand;
    }
}

