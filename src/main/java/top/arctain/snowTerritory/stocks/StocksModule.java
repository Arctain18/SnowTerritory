package top.arctain.snowTerritory.stocks;

import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stocks.command.StockCommand;
import top.arctain.snowTerritory.stocks.config.StocksConfigManager;
import top.arctain.snowTerritory.stocks.engine.RiskEngine;
import top.arctain.snowTerritory.stocks.engine.TradeEngine;
import top.arctain.snowTerritory.stocks.price.ExchangeRestPriceSource;
import top.arctain.snowTerritory.stocks.price.PriceService;
import top.arctain.snowTerritory.stocks.storage.MemoryStockStorage;
import top.arctain.snowTerritory.stocks.storage.StockStorage;
import top.arctain.snowTerritory.utils.MessageUtils;

/**
 * Stocks模块入口
 */
public class StocksModule {
    
    private final Main plugin;
    private final StocksConfigManager configManager;
    private final StockStorage storage;
    private final PriceService priceService;
    private final TradeEngine tradeEngine;
    private final RiskEngine riskEngine;
    private StockCommand stockCommand;
    
    public StocksModule(Main plugin) {
        this.plugin = plugin;
        this.configManager = new StocksConfigManager(plugin);
        // 先加载配置，因为后续初始化需要读取配置
        this.configManager.loadAll();
        this.storage = new MemoryStockStorage();
        this.priceService = new ExchangeRestPriceSource(
            plugin, 
            configManager.getExchangeApiUrl(),
            configManager.getExchangeType(),
            configManager.getPriceUpdateInterval()
        );
        this.tradeEngine = new TradeEngine(storage, priceService);
        this.riskEngine = new RiskEngine(
            plugin,
            storage,
            priceService,
            tradeEngine,
            configManager,
            configManager.getRiskCheckInterval()
        );
    }
    
    public void enable() {
        // 配置已在构造函数中加载，这里只需要启动服务
        
        // 注册需要监控的交易对到价格服务
        if (priceService instanceof ExchangeRestPriceSource) {
            ExchangeRestPriceSource priceSource = (ExchangeRestPriceSource) priceService;
            for (String symbol : configManager.getAllSymbols().keySet()) {
                priceSource.addSymbol(symbol);
            }
        }
        
        priceService.start();
        
        tradeEngine.start();
        
        riskEngine.start();
        
        this.stockCommand = new StockCommand(plugin, configManager, storage, priceService, tradeEngine);
        
        MessageUtils.logSuccess("Stocks 模块已启用，配置目录: plugins/SnowTerritory/stocks/");
    }
    
    public void disable() {
        if (riskEngine != null) {
            riskEngine.stop();
        }
        if (tradeEngine != null) {
            tradeEngine.stop();
        }
        if (priceService != null) {
            priceService.stop();
        }
    }
    
    public void reload() {
        configManager.loadAll();
        // 可以重新加载配置，但价格服务和交易引擎通常不需要重启
    }
    
    public StockCommand getStockCommand() {
        return stockCommand;
    }
    
    public StocksConfigManager getConfigManager() {
        return configManager;
    }
}

