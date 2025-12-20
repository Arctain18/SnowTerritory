package top.arctain.snowTerritory.stocks.engine;

import top.arctain.snowTerritory.stocks.model.Symbol;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 手续费计算模型
 */
public class FeeModel {
    
    /**
     * 计算taker手续费
     * fee = price * qty * takerFeeRate
     */
    public static BigDecimal calculateTakerFee(Symbol symbol, BigDecimal qty, BigDecimal price) {
        BigDecimal notional = price.multiply(qty);
        BigDecimal fee = notional.multiply(symbol.getTakerFeeRate());
        // 保留8位小数
        return fee.setScale(8, RoundingMode.HALF_UP);
    }
}

