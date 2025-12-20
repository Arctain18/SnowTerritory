package top.arctain.snowTerritory.stocks.engine;

import top.arctain.snowTerritory.stocks.model.OrderSide;
import top.arctain.snowTerritory.stocks.model.Position;
import top.arctain.snowTerritory.stocks.model.Symbol;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 保证金计算模型
 */
public class MarginModel {
    
    /**
     * 计算初始保证金
     * IM = (P * Q) / L
     */
    public static BigDecimal calculateInitialMargin(BigDecimal price, BigDecimal qty, int leverage) {
        BigDecimal notional = price.multiply(qty);
        BigDecimal margin = notional.divide(new BigDecimal(leverage), 8, RoundingMode.HALF_UP);
        return margin;
    }
    
    /**
     * 计算维持保证金
     * MM = V * mmr
     */
    public static BigDecimal calculateMaintenanceMargin(BigDecimal price, BigDecimal qty, BigDecimal mmr) {
        BigDecimal notional = price.multiply(qty);
        BigDecimal margin = notional.multiply(mmr);
        return margin.setScale(8, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算未实现盈亏（线性，USDT本位）
     * 多仓：uPnL = (P - P0) * Q
     * 空仓：uPnL = (P0 - P) * Q
     */
    public static BigDecimal calculateUnrealizedPnl(Position position, BigDecimal currentPrice) {
        BigDecimal qty = position.getQty();
        BigDecimal entryPrice = position.getEntryPrice();
        
        if (position.getSide() == OrderSide.LONG) {
            // 多仓
            return currentPrice.subtract(entryPrice).multiply(qty);
        } else {
            // 空仓
            return entryPrice.subtract(currentPrice).multiply(qty);
        }
    }
    
    /**
     * 计算可用保证金（逐仓）
     * Margin = isolatedMargin + uPnL
     */
    public static BigDecimal calculateAvailableMargin(Position position, BigDecimal currentPrice) {
        BigDecimal isolatedMargin = position.getIsolatedMargin();
        BigDecimal unrealizedPnl = calculateUnrealizedPnl(position, currentPrice);
        return isolatedMargin.add(unrealizedPnl);
    }
    
    /**
     * 计算爆仓价（数值方法）
     * 当 availableMargin <= maintenanceMargin 时触发强平
     * 多仓：liquidationPrice = entryPrice - (isolatedMargin / qty) * (1 - mmr)
     * 空仓：liquidationPrice = entryPrice + (isolatedMargin / qty) * (1 - mmr)
     */
    public static BigDecimal calculateLiquidationPrice(Position position, Symbol symbol) {
        BigDecimal qty = position.getQty();
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal entryPrice = position.getEntryPrice();
        BigDecimal isolatedMargin = position.getIsolatedMargin();
        BigDecimal mmr = symbol.getMaintenanceMarginRate();
        
        // 计算每单位币的保证金
        BigDecimal marginPerUnit = isolatedMargin.divide(qty, 8, RoundingMode.HALF_UP);
        
        if (position.getSide() == OrderSide.LONG) {
            // 多仓爆仓价 = 开仓价 - (保证金/数量) * (1 - 维持保证金率)
            BigDecimal adjustment = marginPerUnit.multiply(BigDecimal.ONE.subtract(mmr));
            return entryPrice.subtract(adjustment);
        } else {
            // 空仓爆仓价 = 开仓价 + (保证金/数量) * (1 - 维持保证金率)
            BigDecimal adjustment = marginPerUnit.multiply(BigDecimal.ONE.subtract(mmr));
            return entryPrice.add(adjustment);
        }
    }
}

