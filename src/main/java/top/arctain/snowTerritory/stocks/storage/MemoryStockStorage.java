package top.arctain.snowTerritory.stocks.storage;

import top.arctain.snowTerritory.stocks.model.Account;
import top.arctain.snowTerritory.stocks.model.Order;
import top.arctain.snowTerritory.stocks.model.Position;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存存储实现（使用ConcurrentHashMap）
 */
public class MemoryStockStorage implements StockStorage {
    
    // 账户存储：playerId -> Account
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    
    // 仓位存储：playerId_symbol -> Position
    private final Map<String, Position> positions = new ConcurrentHashMap<>();
    
    // 订单存储：orderId -> Order
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    
    // 玩家订单索引：playerId -> List<orderId>
    private final Map<UUID, List<Long>> playerOrders = new ConcurrentHashMap<>();
    
    private long nextOrderId = 1;
    
    @Override
    public Account getAccount(UUID playerId) {
        return accounts.computeIfAbsent(playerId, Account::new);
    }
    
    @Override
    public void saveAccount(Account account) {
        accounts.put(account.getPlayerId(), account);
    }
    
    @Override
    public Position getPosition(UUID playerId, String symbol) {
        String key = playerId.toString() + "_" + symbol;
        return positions.get(key);
    }
    
    @Override
    public void savePosition(Position position) {
        String key = position.getPlayerId().toString() + "_" + position.getSymbol();
        positions.put(key, position);
    }
    
    @Override
    public void deletePosition(UUID playerId, String symbol) {
        String key = playerId.toString() + "_" + symbol;
        positions.remove(key);
    }
    
    @Override
    public List<Position> getAllPositions(UUID playerId) {
        List<Position> result = new ArrayList<>();
        String prefix = playerId.toString() + "_";
        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }
    
    @Override
    public void saveOrder(Order order) {
        orders.put(order.getId(), order);
        playerOrders.computeIfAbsent(order.getPlayerId(), k -> new ArrayList<>()).add(order.getId());
    }
    
    @Override
    public Order getOrder(long orderId) {
        return orders.get(orderId);
    }
    
    @Override
    public List<Order> getOrders(UUID playerId, String symbol) {
        List<Long> orderIds = playerOrders.getOrDefault(playerId, Collections.emptyList());
        List<Order> result = new ArrayList<>();
        for (Long orderId : orderIds) {
            Order order = orders.get(orderId);
            if (order != null && (symbol == null || order.getSymbol().equals(symbol))) {
                result.add(order);
            }
        }
        // 按创建时间倒序
        result.sort((a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()));
        return result;
    }
    
    /**
     * 生成下一个订单ID
     */
    public long getNextOrderId() {
        return nextOrderId++;
    }
}

