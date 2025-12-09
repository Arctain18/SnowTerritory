package top.arctain.snowTerritory;

import top.arctain.snowTerritory.commands.EditCommand;
import top.arctain.snowTerritory.commands.ReloadCommand;
import top.arctain.snowTerritory.commands.ItemIdCommand;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.listeners.GUIListener;
import top.arctain.snowTerritory.listeners.ItemEditListener;
import top.arctain.snowTerritory.utils.NBTUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        // 初始化工具类
        NBTUtils.initialize(this);

        // 检查依赖
        if (!checkDependencies()) {
            getLogger().severe("缺少必要的依赖插件！插件将无法正常工作。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 加载配置
        this.pluginConfig = new PluginConfig(this);
        pluginConfig.loadConfig();

        // 注册命令
        org.bukkit.command.PluginCommand editCommand = getServer().getPluginCommand("edititem");
        if (editCommand != null) {
            editCommand.setExecutor(new EditCommand(this, pluginConfig));
            getLogger().info("命令 'edititem' 已注册");
        } else {
            getLogger().warning("命令 'edititem' 未在 plugin.yml 中注册！");
        }

        org.bukkit.command.PluginCommand reloadCommand = getServer().getPluginCommand("reloadeditor");
        if (reloadCommand != null) {
            reloadCommand.setExecutor(new ReloadCommand(this, pluginConfig));
            getLogger().info("命令 'reloadeditor' 已注册");
        } else {
            getLogger().warning("命令 'reloadeditor' 未在 plugin.yml 中注册！");
        }

        org.bukkit.command.PluginCommand itemIdCommand = getServer().getPluginCommand("itemid");
        if (itemIdCommand != null) {
            itemIdCommand.setExecutor(new ItemIdCommand());
            getLogger().info("命令 'itemid' 已注册");
        } else {
            getLogger().warning("命令 'itemid' 未在 plugin.yml 中注册！");
        }

        // 注册监听器
        getServer().getPluginManager().registerEvents(new GUIListener(pluginConfig), this);
        getServer().getPluginManager().registerEvents(new ItemEditListener(), this);

        getLogger().info("========================================");
        getLogger().info("MMOItems强化编辑插件已启用！");
        getLogger().info("版本: " + getPluginMeta().getVersion());
        getLogger().info("作者: " + String.join(", ", getPluginMeta().getAuthors()));
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("MMOItems强化编辑插件已禁用！");
    }

    /**
     * 检查必要的依赖插件
     */
    private boolean checkDependencies() {
        boolean hasMMOItems = getServer().getPluginManager().getPlugin("MMOItems") != null;
        if (!hasMMOItems) {
            getLogger().severe("未找到 MMOItems 插件！");
            return false;
        }

        // Vault 和 PlayerPoints 是可选的
        boolean hasVault = getServer().getPluginManager().getPlugin("Vault") != null;
        boolean hasPlayerPoints = getServer().getPluginManager().getPlugin("PlayerPoints") != null;

        if (!hasVault) {
            getLogger().warning("未找到 Vault 插件，金币消耗功能将不可用。");
        }
        if (!hasPlayerPoints) {
            getLogger().warning("未找到 PlayerPoints 插件，点券消耗功能将不可用。");
        }

        return true;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
}