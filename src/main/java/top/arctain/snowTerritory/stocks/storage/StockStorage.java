package top.arctain.snowTerritory.stocks.storage;

import top.arctain.snowTerritory.stocks.model.Account;
import top.arctain.snowTerritory.stocks.model.Order;
import top.arctain.snowTerritory.stocks.model.Position;

import java.util.List;
import java.util.UUID;

/**
 * 存储接口
 */
public interface StockStorage {
    
    /**
     * 获取账户
     */
    Account getAccount(UUID playerId);
    
    /**
     * 保存账户
     */
    void saveAccount(Account account);
    
    /**
     * 获取仓位
     */
    Position getPosition(UUID playerId, String symbol);
    
    /**
     * 保存仓位
     */
    void savePosition(Position position);
    
    /**
     * 删除仓位
     */
    void deletePosition(UUID playerId, String symbol);
    
    /**
     * 获取玩家所有仓位
     */
    List<Position> getAllPositions(UUID playerId);
    
    /**
     * 保存订单
     */
    void saveOrder(Order order);
    
    /**
     * 获取订单
     */
    Order getOrder(long orderId);
    
    /**
     * 获取玩家的订单历史
     */
    List<Order> getOrders(UUID playerId, String symbol);
}

