package top.arctain.snowTerritory.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.enderstorage.EnderStorageModule;
import top.arctain.snowTerritory.utils.ConfigUtils;
import top.arctain.snowTerritory.quest.QuestModule;
import top.arctain.snowTerritory.reinforce.ReinforceModule;
import top.arctain.snowTerritory.stocks.StocksModule;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class SnowTerritoryCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final PluginConfig config;
    private final DebugResetConfirmHandler debugResetHandler;
    private final ItemIdCommand itemIdCommand;
    private final ReinforceModule reinforceModule;
    private final EnderStorageModule enderModule;
    private final QuestModule questModule;
    private final StocksModule stocksModule;

    public SnowTerritoryCommand(Main plugin, PluginConfig config, ReinforceModule reinforceModule, DebugResetConfirmHandler debugResetHandler) {
        this.plugin = plugin;
        this.config = config;
        this.debugResetHandler = debugResetHandler;
        this.itemIdCommand = new ItemIdCommand();
        this.reinforceModule = reinforceModule;
        this.enderModule = plugin.getEnderStorageModule();
        this.questModule = plugin.getQuestModule();
        this.stocksModule = plugin.getStocksModule();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 处理简写
        if (subCommand.equals("r") || subCommand.equals("reinforce")) {
            return handleReinforce(sender, args);
        } else if (subCommand.equals("reload")) {
            return handleReload(sender, args);
        } else if (subCommand.equals("checkid") || subCommand.equals("check")) {
            return handleCheckID(sender, args);
        } else if (subCommand.equals("es") || subCommand.equals("enderstorage")) {
            return handleEnderStorage(sender, args);
        } else if (subCommand.equals("q") || subCommand.equals("quest")) {
            return handleQuest(sender, args);
        } else if (subCommand.equals("stock") || subCommand.equals("stocks")) {
            return handleStock(sender, args);
        } else if (subCommand.equals("debug")) {
            return handleDebug(sender, args);
        } else {
            MessageUtils.sendError(sender, "command.unknown-command", "&c✗ &f未知的子命令！输入 /{label} 查看帮助。", "label", label);
            return true;
        }
    }

    /**
     * 处理强化命令: /sn reinforce 或 /sn r [玩家名字]
     */
    private boolean handleReinforce(CommandSender sender, String[] args) {
        if (reinforceModule == null || reinforceModule.getReinforceCommand() == null) {
            MessageUtils.sendError(sender, "command.feature-missing", "&c✗ &fReinforce 功能未启用");
            return true;
        }
        // 去除首个 "r" 或 "reinforce"
        String[] forward = new String[Math.max(0, args.length - 1)];
        if (args.length > 1) {
            System.arraycopy(args, 1, forward, 0, args.length - 1);
        }
        boolean handled = reinforceModule.getReinforceCommand().onCommand(sender, null, "snowterritory reinforce", forward);
        if (!handled) {
            MessageUtils.sendError(sender, "reinforce.usage", "&c用法: /sn r [玩家名]");
        }
        return true;
    }

    /**
     * 处理重载命令: /st reload
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mmoitemseditor.reload") && !(sender instanceof Player && ((Player) sender).isOp())) {
            MessageUtils.sendError(sender, "command.no-permission", "&c✗ &f您没有权限使用此命令！");
            return true;
        }

        config.reloadConfig();
        // 同步重载 Reinforce 模块
        if (plugin.getReinforceModule() != null) {
            plugin.getReinforceModule().reload();
        }
        // 同步重载 EnderStorage 模块
        if (plugin.getEnderStorageModule() != null) {
            plugin.getEnderStorageModule().reload();
        }
        // 同步重载 Quest 模块
        if (plugin.getQuestModule() != null) {
            plugin.getQuestModule().reload();
        }
        // 同步重载 Stocks 模块
        if (plugin.getStocksModule() != null) {
            plugin.getStocksModule().reload();
        }
        MessageUtils.sendSuccess(sender, "command.reload-success", "&a✓ &f插件配置已重载！");
        return true;
    }

    /**
     * 处理查看ID命令: /st checkID
     */
    private boolean handleCheckID(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendError(sender, "command.player-only", "&c✗ &f此命令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mmoitemseditor.itemid") && !player.isOp()) {
            MessageUtils.sendError(player, "command.no-permission", "&c✗ &f您没有权限使用此命令！");
            return true;
        }

        return itemIdCommand.onCommand(sender, null, "checkid", new String[0]);
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        MessageUtils.sendHelp(sender);
    }

    /**
     * 处理 EnderStorage 子命令: /sn es ...
     */
    private boolean handleEnderStorage(CommandSender sender, String[] args) {
        if (enderModule == null || enderModule.getEnderCommand() == null) {
            MessageUtils.sendError(sender, "command.feature-missing", "&c✗ &fEnderStorage 功能未启用");
            return true;
        }
        // 去除首个 "es"
        String[] forward = new String[Math.max(0, args.length - 1)];
        if (args.length > 1) {
            System.arraycopy(args, 1, forward, 0, args.length - 1);
        }
        // 校验基本格式：当子参数为空且非玩家则提示
        if (forward.length == 0 && !(sender instanceof Player)) {
            MessageUtils.sendError(sender, "enderstorage.player-only", "&c✗ &f此命令仅限玩家使用");
            return true;
        }
        boolean handled = enderModule.getEnderCommand().onCommand(sender, null, "snowterritory es", forward);
        if (!handled) {
            MessageUtils.sendError(sender, "enderstorage.usage", "&c用法: /sn es reload | /sn es give <player> <itemKey> <amount>");
        }
        return true;
    }

    /**
     * 处理 Quest 子命令: /sn q ...
     */
    private boolean handleQuest(CommandSender sender, String[] args) {
        if (questModule == null || questModule.getQuestCommand() == null) {
            MessageUtils.sendError(sender, "command.feature-missing", "&c✗ &fQuest 功能未启用");
            return true;
        }
        // 去除首个 "q" 或 "quest"
        String[] forward = new String[Math.max(0, args.length - 1)];
        if (args.length > 1) {
            System.arraycopy(args, 1, forward, 0, args.length - 1);
        }
        return questModule.getQuestCommand().onCommand(sender, null, "snowterritory quest", forward);
    }

    /**
     * 处理 Stock 子命令: /sn stock ...
     */
    private boolean handleStock(CommandSender sender, String[] args) {
        if (stocksModule == null || stocksModule.getStockCommand() == null) {
            MessageUtils.sendError(sender, "command.feature-missing", "&c✗ &fStocks 功能未启用");
            return true;
        }
        // 去除首个 "stock" 或 "stocks"
        String[] forward = new String[Math.max(0, args.length - 1)];
        if (args.length > 1) {
            System.arraycopy(args, 1, forward, 0, args.length - 1);
        }
        return stocksModule.getStockCommand().onCommand(sender, null, "snowterritory stock", forward);
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mmoitemseditor.reload") && !(sender instanceof Player && ((Player) sender).isOp())) {
            MessageUtils.sendError(sender, "command.no-permission", "&c✗ &f您没有权限使用此命令！");
            return true;
        }
        if (!(sender instanceof Player player)) {
            MessageUtils.sendError(sender, "command.player-only", "&c✗ &f此命令仅限玩家使用！");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("resetconfig")) {
            MessageUtils.sendConfigMessage(sender, "debug.usage", "&e用法: /sn debug resetconfig [模块] &7(不指定则重置全部)");
            return true;
        }
        java.util.List<String> modules;
        if (args.length >= 3) {
            String m = args[2].toLowerCase();
            if ("all".equals(m) || "全部".equals(m)) {
                modules = new java.util.ArrayList<>();
                if (reinforceModule != null) modules.add("reinforce");
                if (enderModule != null) modules.add("enderstorage");
                if (questModule != null) modules.add("quest");
                if (stocksModule != null) modules.add("stocks");
                if (modules.isEmpty()) {
                    MessageUtils.sendError(sender, "command.feature-missing", "&c✗ &f没有已启用的模块");
                    return true;
                }
            } else if (!java.util.Set.of("reinforce", "enderstorage", "es", "quest", "stocks").contains(m)) {
                MessageUtils.sendConfigMessage(sender, "debug.invalid-module",
                        "&c✗ &f未知模块: {module}，可选: reinforce, enderstorage, quest, stocks, all", "module", m);
                return true;
            } else {
                modules = "es".equals(m) ? java.util.List.of("enderstorage") : java.util.List.of(m);
            }
        } else {
            modules = new java.util.ArrayList<>();
            if (reinforceModule != null) modules.add("reinforce");
            if (enderModule != null) modules.add("enderstorage");
            if (questModule != null) modules.add("quest");
            if (stocksModule != null) modules.add("stocks");
            if (modules.isEmpty()) {
                MessageUtils.sendError(sender, "command.feature-missing", "&c✗ &f没有已启用的模块");
                return true;
            }
        }
        String scopeDesc = modules.size() == 1 ? modules.get(0) : "全部";
        Runnable action = () -> {
            int total = 0;
            for (String mod : modules) {
                total += executeReset(mod);
            }
            MessageUtils.sendConfigMessage(player, "debug.resetconfig-success",
                    "&a✓ &f已删除 {module} 模块 {count} 个配置文件并重载（数据库已保留）",
                    "module", scopeDesc, "count", String.valueOf(total));
        };
        debugResetHandler.request(player, action, scopeDesc);
        MessageUtils.sendConfigMessage(sender, "debug.confirm-prompt",
                "&e⚠ &f确定要删除 {module} 的配置并重载吗？&c输入 yes 确认&f，30秒内有效",
                "module", scopeDesc);
        return true;
    }

    private int executeReset(String module) {
        return switch (module) {
            case "reinforce" -> {
                if (reinforceModule != null) {
                    int n = ConfigUtils.deleteConfigFilesExcludingDatabase(reinforceModule.getConfigManager().getBaseDir());
                    reinforceModule.reload();
                    yield n;
                }
                yield 0;
            }
            case "enderstorage" -> {
                if (enderModule != null) {
                    int n = ConfigUtils.deleteConfigFilesExcludingDatabase(enderModule.getConfigManager().getBaseDir());
                    enderModule.reload();
                    yield n;
                }
                yield 0;
            }
            case "quest" -> {
                if (questModule != null) {
                    int n = ConfigUtils.deleteConfigFilesExcludingDatabase(questModule.getConfigManager().getBaseDir());
                    questModule.reload();
                    yield n;
                }
                yield 0;
            }
            case "stocks" -> {
                if (stocksModule != null) {
                    int n = ConfigUtils.deleteConfigFilesExcludingDatabase(stocksModule.getConfigManager().getBaseDir());
                    stocksModule.reload();
                    yield n;
                }
                yield 0;
            }
            default -> 0;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            
            if ("reinforce".startsWith(input) || "r".startsWith(input)) {
                completions.add("reinforce");
            }
            if ("reload".startsWith(input)) {
                completions.add("reload");
            }
            if ("checkid".startsWith(input) || "check".startsWith(input)) {
                completions.add("checkID");
            }
            if ("es".startsWith(input) || "enderstorage".startsWith(input)) {
                completions.add("es");
            }
            if ("q".startsWith(input) || "quest".startsWith(input)) {
                completions.add("quest");
            }
            if ("stock".startsWith(input) || "stocks".startsWith(input)) {
                completions.add("stock");
            }
            if ("debug".startsWith(input)) {
                completions.add("debug");
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            if ("resetconfig".startsWith(args[1].toLowerCase())) {
                return List.of("resetconfig");
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("resetconfig")) {
            List<String> modules = new ArrayList<>();
            String input = args[2].toLowerCase();
            if ("reinforce".startsWith(input)) modules.add("reinforce");
            if ("enderstorage".startsWith(input) || "es".startsWith(input)) modules.add("enderstorage");
            if ("quest".startsWith(input)) modules.add("quest");
            if ("stocks".startsWith(input)) modules.add("stocks");
            if ("all".startsWith(input) || "全部".startsWith(input)) modules.add("all");
            return modules;
        }

        // /sn es 子命令补全
        if (args.length == 2 && (args[0].equalsIgnoreCase("es") || args[0].equalsIgnoreCase("enderstorage"))) {
            List<String> subs = new ArrayList<>();
            subs.add("reload");
            subs.add("give");
            return subs;
        }
        
        // /sn q 子命令补全
        if (args.length == 2 && (args[0].equalsIgnoreCase("q") || args[0].equalsIgnoreCase("quest"))) {
            if (questModule != null && questModule.getQuestCommand() != null) {
                return questModule.getQuestCommand().onTabComplete(sender, null, "quest", new String[]{args[1]});
            }
        }
        
        // /sn stock 子命令补全
        if (args.length >= 2 && (args[0].equalsIgnoreCase("stock") || args[0].equalsIgnoreCase("stocks"))) {
            if (stocksModule != null && stocksModule.getStockCommand() != null) {
                String[] forward = new String[args.length - 1];
                System.arraycopy(args, 1, forward, 0, args.length - 1);
                return stocksModule.getStockCommand().onTabComplete(sender, null, "stock", forward);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("es") && args[1].equalsIgnoreCase("give")) {
            List<String> playerNames = new ArrayList<>();
            String input = args[2].toLowerCase();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    playerNames.add(player.getName());
                }
            }
            return playerNames;
        }
        
        // 当输入 /sn r 后，补全玩家名字
        if (args.length == 2 && (args[0].equalsIgnoreCase("r") || args[0].equalsIgnoreCase("reinforce"))) {
            // 检查是否有权限为其他玩家打开GUI
            if (sender.hasPermission("mmoitemseditor.openforothers") || 
                (sender instanceof Player && ((Player) sender).isOp()) || 
                sender instanceof org.bukkit.command.ConsoleCommandSender) {
                
                List<String> playerNames = new ArrayList<>();
                String input = args[1].toLowerCase();
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        playerNames.add(player.getName());
                    }
                }
                
                return playerNames;
            }
        }
        
        return new ArrayList<>();
    }
}

