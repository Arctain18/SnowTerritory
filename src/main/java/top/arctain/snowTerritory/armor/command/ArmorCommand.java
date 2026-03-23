package top.arctain.snowTerritory.armor.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.armor.config.ArmorConfigManager;
import top.arctain.snowTerritory.armor.service.ArmorGenerateService;
import top.arctain.snowTerritory.armor.util.ArmorSetArgParser;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class ArmorCommand implements CommandExecutor, TabCompleter {

    private final ArmorGenerateService generateService;
    private final ArmorConfigManager configManager;

    public ArmorCommand(ArmorGenerateService generateService, ArmorConfigManager configManager) {
        this.generateService = generateService;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendConfigMessage(sender, "armor.command-usage",
                    "&e用法: /sn armor generate all <套装ID>[普通,精品,极品权重]");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (!sub.equals("armor")) {
            return false;
        }
        String action = args[1].toLowerCase();
        if (!action.equals("generate")) {
            MessageUtils.sendConfigMessage(sender, "armor.command-usage",
                    "&e用法: /sn armor generate all <套装ID>[普通,精品,极品权重]");
            return true;
        }
        String mode = args[2].toLowerCase();
        if (!mode.equals("all")) {
            MessageUtils.sendConfigMessage(sender, "armor.command-usage",
                    "&e用法: /sn armor generate all <套装ID>[普通,精品,极品权重]");
            return true;
        }
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "armor.command-player-only",
                    "&c✗ &f此命令仅限玩家使用");
            return true;
        }
        if (!player.hasPermission("st.armor.generate")) {
            MessageUtils.sendConfigMessage(player, "armor.command-no-permission",
                    "&c✗ &f您没有权限使用该命令");
            return true;
        }
        if (args.length < 4) {
            MessageUtils.sendConfigMessage(sender, "armor.command-usage",
                    "&e用法: /sn armor generate all <套装ID>[普通,精品,极品权重]");
            return true;
        }
        ArmorSetArgParser.Parsed parsed = ArmorSetArgParser.parse(args[3]);
        String setId = parsed.setId();
        if (parsed.qualityWeights() != null && parsed.qualityWeights().length == 0) {
            MessageUtils.sendConfigMessage(player, "armor.command-invalid-weights",
                    "&c✗ &f品质权重格式错误，需为三个非负整数: &e套装ID[w1,w2,w3] &7(对应 common/rare/epic)");
            return true;
        }
        int[] weightOverride = parsed.hasQualityWeights() ? parsed.qualityWeights() : null;
        var items = generateService.generateFullSet(player, setId, weightOverride);
        if (items.isEmpty()) {
            return true;
        }
        for (ItemStack item : items) {
            var overflow = player.getInventory().addItem(item);
            overflow.values().forEach(stack -> {
                if (stack != null && !stack.getType().isAir()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                }
            });
        }
        var setDef = configManager.getSet(setId);
        String display = setDef != null && setDef.getDisplayName() != null && !setDef.getDisplayName().isBlank()
                ? setDef.getDisplayName()
                : setId;
        MessageUtils.sendConfigMessage(player, "armor.generate-set-success",
                "&a✓ &f已为 &e{player} &f生成 &e{set-display} &f套装",
                "player", player.getName(), "set-display", display);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            if ("armor".startsWith(args[0].toLowerCase())) {
                result.add("armor");
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("armor")) {
            if ("generate".startsWith(args[1].toLowerCase())) {
                result.add("generate");
            }
            return result;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("armor") && args[1].equalsIgnoreCase("generate")) {
            if ("all".startsWith(args[2].toLowerCase())) {
                result.add("all");
            }
            return result;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("armor") && args[1].equalsIgnoreCase("generate") && args[2].equalsIgnoreCase("all")) {
            String prefix = ArmorSetArgParser.tabCompletePrefix(args[3]).toLowerCase();
            configManager.getAllSets().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(prefix))
                    .sorted()
                    .forEach(result::add);
            return result;
        }
        return result;
    }
}

