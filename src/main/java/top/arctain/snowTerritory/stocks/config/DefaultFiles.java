package top.arctain.snowTerritory.stocks.config;

/** 默认配置内容，首次运行时写入 plugins/SnowTerritory/stocks/ */
public final class DefaultFiles {

    private DefaultFiles() {
    }

    public static final String DEFAULT_CONFIG = """
            exchange:
              apiUrl: "https://api.coingecko.com/api/v3"
              type: coingecko

            price:
              updateInterval: 1200

            risk:
              checkInterval: 40

            symbols:
              BTCUSDT:
                priceTick: "0.01"
                qtyStep: "0.001"
                minQty: "0.001"
                maxQty: "1000"
                maxLeverage: 20
                maintenanceMarginRate: "0.005"
                takerFeeRate: "0.0006"
              ETHUSDT:
                priceTick: "0.01"
                qtyStep: "0.01"
                minQty: "0.01"
                maxQty: "10000"
                maxLeverage: 20
                maintenanceMarginRate: "0.005"
                takerFeeRate: "0.0006"
            """;
}
