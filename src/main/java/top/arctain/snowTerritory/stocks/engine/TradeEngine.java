package top.arctain.snowTerritory.stocks.engine;

import org.bukkit.entity.Player;
import top.arctain.snowTerritory.stocks.model.*;
import top.arctain.snowTerritory.stocks.price.PriceService;
import top.arctain.snowTerritory.stocks.storage.MemoryStockStorage;
import top.arctain.snowTerritory.stocks.storage.StockStorage;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 交易引擎
 * 处理开仓、平仓、追加保证金等操作
 * 使用单线程队列避免并发问题
 */
public class TradeEngine {
    
    private final StockStorage storage;
    private final PriceService priceService;
    private final BlockingQueue<Runnable> tradeQueue;
    private Thread tradeThread;
    private volatile boolean running = false;
    
    public TradeEngine(StockStorage storage, PriceService priceService) {
        this.storage = storage;
        this.priceService = priceService;
        this.tradeQueue = new LinkedBlockingQueue<>();
    }
    
    /**
     * 启动交易引擎
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        tradeThread = new Thread(() -> {
            while (running) {
                try {
                    Runnable task = tradeQueue.take();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "StockTradeEngine");
        tradeThread.start();
    }
    
    /**
     * 停止交易引擎
     */
    public void stop() {
        running = false;
        if (tradeThread != null) {
            tradeThread.interrupt();
            try {
                tradeThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 开仓（异步执行）
     */
    public void openPosition(Player player, Symbol symbol, OrderSide side, 
                            BigDecimal qty, int leverage, TradeCallback callback) {
        tradeQueue.offer(() -> {
            try {
                openPositionSync(player, symbol, side, qty, leverage, callback);
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 开仓（同步执行，在交易线程中）
     */
    private void openPositionSync(Player player, Symbol symbol, OrderSide side,
                                  BigDecimal qty, int leverage, TradeCallback callback) {
        UUID playerId = player.getUniqueId();
        
        // 1. 验证杠杆
        if (!symbol.isValidLeverage(leverage)) {
            if (callback != null) {
                callback.onError("杠杆倍数无效，范围: 1-" + symbol.getMaxLeverage());
            }
            return;
        }
        
        // 2. 格式化数量
        qty = symbol.formatQuantity(qty);
        if (!symbol.isValidQuantity(qty)) {
            if (callback != null) {
                callback.onError("数量无效，范围: " + symbol.getMinQty() + "-" + symbol.getMaxQty());
            }
            return;
        }
        
        // 3. 获取当前价格
        BigDecimal currentPrice = priceService.getLastPrice(symbol.getName());
        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            if (callback != null) {
                callback.onError("无法获取价格，请稍后重试");
            }
            return;
        }
        
        // 4. 计算初始保证金和手续费
        BigDecimal initialMargin = MarginModel.calculateInitialMargin(currentPrice, qty, leverage);
        BigDecimal fee = FeeModel.calculateTakerFee(symbol, qty, currentPrice);
        BigDecimal totalRequired = initialMargin.add(fee);
        
        // 5. 检查余额
        Account account = storage.getAccount(playerId);
        if (account.getAvailableBalance().compareTo(totalRequired) < 0) {
            if (callback != null) {
                callback.onError("余额不足，需要: " + totalRequired + " USDT，可用: " + account.getAvailableBalance() + " USDT");
            }
            return;
        }
        
        // 6. 扣除保证金和手续费
        account.deductWalletBalance(fee);
        account.setWalletBalance(account.getWalletBalance().subtract(initialMargin));
        account.updateAvailableBalance(BigDecimal.ZERO);
        storage.saveAccount(account);
        
        // 7. 创建订单
        long orderId = ((MemoryStockStorage) storage).getNextOrderId();
        Order order = new Order(orderId, playerId, symbol.getName(), side, OrderType.MARKET, qty);
        order.fill(currentPrice, fee);
        storage.saveOrder(order);
        
        // 8. 处理仓位
        Position existingPosition = storage.getPosition(playerId, symbol.getName());
        if (existingPosition != null && existingPosition.getSide() == side) {
            // 同向加仓：更新加权均价
            existingPosition.addPosition(qty, currentPrice, initialMargin);
            storage.savePosition(existingPosition);
        } else if (existingPosition != null && existingPosition.getSide() != side) {
            // 反向开仓：先平掉反向仓位
            if (existingPosition.getQty().compareTo(qty) <= 0) {
                // 全平反向仓位
                closePositionSync(player, symbol, existingPosition.getQty(), null);
                // 然后开新仓
                Position newPosition = new Position(playerId, symbol.getName(), side, qty, currentPrice, leverage, initialMargin);
                storage.savePosition(newPosition);
            } else {
                // 部分平仓
                closePositionSync(player, symbol, qty, null);
            }
        } else {
            // 新开仓
            Position newPosition = new Position(playerId, symbol.getName(), side, qty, currentPrice, leverage, initialMargin);
            storage.savePosition(newPosition);
        }
        
        if (callback != null) {
            callback.onSuccess("开仓成功: " + side + " " + qty + " " + symbol.getName() + " @ " + currentPrice);
        }
    }
    
    /**
     * 平仓（异步执行）
     */
    public void closePosition(Player player, Symbol symbol, BigDecimal qty, TradeCallback callback) {
        tradeQueue.offer(() -> {
            try {
                closePositionSync(player, symbol, qty, callback);
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 平仓（同步执行）
     */
    private void closePositionSync(Player player, Symbol symbol, BigDecimal qty, TradeCallback callback) {
        UUID playerId = player.getUniqueId();
        
        Position position = storage.getPosition(playerId, symbol.getName());
        if (position == null || position.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            if (callback != null) {
                callback.onError("没有持仓");
            }
            return;
        }
        
        // 如果qty为null或大于持仓量，则全平
        if (qty == null || qty.compareTo(position.getQty()) > 0) {
            qty = position.getQty();
        }
        
        // 格式化数量
        qty = symbol.formatQuantity(qty);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            if (callback != null) {
                callback.onError("平仓数量必须大于0");
            }
            return;
        }
        
        // 获取当前价格
        BigDecimal currentPrice = priceService.getLastPrice(symbol.getName());
        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            if (callback != null) {
                callback.onError("无法获取价格，请稍后重试");
            }
            return;
        }
        
        // 计算已实现盈亏
        BigDecimal entryPrice = position.getEntryPrice();
        BigDecimal realizedPnl;
        if (position.getSide() == OrderSide.LONG) {
            realizedPnl = currentPrice.subtract(entryPrice).multiply(qty);
        } else {
            realizedPnl = entryPrice.subtract(currentPrice).multiply(qty);
        }
        
        // 计算手续费
        BigDecimal fee = FeeModel.calculateTakerFee(symbol, qty, currentPrice);
        
        // 计算返还的保证金（按比例）
        BigDecimal marginRatio = qty.divide(position.getQty(), 8, java.math.RoundingMode.HALF_UP);
        BigDecimal returnedMargin = position.getIsolatedMargin().multiply(marginRatio);
        
        // 更新账户
        Account account = storage.getAccount(playerId);
        BigDecimal netReturn = returnedMargin.add(realizedPnl).subtract(fee);
        account.addWalletBalance(netReturn);
        account.updateAvailableBalance(BigDecimal.ZERO);
        storage.saveAccount(account);
        
        // 创建订单记录
        long orderId = ((MemoryStockStorage) storage).getNextOrderId();
        Order order = new Order(orderId, playerId, symbol.getName(), 
                               position.getSide() == OrderSide.LONG ? OrderSide.SHORT : OrderSide.LONG,
                               OrderType.MARKET, qty);
        order.fill(currentPrice, fee);
        storage.saveOrder(order);
        
        // 更新仓位
        position.reducePosition(qty);
        position.reduceMargin(returnedMargin);
        if (position.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            storage.deletePosition(playerId, symbol.getName());
        } else {
            storage.savePosition(position);
        }
        
        if (callback != null) {
            callback.onSuccess("平仓成功: " + qty + " " + symbol.getName() + " @ " + currentPrice + 
                             ", 盈亏: " + realizedPnl + " USDT");
        }
    }
    
    /**
     * 追加保证金（异步执行）
     */
    public void addMargin(Player player, Symbol symbol, BigDecimal amount, TradeCallback callback) {
        tradeQueue.offer(() -> {
            try {
                addMarginSync(player, symbol, amount, callback);
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 追加保证金（同步执行）
     */
    private void addMarginSync(Player player, Symbol symbol, BigDecimal amount, TradeCallback callback) {
        UUID playerId = player.getUniqueId();
        
        Position position = storage.getPosition(playerId, symbol.getName());
        if (position == null) {
            if (callback != null) {
                callback.onError("没有持仓");
            }
            return;
        }
        
        Account account = storage.getAccount(playerId);
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            if (callback != null) {
                callback.onError("余额不足");
            }
            return;
        }
        
        // 扣除钱包余额
        account.setWalletBalance(account.getWalletBalance().subtract(amount));
        account.updateAvailableBalance(BigDecimal.ZERO);
        storage.saveAccount(account);
        
        // 追加到逐仓保证金
        position.addMargin(amount);
        storage.savePosition(position);
        
        if (callback != null) {
            callback.onSuccess("追加保证金成功: " + amount + " USDT");
        }
    }
    
    /**
     * 交易回调接口
     */
    public interface TradeCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}

