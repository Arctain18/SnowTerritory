package top.arctain.snowTerritory.stocks.price;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 从CoinGecko API拉取价格
 */
public class ExchangeRestPriceSource implements PriceService {
    
    private final JavaPlugin plugin;
    private final String exchangeApiUrl;
    private final long updateInterval; // 更新间隔（tick，20tick=1秒）
    private final ConcurrentMap<String, BigDecimal> markPrices = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> symbolToCoinId = new ConcurrentHashMap<>(); // 交易对到CoinGecko币种ID的映射
    private final ConcurrentMap<String, Boolean> symbolsToUpdate = new ConcurrentHashMap<>(); // 需要更新的交易对列表
    private final ConcurrentMap<String, Long> lastErrorTime = new ConcurrentHashMap<>(); // 记录错误时间，避免频繁日志
    private BukkitRunnable updateTask;
    private boolean running = false;
    
    public ExchangeRestPriceSource(JavaPlugin plugin, String exchangeApiUrl, String exchangeType, long updateInterval) {
        this.plugin = plugin;
        this.exchangeApiUrl = exchangeApiUrl != null ? exchangeApiUrl : "https://api.coingecko.com/api/v3";
        this.updateInterval = updateInterval;
        initSymbolMapping();
    }
    
    /**
     * 初始化交易对到CoinGecko币种ID的映射
     */
    private void initSymbolMapping() {
        // BTCUSDT -> bitcoin
        symbolToCoinId.put("BTCUSDT", "bitcoin");
        symbolToCoinId.put("BTC", "bitcoin");
        // ETHUSDT -> ethereum
        symbolToCoinId.put("ETHUSDT", "ethereum");
        symbolToCoinId.put("ETH", "ethereum");
        // 可以继续添加更多映射
    }
    
    /**
     * 添加需要监控的交易对
     */
    public void addSymbol(String symbol) {
        symbolsToUpdate.put(symbol, true);
    }
    
    /**
     * 移除监控的交易对
     */
    public void removeSymbol(String symbol) {
        symbolsToUpdate.remove(symbol);
    }
    
    @Override
    public BigDecimal getMarkPrice(String symbol) {
        return markPrices.getOrDefault(symbol, BigDecimal.ZERO);
    }
    
    @Override
    public BigDecimal getLastPrice(String symbol) {
        BigDecimal lastPrice = lastPrices.get(symbol);
        if (lastPrice == null) {
            // 如果没有最新价，使用标记价格
            return getMarkPrice(symbol);
        }
        return lastPrice;
    }
    
    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updatePrices();
            }
        };
        
        // 立即执行一次
        updatePrices();
        // 然后定时执行
        updateTask.runTaskTimerAsynchronously(plugin, updateInterval, updateInterval);
        
        MessageUtils.logInfo("价格服务已启动，更新间隔: " + (updateInterval * 50) + "ms");
    }
    
    @Override
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        running = false;
        MessageUtils.logInfo("价格服务已停止");
    }
    
    @Override
    public boolean isSymbolSupported(String symbol) {
        // 简单检查：如果已经获取过价格，说明支持
        return markPrices.containsKey(symbol) || lastPrices.containsKey(symbol);
    }
    
    /**
     * 更新价格（异步执行）
     * 批量获取所有币种价格，减少API调用次数
     */
    private void updatePrices() {
        if (symbolsToUpdate.isEmpty()) {
            return;
        }
        
        // 收集所有需要获取的币种ID
        StringBuilder coinIds = new StringBuilder();
        ConcurrentMap<String, String> coinIdToSymbol = new ConcurrentHashMap<>();
        
        for (String symbol : symbolsToUpdate.keySet()) {
            String coinId = symbolToCoinId.get(symbol);
            if (coinId != null) {
                if (coinIds.length() > 0) {
                    coinIds.append(",");
                }
                coinIds.append(coinId);
                coinIdToSymbol.put(coinId, symbol);
            }
        }
        
        if (coinIds.length() == 0) {
            return;
        }
        
        // 批量获取价格
        Map<String, BigDecimal> prices = fetchPricesFromCoinGecko(coinIds.toString());
        
        // 更新价格
        for (Map.Entry<String, BigDecimal> entry : prices.entrySet()) {
            String coinId = entry.getKey();
            BigDecimal price = entry.getValue();
            String symbol = coinIdToSymbol.get(coinId);
            
            if (symbol != null && price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                markPrices.put(symbol, price);
                lastPrices.put(symbol, price);
                // 清除错误记录
                lastErrorTime.remove(symbol);
            }
        }
    }
    
    /**
     * 从CoinGecko API批量获取价格
     * API格式: https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd
     * 返回格式: {"bitcoin":{"usd":88244},"ethereum":{"usd":2456}}
     */
    private Map<String, BigDecimal> fetchPricesFromCoinGecko(String coinIds) {
        Map<String, BigDecimal> result = new ConcurrentHashMap<>();
        String urlStr = exchangeApiUrl + "/simple/price?ids=" + coinIds + "&vs_currencies=usd";
        
        // 添加调试日志
        long now = System.currentTimeMillis();
        Long lastDebug = lastErrorTime.get("_debug");
        if (lastDebug == null || now - lastDebug > 300000) { // 每5分钟记录一次调试信息
            MessageUtils.logInfo("正在获取价格，URL: " + urlStr);
            lastErrorTime.put("_debug", now);
        }
        
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            // 使用 SOCKS 代理
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 21881));
            conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000); // 增加到15秒
            conn.setReadTimeout(15000); // 增加到15秒
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    // 解析JSON: {"bitcoin":{"usd":88244},"ethereum":{"usd":2456}}
                    String json = response.toString();
                    String[] coinIdArray = coinIds.split(",");
                    
                    for (String coinId : coinIdArray) {
                        coinId = coinId.trim();
                        // 查找该币种的usd价格
                        String coinKey = "\"" + coinId + "\":";
                        int coinIndex = json.indexOf(coinKey);
                        if (coinIndex > 0) {
                            // 在该币种对象中查找usd价格
                            int usdKeyIndex = json.indexOf("\"usd\":", coinIndex);
                            if (usdKeyIndex > coinIndex) {
                                int priceStart = usdKeyIndex + 6; // "usd": 的长度
                                // 跳过可能的空格
                                while (priceStart < json.length() && Character.isWhitespace(json.charAt(priceStart))) {
                                    priceStart++;
                                }
                                // 找到数字结束位置（逗号或右大括号）
                                int priceEnd = priceStart;
                                while (priceEnd < json.length() && 
                                       (Character.isDigit(json.charAt(priceEnd)) || json.charAt(priceEnd) == '.')) {
                                    priceEnd++;
                                }
                                if (priceEnd > priceStart) {
                                    String priceStr = json.substring(priceStart, priceEnd);
                                    try {
                                        BigDecimal price = new BigDecimal(priceStr);
                                        result.put(coinId, price);
                                    } catch (NumberFormatException e) {
                                        // 忽略解析错误
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 读取错误响应
                String errorResponse = "";
                try {
                    if (conn.getErrorStream() != null) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                            StringBuilder error = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                error.append(line);
                            }
                            errorResponse = error.toString();
                        }
                    }
                } catch (Exception ignored) {}
                
                // 只在第一次或间隔较长时间后记录错误
                long errorNow = System.currentTimeMillis();
                Long lastError = lastErrorTime.get("_api_error");
                if (lastError == null || errorNow - lastError > 60000) { // 每分钟最多记录一次
                    MessageUtils.logWarning("CoinGecko API 返回错误代码: " + responseCode);
                    if (!errorResponse.isEmpty()) {
                        MessageUtils.logWarning("错误响应: " + errorResponse);
                    }
                    lastErrorTime.put("_api_error", errorNow);
                }
            }
        } catch (java.net.SocketTimeoutException e) {
            // 连接超时
            long timeoutNow = System.currentTimeMillis();
            Long lastError = lastErrorTime.get("_timeout");
            if (lastError == null || timeoutNow - lastError > 300000) { // 每5分钟最多记录一次
                MessageUtils.logWarning("CoinGecko API 连接超时，URL: " + urlStr);
                MessageUtils.logWarning("超时详情: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                lastErrorTime.put("_timeout", timeoutNow);
            }
        } catch (java.net.ConnectException e) {
            // 连接失败
            long connectNow = System.currentTimeMillis();
            Long lastError = lastErrorTime.get("_connect");
            if (lastError == null || connectNow - lastError > 300000) { // 每5分钟最多记录一次
                MessageUtils.logWarning("CoinGecko API 连接失败，URL: " + urlStr);
                MessageUtils.logWarning("连接错误: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                MessageUtils.logWarning("请检查: 1) 网络连接 2) 防火墙设置 3) 代理配置");
                lastErrorTime.put("_connect", connectNow);
            }
        } catch (java.net.UnknownHostException e) {
            // DNS解析失败
            long dnsNow = System.currentTimeMillis();
            Long lastError = lastErrorTime.get("_dns");
            if (lastError == null || dnsNow - lastError > 300000) {
                MessageUtils.logWarning("无法解析域名: " + e.getMessage());
                MessageUtils.logWarning("请检查DNS设置或网络连接");
                lastErrorTime.put("_dns", dnsNow);
            }
        } catch (javax.net.ssl.SSLException e) {
            MessageUtils.logWarning("SSL/TLS 连接错误:");
            e.printStackTrace();
        } catch (Exception e) {
            // 其他错误，输出详细信息用于调试
            long exceptionNow = System.currentTimeMillis();
            Long lastError = lastErrorTime.get("_error");
            if (lastError == null || exceptionNow - lastError > 60000) { // 每分钟最多记录一次
                MessageUtils.logWarning("获取价格失败: " + e.getClass().getSimpleName());
                MessageUtils.logWarning("错误信息: " + e.getMessage());
                MessageUtils.logWarning("URL: " + urlStr);
                if (e.getCause() != null) {
                    MessageUtils.logWarning("原因: " + e.getCause().getMessage());
                }
                // 打印堆栈跟踪的前几行用于调试
                StackTraceElement[] stack = e.getStackTrace();
                if (stack.length > 0) {
                    MessageUtils.logWarning("位置: " + stack[0].toString());
                }
                lastErrorTime.put("_error", exceptionNow);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        
        return result;
    }
    
    /**
     * 添加交易对到CoinGecko币种ID的映射
     */
    public void addSymbolMapping(String symbol, String coinId) {
        symbolToCoinId.put(symbol, coinId);
    }
    
    /**
     * 手动更新指定交易对的价格（用于测试或手动设置）
     */
    public void updatePrice(String symbol, BigDecimal markPrice, BigDecimal lastPrice) {
        if (markPrice != null) {
            markPrices.put(symbol, markPrice);
        }
        if (lastPrice != null) {
            lastPrices.put(symbol, lastPrice);
        }
    }
}

