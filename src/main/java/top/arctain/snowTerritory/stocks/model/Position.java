package top.arctain.snowTerritory.stocks.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 仓位模型
 * 存储玩家的持仓信息（逐仓模式）
 */
public class Position {
    
    private final UUID playerId;
    private final String symbol;
    private OrderSide side;              // 方向：LONG/SHORT
    private BigDecimal qty;              // 持仓数量（币数量）
    private BigDecimal entryPrice;       // 开仓均价
    private int leverage;                 // 杠杆倍数
    private BigDecimal isolatedMargin;   // 逐仓保证金
    private BigDecimal unrealizedPnl;    // 未实现盈亏
    private BigDecimal liquidationPrice; // 爆仓价（可实时计算或缓存）
    
    public Position(UUID playerId, String symbol, OrderSide side, 
                   BigDecimal qty, BigDecimal entryPrice, int leverage, 
                   BigDecimal isolatedMargin) {
        this.playerId = playerId;
        this.symbol = symbol;
        this.side = side;
        this.qty = qty;
        this.entryPrice = entryPrice;
        this.leverage = leverage;
        this.isolatedMargin = isolatedMargin;
        this.unrealizedPnl = BigDecimal.ZERO;
        this.liquidationPrice = BigDecimal.ZERO;
    }
    
    /**
     * 更新未实现盈亏
     */
    public void updateUnrealizedPnl(BigDecimal currentPrice) {
        if (side == OrderSide.LONG) {
            // 多仓：uPnL = (P - P0) * Q
            this.unrealizedPnl = currentPrice.subtract(entryPrice).multiply(qty);
        } else {
            // 空仓：uPnL = (P0 - P) * Q
            this.unrealizedPnl = entryPrice.subtract(currentPrice).multiply(qty);
        }
    }
    
    /**
     * 追加保证金
     */
    public void addMargin(BigDecimal amount) {
        this.isolatedMargin = isolatedMargin.add(amount);
    }
    
    /**
     * 减少保证金（平仓时返还）
     */
    public void reduceMargin(BigDecimal amount) {
        this.isolatedMargin = isolatedMargin.subtract(amount);
        if (this.isolatedMargin.compareTo(BigDecimal.ZERO) < 0) {
            this.isolatedMargin = BigDecimal.ZERO;
        }
    }
    
    /**
     * 同向加仓（更新加权均价）
     */
    public void addPosition(BigDecimal newQty, BigDecimal newPrice, BigDecimal newMargin) {
        // 加权均价：entryPrice = (oldQty * oldPrice + newQty * newPrice) / (oldQty + newQty)
        BigDecimal totalValue = qty.multiply(entryPrice).add(newQty.multiply(newPrice));
        this.qty = qty.add(newQty);
        this.entryPrice = totalValue.divide(qty, 8, java.math.RoundingMode.HALF_UP);
        this.isolatedMargin = isolatedMargin.add(newMargin);
    }
    
    /**
     * 减仓
     */
    public void reducePosition(BigDecimal reduceQty) {
        this.qty = qty.subtract(reduceQty);
        if (this.qty.compareTo(BigDecimal.ZERO) <= 0) {
            this.qty = BigDecimal.ZERO;
        }
    }
    
    // Getters and Setters
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public OrderSide getSide() {
        return side;
    }
    
    public void setSide(OrderSide side) {
        this.side = side;
    }
    
    public BigDecimal getQty() {
        return qty;
    }
    
    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }
    
    public BigDecimal getEntryPrice() {
        return entryPrice;
    }
    
    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }
    
    public int getLeverage() {
        return leverage;
    }
    
    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }
    
    public BigDecimal getIsolatedMargin() {
        return isolatedMargin;
    }
    
    public void setIsolatedMargin(BigDecimal isolatedMargin) {
        this.isolatedMargin = isolatedMargin;
    }
    
    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }
    
    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }
    
    public BigDecimal getLiquidationPrice() {
        return liquidationPrice;
    }
    
    public void setLiquidationPrice(BigDecimal liquidationPrice) {
        this.liquidationPrice = liquidationPrice;
    }
}

