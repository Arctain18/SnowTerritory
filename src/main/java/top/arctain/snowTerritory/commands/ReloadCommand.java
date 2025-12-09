package top.arctain.snowTerritory.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.utils.MessageUtils;

public class ReloadCommand implements CommandExecutor {

    private final Main plugin;
    private final PluginConfig config;

    public ReloadCommand(Main plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mmoitemseditor.reload") && !(sender instanceof Player && ((Player) sender).isOp())) {
            MessageUtils.sendError(sender, "command.no-permission", "&c✗ &f您没有权限！");
            return true;
        }

        config.reloadConfig();
        MessageUtils.sendSuccess(sender, "command.reload-success", "&a✓ &f插件配置已重载！");
        return true;
    }
}