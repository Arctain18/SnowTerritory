package top.arctain.snowTerritory.stocks.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stocks.config.StocksConfigManager;
import top.arctain.snowTerritory.stocks.engine.TradeEngine;
import top.arctain.snowTerritory.stocks.model.*;
import top.arctain.snowTerritory.stocks.price.PriceService;
import top.arctain.snowTerritory.stocks.storage.StockStorage;
import top.arctain.snowTerritory.stocks.utils.StockUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 期货交易指令处理器
 */
public class StockCommand implements CommandExecutor, TabCompleter {
    
    private final StocksConfigManager configManager;
    private final StockStorage storage;
    private final PriceService priceService;
    private final TradeEngine tradeEngine;
    
    public StockCommand(Main plugin, StocksConfigManager configManager, StockStorage storage,
                       PriceService priceService, TradeEngine tradeEngine) {
        this.configManager = configManager;
        this.storage = storage;
        this.priceService = priceService;
        this.tradeEngine = tradeEngine;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendError(sender, "stocks.player-only", "&c✗ &f此命令仅限玩家使用！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "price":
                return handlePrice(player, args);
            case "open":
                return handleOpen(player, args);
            case "close":
                return handleClose(player, args);
            case "pos":
            case "position":
                return handlePosition(player, args);
            case "margin":
                return handleMargin(player, args);
            case "setlev":
            case "setleverage":
                return handleSetLeverage(player, args);
            case "bal":
            case "balance":
                return handleBalance(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }
    
    /**
     * 查询价格: /sn stock price BTCUSDT
     */
    private boolean handlePrice(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "stocks.usage.price", "&c用法: /sn stock price <symbol>");
            return true;
        }
        
        String symbolName = args[1].toUpperCase();
        Symbol symbol = configManager.getSymbol(symbolName);
        if (symbol == null) {
            MessageUtils.sendError(player, "stocks.symbol-not-found", "&c✗ &f交易对不存在: " + symbolName);
            return true;
        }
        
        BigDecimal markPrice = priceService.getMarkPrice(symbolName);
        BigDecimal lastPrice = priceService.getLastPrice(symbolName);
        
        MessageUtils.sendTitle(player, "&6" + symbolName + " 价格信息");
        MessageUtils.sendRaw(player, "&7标记价格: &e" + StockUtils.formatPrice(markPrice) + " USDT");
        MessageUtils.sendRaw(player, "&7最新价格: &e" + StockUtils.formatPrice(lastPrice) + " USDT");
        MessageUtils.sendSeparator(player);
        
        return true;
    }
    
    /**
     * 开仓: /sn stock open long BTCUSDT 0.01 20
     */
    private boolean handleOpen(Player player, String[] args) {
        if (args.length < 5) {
            MessageUtils.sendError(player, "stocks.usage.open", 
                "&c用法: /sn stock open <long|short> <symbol> <qty> <leverage>");
            return true;
        }
        
        String sideStr = args[1].toLowerCase();
        OrderSide side;
        if (sideStr.equals("long")) {
            side = OrderSide.LONG;
        } else if (sideStr.equals("short")) {
            side = OrderSide.SHORT;
        } else {
            MessageUtils.sendError(player, "stocks.invalid-side", "&c✗ &f方向必须是 long 或 short");
            return true;
        }
        
        String symbolName = args[2].toUpperCase();
        Symbol symbol = configManager.getSymbol(symbolName);
        if (symbol == null) {
            MessageUtils.sendError(player, "stocks.symbol-not-found", "&c✗ &f交易对不存在: " + symbolName);
            return true;
        }
        
        BigDecimal qty;
        int leverage;
        try {
            qty = new BigDecimal(args[3]);
            leverage = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            MessageUtils.sendError(player, "stocks.invalid-number", "&c✗ &f数量或杠杆格式错误");
            return true;
        }
        
        tradeEngine.openPosition(player, symbol, side, qty, leverage, new TradeEngine.TradeCallback() {
            @Override
            public void onSuccess(String message) {
                MessageUtils.sendSuccess(player, "stocks.open-success", "&a✓ &f" + message);
            }
            
            @Override
            public void onError(String error) {
                MessageUtils.sendError(player, "stocks.open-error", "&c✗ &f" + error);
            }
        });
        
        return true;
    }
    
    /**
     * 平仓: /sn stock close BTCUSDT [qty]
     */
    private boolean handleClose(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "stocks.usage.close", 
                "&c用法: /sn stock close <symbol> [qty]");
            return true;
        }
        
        String symbolName = args[1].toUpperCase();
        Symbol symbol = configManager.getSymbol(symbolName);
        if (symbol == null) {
            MessageUtils.sendError(player, "stocks.symbol-not-found", "&c✗ &f交易对不存在: " + symbolName);
            return true;
        }
        
        BigDecimal qty = null;
        if (args.length >= 3) {
            try {
                qty = new BigDecimal(args[2]);
            } catch (NumberFormatException e) {
                MessageUtils.sendError(player, "stocks.invalid-number", "&c✗ &f数量格式错误");
                return true;
            }
        }
        
        tradeEngine.closePosition(player, symbol, qty, new TradeEngine.TradeCallback() {
            @Override
            public void onSuccess(String message) {
                MessageUtils.sendSuccess(player, "stocks.close-success", "&a✓ &f" + message);
            }
            
            @Override
            public void onError(String error) {
                MessageUtils.sendError(player, "stocks.close-error", "&c✗ &f" + error);
            }
        });
        
        return true;
    }
    
    /**
     * 查看持仓: /sn stock pos
     */
    private boolean handlePosition(Player player, String[] args) {
        UUID playerId = player.getUniqueId();
        List<Position> positions = storage.getAllPositions(playerId);
        
        if (positions.isEmpty()) {
            MessageUtils.sendInfo(player, "stocks.no-position", "&7您当前没有持仓");
            return true;
        }
        
        MessageUtils.sendTitle(player, "&6您的持仓");
        
        for (Position position : positions) {
            if (position.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            
            String symbolName = position.getSymbol();
            BigDecimal markPrice = priceService.getMarkPrice(symbolName);
            position.updateUnrealizedPnl(markPrice);
            
            String sideStr = position.getSide() == OrderSide.LONG ? "&a做多" : "&c做空";
            MessageUtils.sendRaw(player, "&7" + symbolName + " " + sideStr);
            MessageUtils.sendRaw(player, "  &7数量: &e" + StockUtils.formatQuantity(position.getQty()));
            MessageUtils.sendRaw(player, "  &7开仓价: &e" + StockUtils.formatPrice(position.getEntryPrice()));
            MessageUtils.sendRaw(player, "  &7当前价: &e" + StockUtils.formatPrice(markPrice));
            MessageUtils.sendRaw(player, "  &7杠杆: &e" + position.getLeverage() + "x");
            MessageUtils.sendRaw(player, "  &7保证金: &e" + StockUtils.formatAmount(position.getIsolatedMargin()) + " USDT");
            MessageUtils.sendRaw(player, "  &7未实现盈亏: " + StockUtils.formatPnl(position.getUnrealizedPnl()));
            MessageUtils.sendSeparator(player);
        }
        
        return true;
    }
    
    /**
     * 追加保证金: /sn stock margin add BTCUSDT 100
     */
    private boolean handleMargin(Player player, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("add")) {
            MessageUtils.sendError(player, "stocks.usage.margin", 
                "&c用法: /sn stock margin add <symbol> <amount>");
            return true;
        }
        
        String symbolName = args[2].toUpperCase();
        Symbol symbol = configManager.getSymbol(symbolName);
        if (symbol == null) {
            MessageUtils.sendError(player, "stocks.symbol-not-found", "&c✗ &f交易对不存在: " + symbolName);
            return true;
        }
        
        BigDecimal amount;
        try {
            amount = new BigDecimal(args[3]);
        } catch (NumberFormatException e) {
            MessageUtils.sendError(player, "stocks.invalid-number", "&c✗ &f金额格式错误");
            return true;
        }
        
        tradeEngine.addMargin(player, symbol, amount, new TradeEngine.TradeCallback() {
            @Override
            public void onSuccess(String message) {
                MessageUtils.sendSuccess(player, "stocks.margin-success", "&a✓ &f" + message);
            }
            
            @Override
            public void onError(String error) {
                MessageUtils.sendError(player, "stocks.margin-error", "&c✗ &f" + error);
            }
        });
        
        return true;
    }
    
    /**
     * 调整杠杆: /sn stock setlev BTCUSDT 10
     */
    private boolean handleSetLeverage(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendError(player, "stocks.usage.setlev", 
                "&c用法: /sn stock setlev <symbol> <leverage>");
            return true;
        }
        
        String symbolName = args[1].toUpperCase();
        Symbol symbol = configManager.getSymbol(symbolName);
        if (symbol == null) {
            MessageUtils.sendError(player, "stocks.symbol-not-found", "&c✗ &f交易对不存在: " + symbolName);
            return true;
        }
        
        int leverage;
        try {
            leverage = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            MessageUtils.sendError(player, "stocks.invalid-number", "&c✗ &f杠杆格式错误");
            return true;
        }
        
        UUID playerId = player.getUniqueId();
        Position position = storage.getPosition(playerId, symbolName);
        if (position != null) {
            MessageUtils.sendError(player, "stocks.cannot-change-leverage", 
                "&c✗ &f已有持仓时不能调整杠杆");
            return true;
        }
        
        if (!symbol.isValidLeverage(leverage)) {
            MessageUtils.sendError(player, "stocks.invalid-leverage", 
                "&c✗ &f杠杆范围: 1-" + symbol.getMaxLeverage());
            return true;
        }
        
        MessageUtils.sendInfo(player, "stocks.leverage-set", 
            "&7杠杆已设置为: &e" + leverage + "x &7(仅对新开仓有效)");
        return true;
    }
    
    /**
     * 查看余额: /sn stock bal
     */
    private boolean handleBalance(Player player, String[] args) {
        UUID playerId = player.getUniqueId();
        Account account = storage.getAccount(playerId);
        
        // 计算总未实现盈亏
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;
        List<Position> positions = storage.getAllPositions(playerId);
        for (Position position : positions) {
            if (position.getQty().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal markPrice = priceService.getMarkPrice(position.getSymbol());
                position.updateUnrealizedPnl(markPrice);
                totalUnrealizedPnl = totalUnrealizedPnl.add(position.getUnrealizedPnl());
            }
        }
        
        account.updateEquity(totalUnrealizedPnl);
        
        MessageUtils.sendTitle(player, "&6账户余额");
        MessageUtils.sendRaw(player, "&7钱包余额: &e" + StockUtils.formatAmount(account.getWalletBalance()) + " USDT");
        MessageUtils.sendRaw(player, "&7可用余额: &e" + StockUtils.formatAmount(account.getAvailableBalance()) + " USDT");
        MessageUtils.sendRaw(player, "&7未实现盈亏: " + StockUtils.formatPnl(totalUnrealizedPnl));
        MessageUtils.sendRaw(player, "&7账户净值: &e" + StockUtils.formatAmount(account.getEquity()) + " USDT");
        MessageUtils.sendSeparator(player);
        
        return true;
    }
    
    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        MessageUtils.sendTitle(player, "&6期货交易系统");
        MessageUtils.sendHelpLine(player, "/sn stock price <symbol>", "查询价格");
        MessageUtils.sendHelpLine(player, "/sn stock open <long|short> <symbol> <qty> <lev>", "开仓");
        MessageUtils.sendHelpLine(player, "/sn stock close <symbol> [qty]", "平仓");
        MessageUtils.sendHelpLine(player, "/sn stock pos", "查看持仓");
        MessageUtils.sendHelpLine(player, "/sn stock margin add <symbol> <amount>", "追加保证金");
        MessageUtils.sendHelpLine(player, "/sn stock setlev <symbol> <lev>", "设置杠杆");
        MessageUtils.sendHelpLine(player, "/sn stock bal", "查看余额");
        MessageUtils.sendSeparator(player);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("price".startsWith(input)) completions.add("price");
            if ("open".startsWith(input)) completions.add("open");
            if ("close".startsWith(input)) completions.add("close");
            if ("pos".startsWith(input)) completions.add("pos");
            if ("margin".startsWith(input)) completions.add("margin");
            if ("setlev".startsWith(input)) completions.add("setlev");
            if ("bal".startsWith(input)) completions.add("bal");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open")) {
                String input = args[1].toLowerCase();
                if ("long".startsWith(input)) completions.add("long");
                if ("short".startsWith(input)) completions.add("short");
            } else if (args[0].equalsIgnoreCase("price") || args[0].equalsIgnoreCase("close") || 
                       args[0].equalsIgnoreCase("margin") || args[0].equalsIgnoreCase("setlev")) {
                // 补全交易对
                String input = args[1].toUpperCase();
                for (String symbol : configManager.getAllSymbols().keySet()) {
                    if (symbol.startsWith(input)) {
                        completions.add(symbol);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("margin") && args[1].equalsIgnoreCase("add")) {
            String input = args[2].toUpperCase();
            for (String symbol : configManager.getAllSymbols().keySet()) {
                if (symbol.startsWith(input)) {
                    completions.add(symbol);
                }
            }
        }
        
        return completions;
    }
}

