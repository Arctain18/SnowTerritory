package top.arctain.snowTerritory.stocks.model;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    PENDING,    // 待处理
    FILLED,     // 已成交
    CANCELLED,  // 已取消
    REJECTED    // 已拒绝
}

