package top.arctain.snowTerritory.stocks.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 强平事件记录
 */
public class LiquidationEvent {
    
    private final UUID playerId;
    private final String symbol;
    private final BigDecimal liquidationPrice;
    private final BigDecimal qty;
    private final BigDecimal loss;
    private final long timestamp;
    
    public LiquidationEvent(UUID playerId, String symbol, BigDecimal liquidationPrice,
                           BigDecimal qty, BigDecimal loss) {
        this.playerId = playerId;
        this.symbol = symbol;
        this.liquidationPrice = liquidationPrice;
        this.qty = qty;
        this.loss = loss;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public BigDecimal getLiquidationPrice() {
        return liquidationPrice;
    }
    
    public BigDecimal getQty() {
        return qty;
    }
    
    public BigDecimal getLoss() {
        return loss;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}

