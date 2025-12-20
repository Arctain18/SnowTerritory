package top.arctain.snowTerritory.stocks.price;

import java.math.BigDecimal;

/**
 * 价格服务接口
 * 提供标记价格和最新价格
 */
public interface PriceService {
    
    /**
     * 获取标记价格（用于计算盈亏和强平）
     */
    BigDecimal getMarkPrice(String symbol);
    
    /**
     * 获取最新价格（用于市价成交）
     */
    BigDecimal getLastPrice(String symbol);
    
    /**
     * 启动价格更新服务
     */
    void start();
    
    /**
     * 停止价格更新服务
     */
    void stop();
    
    /**
     * 检查交易对是否支持
     */
    boolean isSymbolSupported(String symbol);
}

