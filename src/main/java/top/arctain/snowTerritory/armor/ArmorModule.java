package top.arctain.snowTerritory.armor;

import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.armor.config.ArmorConfigManager;
import top.arctain.snowTerritory.armor.service.ArmorGenerateService;
import top.arctain.snowTerritory.armor.service.ArmorRandomService;
import top.arctain.snowTerritory.armor.command.ArmorCommand;
import top.arctain.snowTerritory.utils.MessageUtils;

public class ArmorModule {

    private final ArmorConfigManager configManager;
    private final ArmorRandomService randomService;
    private final ArmorGenerateService generateService;
    private final ArmorCommand armorCommand;

    public ArmorModule(Main plugin) {
        this.configManager = new ArmorConfigManager(plugin);
        this.randomService = new ArmorRandomService(configManager);
        this.generateService = new ArmorGenerateService(configManager, randomService);
        this.armorCommand = new ArmorCommand(generateService, configManager);
    }

    public void enable() {
        configManager.loadAll();
        MessageUtils.registerModuleMessages("armor", configManager.getMessagesForMerge());

        MessageUtils.logSuccess("Armor 模块已启用");
    }

    public void disable() {
        MessageUtils.unregisterModuleMessages("armor");
    }

    public void reload() {
        configManager.loadAll();
        MessageUtils.registerModuleMessages("armor", configManager.getMessagesForMerge());
    }

    public ArmorCommand getArmorCommand() {
        return armorCommand;
    }

    public ArmorConfigManager getConfigManager() {
        return configManager;
    }
}

