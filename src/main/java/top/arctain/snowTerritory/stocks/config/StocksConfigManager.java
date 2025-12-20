package top.arctain.snowTerritory.stocks.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stocks.model.Symbol;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Stocks配置管理器
 */
public class StocksConfigManager {
    
    private final File baseDir;
    private FileConfiguration mainConfig;
    private final Map<String, Symbol> symbols = new HashMap<>();
    
    public StocksConfigManager(Main plugin) {
        this.baseDir = new File(plugin.getDataFolder(), "stocks");
    }
    
    public void loadAll() {
        ensureDefaults();
        loadMainConfig();
        loadSymbols();
    }
    
    private void ensureDefaults() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 stocks 目录失败: " + baseDir.getAbsolutePath());
        }
    }
    
    private void loadMainConfig() {
        File configFile = new File(baseDir, "config.yml");
        if (!configFile.exists()) {
            DefaultFiles.createDefaultConfig(configFile);
        }
        mainConfig = YamlConfiguration.loadConfiguration(configFile);
    }
    
    private void loadSymbols() {
        symbols.clear();
        
        // 从配置加载交易对
        if (mainConfig.contains("symbols")) {
            for (String symbolName : mainConfig.getConfigurationSection("symbols").getKeys(false)) {
                String path = "symbols." + symbolName;
                
                BigDecimal priceTick = new BigDecimal(mainConfig.getString(path + ".priceTick", "0.01"));
                BigDecimal qtyStep = new BigDecimal(mainConfig.getString(path + ".qtyStep", "0.001"));
                BigDecimal minQty = new BigDecimal(mainConfig.getString(path + ".minQty", "0.001"));
                BigDecimal maxQty = new BigDecimal(mainConfig.getString(path + ".maxQty", "1000"));
                int maxLeverage = mainConfig.getInt(path + ".maxLeverage", 20);
                BigDecimal mmr = new BigDecimal(mainConfig.getString(path + ".maintenanceMarginRate", "0.005"));
                BigDecimal takerFee = new BigDecimal(mainConfig.getString(path + ".takerFeeRate", "0.0006"));
                
                Symbol symbol = new Symbol(symbolName, priceTick, qtyStep, minQty, maxQty, 
                                          maxLeverage, mmr, takerFee);
                symbols.put(symbolName, symbol);
            }
        }
        
        MessageUtils.logInfo("已加载 " + symbols.size() + " 个交易对");
    }
    
    public Symbol getSymbol(String name) {
        return symbols.get(name);
    }
    
    public Map<String, Symbol> getAllSymbols() {
        return new HashMap<>(symbols);
    }
    
    public String getExchangeApiUrl() {
        return mainConfig.getString("exchange.apiUrl", "https://api.coingecko.com/api/v3");
    }
    
    public String getExchangeType() {
        return mainConfig.getString("exchange.type", "coingecko");
    }
    
    public long getPriceUpdateInterval() {
        return mainConfig.getLong("price.updateInterval", 1200L); // 1200 tick = 60秒（1分钟）
    }
    
    public long getRiskCheckInterval() {
        return mainConfig.getLong("risk.checkInterval", 40L); // 40 tick = 2秒
    }
    
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }
}

