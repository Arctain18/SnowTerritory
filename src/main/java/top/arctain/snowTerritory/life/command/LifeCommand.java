package top.arctain.snowTerritory.life.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.life.data.LifeSkillProgress;
import top.arctain.snowTerritory.life.service.LifeService;
import top.arctain.snowTerritory.utils.DisplayUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class LifeCommand implements CommandExecutor, TabCompleter {

    private final LifeService lifeService;

    public LifeCommand(LifeService lifeService) {
        this.lifeService = lifeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "life.player-only", "&c✗ &f此命令仅限玩家使用");
            return true;
        }
        return handleList(player);
    }

    private boolean handleList(Player player) {
        List<LifeSkillProgress> all = lifeService.getAllProgress(player.getUniqueId());
        MessageUtils.sendConfigRaw(player, "life.list-header", "&7========= &f生活经验 &7=========");
        for (LifeSkillProgress progress : all) {
            String bar = progress.neededExp() <= 0
                    ? DisplayUtils.progressBar(DisplayUtils.BarStyle.BLOCKS, "&a", "&a", "&a", 1, 1, 18, "&7")
                    : DisplayUtils.progressBar(DisplayUtils.BarStyle.BLOCKS, "&c", "&e", "&a",
                    progress.currentExp(), progress.neededExp(), 18, "&7");
            MessageUtils.sendConfigRaw(player, "life.list-item",
                    "&7{skill} &8| &fLv.{level} &8| {bar} &8({current}/{need})",
                    "skill", progress.skillType().getDisplayName(),
                    "level", String.valueOf(progress.level()),
                    "bar", bar,
                    "current", String.valueOf(progress.currentExp()),
                    "need", progress.neededExp() <= 0 ? "MAX" : String.valueOf(progress.neededExp()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            if ("list".startsWith(args[0].toLowerCase())) {
                result.add("list");
            }
            return result;
        }
        return List.of();
    }
}
