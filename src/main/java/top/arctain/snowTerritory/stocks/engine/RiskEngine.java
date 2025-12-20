package top.arctain.snowTerritory.stocks.engine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import top.arctain.snowTerritory.stocks.config.StocksConfigManager;
import top.arctain.snowTerritory.stocks.model.*;
import top.arctain.snowTerritory.stocks.price.PriceService;
import top.arctain.snowTerritory.stocks.storage.StockStorage;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 风控引擎
 * 定时检查所有仓位，执行强平
 */
public class RiskEngine {
    
    private final JavaPlugin plugin;
    private final StockStorage storage;
    private final PriceService priceService;
    private final TradeEngine tradeEngine;
    private final StocksConfigManager configManager;
    private BukkitRunnable checkTask;
    private long checkInterval; // 检查间隔（tick）
    
    public RiskEngine(JavaPlugin plugin, StockStorage storage, PriceService priceService, 
                     TradeEngine tradeEngine, StocksConfigManager configManager, long checkInterval) {
        this.plugin = plugin;
        this.storage = storage;
        this.priceService = priceService;
        this.tradeEngine = tradeEngine;
        this.configManager = configManager;
        this.checkInterval = checkInterval;
    }
    
    /**
     * 启动风控检查
     */
    public void start() {
        if (checkTask != null) {
            return;
        }
        
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAllPositions();
            }
        };
        
        checkTask.runTaskTimer(plugin, checkInterval, checkInterval);
        MessageUtils.logInfo("风控引擎已启动，检查间隔: " + (checkInterval * 50) + "ms");
    }
    
    /**
     * 停止风控检查
     */
    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }
    
    /**
     * 检查所有仓位
     */
    private void checkAllPositions() {
        // 这里需要获取所有玩家的所有仓位
        // 由于是内存存储，我们需要遍历所有仓位
        // 实际实现中，可以从存储层获取所有仓位列表
        
        // 简化实现：遍历所有在线玩家的仓位
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            List<Position> positions = storage.getAllPositions(playerId);
            
            for (Position position : positions) {
                checkLiquidation(player, position);
            }
        }
    }
    
    /**
     * 检查单个仓位是否需要强平
     */
    private void checkLiquidation(Player player, Position position) {
        if (position.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        String symbolName = position.getSymbol();
        BigDecimal markPrice = priceService.getMarkPrice(symbolName);
        
        if (markPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        // 更新未实现盈亏
        BigDecimal unrealizedPnl = MarginModel.calculateUnrealizedPnl(position, markPrice);
        position.setUnrealizedPnl(unrealizedPnl);
        
        // 计算可用保证金
        BigDecimal availableMargin = MarginModel.calculateAvailableMargin(position, markPrice);
        
        // 计算维持保证金
        Symbol symbol = configManager.getSymbol(symbolName);
        BigDecimal mmr;
        if (symbol != null) {
            mmr = symbol.getMaintenanceMarginRate();
        } else {
            mmr = new BigDecimal("0.005"); // 默认0.5%
        }
        BigDecimal maintenanceMargin = MarginModel.calculateMaintenanceMargin(
            markPrice, position.getQty(), mmr);
        
        // 检查是否触发强平
        if (availableMargin.compareTo(maintenanceMargin) <= 0) {
            liquidate(player, position, markPrice);
        }
    }
    
    /**
     * 执行强平
     */
    private void liquidate(Player player, Position position, BigDecimal liquidationPrice) {
        UUID playerId = player.getUniqueId();
        
        // 记录强平事件
        BigDecimal loss = position.getIsolatedMargin().add(position.getUnrealizedPnl());
        if (loss.compareTo(BigDecimal.ZERO) < 0) {
            loss = BigDecimal.ZERO;
        }
        final BigDecimal finalLoss = loss; // 用于内部类
        // 记录强平事件（可以保存到存储中，这里简化处理）
        @SuppressWarnings("unused")
        LiquidationEvent event = new LiquidationEvent(playerId, position.getSymbol(), 
                                                      liquidationPrice, position.getQty(), finalLoss);
        
        // 执行强平：以markPrice强制平仓
        // 需要获取Symbol对象
        Symbol symbol = configManager.getSymbol(position.getSymbol());
        if (symbol == null) {
            MessageUtils.logWarning("无法获取交易对配置: " + position.getSymbol());
            return;
        }
        tradeEngine.closePosition(player, symbol, position.getQty(), new TradeEngine.TradeCallback() {
            @Override
            public void onSuccess(String message) {
                // 强平成功
                MessageUtils.sendError(player, "stocks.liquidation", 
                    "&c✗ &f您的仓位已被强平！损失: " + finalLoss + " USDT");
            }
            
            @Override
            public void onError(String error) {
                MessageUtils.logWarning("强平失败: " + error);
            }
        });
        
        // 清空保证金
        position.setIsolatedMargin(BigDecimal.ZERO);
        storage.savePosition(position);
        
        MessageUtils.logWarning("玩家 " + player.getName() + " 的 " + position.getSymbol() + 
                               " 仓位被强平，损失: " + finalLoss + " USDT");
    }
    
    /**
     * 设置检查间隔
     */
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
        if (checkTask != null) {
            checkTask.cancel();
            start();
        }
    }
}

