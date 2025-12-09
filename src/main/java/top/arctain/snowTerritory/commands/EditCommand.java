package top.arctain.snowTerritory.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.gui.ItemEditorGUI;
import top.arctain.snowTerritory.utils.MessageUtils;

public class EditCommand implements CommandExecutor {

    private final Main plugin;
    private final PluginConfig config;
    private final ItemEditorGUI gui;

    public EditCommand(Main plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.gui = new ItemEditorGUI(config);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendError(sender, "command.player-only", "&c✗ &f此命令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mmoitemseditor.edit") && !player.isOp()) {
            MessageUtils.sendError(player, "command.no-permission", "&c✗ &f您没有权限！");
            return true;
        }

        gui.openGUI(player);
        return true;
    }
}
