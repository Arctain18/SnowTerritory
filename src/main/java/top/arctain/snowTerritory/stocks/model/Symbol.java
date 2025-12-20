package top.arctain.snowTerritory.stocks.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 合约品种
 * 定义交易对的各种参数
 */
public class Symbol {
    
    private final String name;                    // 交易对名称，如 BTCUSDT
    private final BigDecimal priceTick;           // 最小价格变动
    private final BigDecimal qtyStep;            // 最小数量步进
    private final BigDecimal minQty;              // 最小数量
    private final BigDecimal maxQty;              // 最大数量
    private final int maxLeverage;                // 最大杠杆
    private final BigDecimal maintenanceMarginRate; // 维持保证金率（如 0.005 = 0.5%）
    private final BigDecimal takerFeeRate;        // 吃单手续费率（如 0.0006 = 0.06%）
    
    public Symbol(String name, BigDecimal priceTick, BigDecimal qtyStep, 
                  BigDecimal minQty, BigDecimal maxQty, int maxLeverage,
                  BigDecimal maintenanceMarginRate, BigDecimal takerFeeRate) {
        this.name = name;
        this.priceTick = priceTick;
        this.qtyStep = qtyStep;
        this.minQty = minQty;
        this.maxQty = maxQty;
        this.maxLeverage = maxLeverage;
        this.maintenanceMarginRate = maintenanceMarginRate;
        this.takerFeeRate = takerFeeRate;
    }
    
    /**
     * 验证价格是否符合价格精度
     */
    public boolean isValidPrice(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        // 检查是否为 priceTick 的整数倍
        BigDecimal remainder = price.remainder(priceTick);
        return remainder.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * 格式化价格（四舍五入到 priceTick）
     */
    public BigDecimal formatPrice(BigDecimal price) {
        if (priceTick.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        BigDecimal divided = price.divide(priceTick, 0, RoundingMode.HALF_UP);
        return divided.multiply(priceTick);
    }
    
    /**
     * 验证数量是否符合要求
     */
    public boolean isValidQuantity(BigDecimal qty) {
        if (qty.compareTo(minQty) < 0) {
            return false;
        }
        if (qty.compareTo(maxQty) > 0) {
            return false;
        }
        // 检查是否为 qtyStep 的整数倍
        BigDecimal remainder = qty.remainder(qtyStep);
        return remainder.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * 格式化数量（四舍五入到 qtyStep）
     */
    public BigDecimal formatQuantity(BigDecimal qty) {
        if (qtyStep.compareTo(BigDecimal.ZERO) == 0) {
            return qty;
        }
        BigDecimal divided = qty.divide(qtyStep, 0, RoundingMode.HALF_UP);
        return divided.multiply(qtyStep);
    }
    
    /**
     * 验证杠杆是否在范围内
     */
    public boolean isValidLeverage(int leverage) {
        return leverage >= 1 && leverage <= maxLeverage;
    }
    
    // Getters
    
    public String getName() {
        return name;
    }
    
    public BigDecimal getPriceTick() {
        return priceTick;
    }
    
    public BigDecimal getQtyStep() {
        return qtyStep;
    }
    
    public BigDecimal getMinQty() {
        return minQty;
    }
    
    public BigDecimal getMaxQty() {
        return maxQty;
    }
    
    public int getMaxLeverage() {
        return maxLeverage;
    }
    
    public BigDecimal getMaintenanceMarginRate() {
        return maintenanceMarginRate;
    }
    
    public BigDecimal getTakerFeeRate() {
        return takerFeeRate;
    }
}

