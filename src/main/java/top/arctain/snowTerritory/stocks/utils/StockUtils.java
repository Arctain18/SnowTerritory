package top.arctain.snowTerritory.stocks.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 股票/期货工具类
 */
public class StockUtils {
    
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat QTY_FORMAT = new DecimalFormat("#,##0.00000000");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
    
    /**
     * 格式化价格
     */
    public static String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0.00";
        }
        return PRICE_FORMAT.format(price);
    }
    
    /**
     * 格式化数量
     */
    public static String formatQuantity(BigDecimal qty) {
        if (qty == null) {
            return "0.00000000";
        }
        return QTY_FORMAT.format(qty);
    }
    
    /**
     * 格式化金额
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return AMOUNT_FORMAT.format(amount);
    }
    
    /**
     * 格式化盈亏（带颜色）
     */
    public static String formatPnl(BigDecimal pnl) {
        if (pnl == null) {
            return "&70.00";
        }
        if (pnl.compareTo(BigDecimal.ZERO) > 0) {
            return "&a+" + formatAmount(pnl);
        } else if (pnl.compareTo(BigDecimal.ZERO) < 0) {
            return "&c" + formatAmount(pnl);
        } else {
            return "&7" + formatAmount(pnl);
        }
    }
}

