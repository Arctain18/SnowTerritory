package top.arctain.snowTerritory.stocks.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 账户模型
 * 存储玩家的钱包余额、可用余额、净值等信息
 */
public class Account {
    
    private final UUID playerId;
    private BigDecimal walletBalance;      // 钱包余额（USDT）
    private BigDecimal availableBalance;  // 可用余额（扣除冻结和逐仓占用）
    private BigDecimal equity;            // 净值（钱包 + 未实现盈亏）
    private String marginMode;            // 保证金模式（ISOLATED/CROSS，先只做逐仓）
    
    public Account(UUID playerId) {
        this.playerId = playerId;
        this.walletBalance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.equity = BigDecimal.ZERO;
        this.marginMode = "ISOLATED";
    }
    
    public Account(UUID playerId, BigDecimal walletBalance) {
        this.playerId = playerId;
        this.walletBalance = walletBalance;
        this.availableBalance = walletBalance;
        this.equity = walletBalance;
        this.marginMode = "ISOLATED";
    }
    
    /**
     * 更新可用余额（扣除冻结保证金）
     */
    public void updateAvailableBalance(BigDecimal isolatedMargin) {
        this.availableBalance = walletBalance.subtract(isolatedMargin);
        if (this.availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            this.availableBalance = BigDecimal.ZERO;
        }
    }
    
    /**
     * 更新净值（钱包 + 未实现盈亏）
     */
    public void updateEquity(BigDecimal unrealizedPnl) {
        this.equity = walletBalance.add(unrealizedPnl);
    }
    
    /**
     * 扣除钱包余额（用于手续费等）
     */
    public void deductWalletBalance(BigDecimal amount) {
        this.walletBalance = walletBalance.subtract(amount);
        if (this.walletBalance.compareTo(BigDecimal.ZERO) < 0) {
            this.walletBalance = BigDecimal.ZERO;
        }
        updateAvailableBalance(BigDecimal.ZERO);
    }
    
    /**
     * 增加钱包余额
     */
    public void addWalletBalance(BigDecimal amount) {
        this.walletBalance = walletBalance.add(amount);
        updateAvailableBalance(BigDecimal.ZERO);
    }
    
    // Getters and Setters
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public BigDecimal getWalletBalance() {
        return walletBalance;
    }
    
    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
        updateAvailableBalance(BigDecimal.ZERO);
    }
    
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
    
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }
    
    public BigDecimal getEquity() {
        return equity;
    }
    
    public void setEquity(BigDecimal equity) {
        this.equity = equity;
    }
    
    public String getMarginMode() {
        return marginMode;
    }
    
    public void setMarginMode(String marginMode) {
        this.marginMode = marginMode;
    }
}

