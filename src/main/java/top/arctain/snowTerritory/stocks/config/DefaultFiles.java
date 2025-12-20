package top.arctain.snowTerritory.stocks.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * 生成默认配置文件
 */
public class DefaultFiles {
    
    public static void createDefaultConfig(File configFile) {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            
            YamlConfiguration config = new YamlConfiguration();
            
            // 交易所配置
            config.set("exchange.apiUrl", "https://api.coingecko.com/api/v3");
            config.set("exchange.type", "coingecko"); // coingecko, binance
            
            // 价格更新配置
            config.set("price.updateInterval", 1200L); // tick数，1200 = 60秒（1分钟）
            
            // 风控配置
            config.set("risk.checkInterval", 40L); // tick数，40 = 2秒
            
            // 交易对配置
            config.set("symbols.BTCUSDT.priceTick", "0.01");
            config.set("symbols.BTCUSDT.qtyStep", "0.001");
            config.set("symbols.BTCUSDT.minQty", "0.001");
            config.set("symbols.BTCUSDT.maxQty", "1000");
            config.set("symbols.BTCUSDT.maxLeverage", 20);
            config.set("symbols.BTCUSDT.maintenanceMarginRate", "0.005"); // 0.5%
            config.set("symbols.BTCUSDT.takerFeeRate", "0.0006"); // 0.06%
            
            config.set("symbols.ETHUSDT.priceTick", "0.01");
            config.set("symbols.ETHUSDT.qtyStep", "0.01");
            config.set("symbols.ETHUSDT.minQty", "0.01");
            config.set("symbols.ETHUSDT.maxQty", "10000");
            config.set("symbols.ETHUSDT.maxLeverage", 20);
            config.set("symbols.ETHUSDT.maintenanceMarginRate", "0.005");
            config.set("symbols.ETHUSDT.takerFeeRate", "0.0006");
            
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

