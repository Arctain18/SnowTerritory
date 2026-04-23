package top.arctain.snowTerritory.stfish.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.stfish.config.StfishConfigManager;
import top.arctain.snowTerritory.stfish.data.FishDefinition;
import top.arctain.snowTerritory.stfish.data.FishTier;
import top.arctain.snowTerritory.stfish.gui.FishAtlasGUI;
import top.arctain.snowTerritory.stfish.service.EconomyService;
import top.arctain.snowTerritory.stfish.service.FishItemFactory;
import top.arctain.snowTerritory.stfish.service.WeatherService;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.NumberFormatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 处理 /sn fish 与 /sn weather 命令。 */
public class StfishCommand implements CommandExecutor, TabCompleter {

    private final StfishConfigManager configManager;
    private final WeatherService weatherService;
    private final EconomyService economyService;
    private final FishAtlasGUI atlasGUI;
    private final FishItemFactory itemFactory;

    public StfishCommand(StfishConfigManager configManager, WeatherService weatherService, EconomyService economyService,
                         FishAtlasGUI atlasGUI, FishItemFactory itemFactory) {
        this.configManager = configManager;
        this.weatherService = weatherService;
        this.economyService = economyService;
        this.atlasGUI = atlasGUI;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String sub = args[0].toLowerCase();
        if (sub.equals("weather")) {
            return handleWeather(sender, args);
        }
        if (sub.equals("fish")) {
            return handleFish(sender, args);
        }
        return false;
    }

    private boolean handleFish(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return handleFishAtlas(sender);
        }
        String action = args[1].toLowerCase();
        if (action.equals("atlas") || action.equals("图鉴")) {
            return handleFishAtlas(sender);
        }
        if (action.equals("give")) {
            return handleFishGive(sender, args);
        }
        return handleFishAtlas(sender);
    }

    private boolean handleFishAtlas(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "stfish.atlas-player-only", "&c✗ &f此命令仅限玩家使用");
            return true;
        }
        if (!sender.hasPermission("st.fish.use")) {
            MessageUtils.sendConfigMessage(sender, "stfish.atlas-no-permission", "&c✗ &f您没有权限打开图鉴");
            return true;
        }
        atlasGUI.open(player, 1);
        return true;
    }

    private boolean handleFishGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("st.fish.give")) {
            MessageUtils.sendConfigMessage(sender, "stfish.give-no-permission", "&c✗ &f您没有权限给予鱼类");
            return true;
        }
        if (args.length < 4) {
            MessageUtils.sendConfigMessage(sender, "stfish.give-usage", "&e用法: /sn fish give <玩家> <鱼种ID> [数量]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null || !target.isOnline()) {
            MessageUtils.sendConfigMessage(sender, "stfish.give-player-not-found", "&c✗ &f未找到玩家: {player}", "player", args[2]);
            return true;
        }
        FishDefinition def = configManager.getFishById(args[3]);
        if (def == null) {
            MessageUtils.sendConfigMessage(sender, "stfish.give-fish-not-found", "&c✗ &f未找到鱼种: {id}", "id", args[3]);
            return true;
        }
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[4])));
            } catch (NumberFormatException ignored) {
            }
        }
        FishTier tier = getTierForFish(def);
        if (tier == null) {
            MessageUtils.sendConfigMessage(sender, "stfish.give-tier-unknown", "&c✗ &f无法确定鱼种品质");
            return true;
        }
        double midLength = (def.lengthMin() + def.lengthMax()) / 2;
        for (int i = 0; i < amount; i++) {
            ItemStack fish = itemFactory.create(def, midLength, tier);
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(fish);
            for (ItemStack drop : overflow.values()) {
                if (drop != null && !drop.getType().isAir()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), drop);
                }
            }
        }
        String fishDisplayName = itemFactory.getDisplayNameForBroadcast(def, tier);
        MessageUtils.sendConfigMessage(sender, "stfish.give-success", "&a✓ &f已给予 &e{player} &f{amount}x &e{fish}",
                "player", target.getName(), "amount", String.valueOf(amount), "fish", fishDisplayName);
        return true;
    }

    private FishTier getTierForFish(FishDefinition def) {
        for (var e : configManager.getFishByTier().entrySet()) {
            if (e.getValue().stream().anyMatch(f -> f.id().equals(def.id()))) {
                return e.getKey();
            }
        }
        return null;
    }

    private boolean handleWeather(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("summon")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            MessageUtils.sendConfigMessage(sender, "stfish.summon-player-only", "&c✗ &f此命令仅限玩家使用");
            return true;
        }

        if (!sender.hasPermission("st.fish.weather")) {
            MessageUtils.sendConfigMessage(sender, "stfish.summon-no-permission", "&c✗ &f您没有权限召唤天气");
            return true;
        }

        if (!economyService.isEnabled()) {
            MessageUtils.sendConfigMessage(sender, "stfish.summon-no-economy", "&c✗ &f经济系统未启用，无法召唤天气");
            return true;
        }

        double cost = configManager.getSummonCost();
        if (economyService.getBalance(player) < cost) {
            MessageUtils.sendConfigMessage(sender, "stfish.summon-insufficient-funds",
                    "&c✗ &f金币不足，需要 &e{cost} &f金币", "cost", NumberFormatUtils.formatWithUnit(cost));
            return true;
        }

        String weatherName = weatherService.summon(player);
        if (weatherName != null) {
            MessageUtils.sendConfigMessage(sender, "stfish.summon-success",
                    "&a✓ &f已消耗 &e{cost} &f金币召唤 &e{weather} &7天气",
                    "cost", NumberFormatUtils.formatWithUnit(cost), "weather", weatherName);
        } else {
            String worldName = configManager.getWeatherWorldName();
            MessageUtils.sendConfigMessage(sender, "stfish.summon-world-not-found",
                    "&c✗ &f天气世界未找到或未加载: &e{world}", "world", worldName.isBlank() ? "(auto)" : worldName);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            if ("weather".startsWith(args[0].toLowerCase())) out.add("weather");
            if ("fish".startsWith(args[0].toLowerCase())) out.add("fish");
            return out;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("weather") && "summon".startsWith(args[1].toLowerCase())) {
                return List.of("summon");
            }
            if (args[0].equalsIgnoreCase("fish")) {
                List<String> out = new ArrayList<>();
                if ("atlas".startsWith(args[1].toLowerCase())) out.add("atlas");
                if ("give".startsWith(args[1].toLowerCase()) && sender.hasPermission("st.fish.give")) out.add("give");
                return out;
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("fish") && args[1].equalsIgnoreCase("give")) {
            String input = args[2].toLowerCase();
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input)).collect(Collectors.toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("fish") && args[1].equalsIgnoreCase("give")) {
            String input = args[3].toLowerCase();
            return configManager.getAllFishOrdered().stream().map(FishDefinition::id)
                    .filter(id -> id.toLowerCase().startsWith(input)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
