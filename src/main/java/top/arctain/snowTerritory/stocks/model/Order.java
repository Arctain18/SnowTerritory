package top.arctain.snowTerritory.stocks.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单模型
 * 记录交易订单信息
 */
public class Order {
    
    private final long id;                   // 订单ID
    private final UUID playerId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType type;
    private BigDecimal qty;                  // 数量
    private OrderStatus status;               // 状态
    private BigDecimal fillPrice;            // 成交价
    private BigDecimal fee;                  // 手续费
    private final long createTime;           // 创建时间（毫秒时间戳）
    
    public Order(long id, UUID playerId, String symbol, OrderSide side, 
                OrderType type, BigDecimal qty) {
        this.id = id;
        this.playerId = playerId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.qty = qty;
        this.status = OrderStatus.PENDING;
        this.fillPrice = BigDecimal.ZERO;
        this.fee = BigDecimal.ZERO;
        this.createTime = System.currentTimeMillis();
    }
    
    /**
     * 订单成交
     */
    public void fill(BigDecimal fillPrice, BigDecimal fee) {
        this.status = OrderStatus.FILLED;
        this.fillPrice = fillPrice;
        this.fee = fee;
    }
    
    /**
     * 订单取消
     */
    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }
    
    /**
     * 订单拒绝
     */
    public void reject() {
        this.status = OrderStatus.REJECTED;
    }
    
    // Getters and Setters
    
    public long getId() {
        return id;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public OrderSide getSide() {
        return side;
    }
    
    public OrderType getType() {
        return type;
    }
    
    public BigDecimal getQty() {
        return qty;
    }
    
    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public BigDecimal getFillPrice() {
        return fillPrice;
    }
    
    public void setFillPrice(BigDecimal fillPrice) {
        this.fillPrice = fillPrice;
    }
    
    public BigDecimal getFee() {
        return fee;
    }
    
    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }
    
    public long getCreateTime() {
        return createTime;
    }
}

