package top.arctain.snowTerritory.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import top.arctain.snowTerritory.config.PluginConfig;

import java.util.logging.Level;

/**
 * 统一的消息工具类
 * 用于美化所有控制台和聊天框输出
 */
public class MessageUtils {

    private static JavaPlugin plugin;
    private static PluginConfig config;
    private static final String CONSOLE_PREFIX = "[SnowTerritory] ";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static boolean consoleBoldEnabled = true;

    /**
     * 初始化消息工具类
     */
    public static void initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    /**
     * 设置配置（用于读取消息）
     */
    public static void setConfig(PluginConfig pluginConfig) {
        config = pluginConfig;
    }
    
    /**
     * 获取消息前缀
     */
    private static String getPrefix() {
        if (config != null) {
            return config.getMessagePrefix();
        }
        return "&8[&6SnowTerritory&8]&r ";
    }
    
    /**
     * 获取配置的消息，如果不存在则使用默认值
     */
    private static String getMessage(String key, String defaultValue, String... placeholders) {
        String message = defaultValue;
        if (config != null) {
            String configMessage = config.getMessage(key);
            // 如果配置中有消息且不是空字符串，且不等于key（说明找到了配置），则使用配置的消息
            if (configMessage != null && !configMessage.isEmpty() && !configMessage.equals(key) && !configMessage.equals("messages." + key)) {
                message = configMessage;
            }
        }
        
        // 替换占位符
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }
    
    /**
     * 发送配置消息（从配置文件读取，带前缀）
     */
    public static void sendConfigMessage(CommandSender sender, String key, String defaultValue, String... placeholders) {
        String message = getMessage(key, defaultValue, placeholders);
        sendMessage(sender, message);
    }
    
    /**
     * 发送配置消息（不带前缀）
     */
    public static void sendConfigRaw(CommandSender sender, String key, String defaultValue, String... placeholders) {
        String message = getMessage(key, defaultValue, placeholders);
        sendRaw(sender, message);
    }

    /**
     * 发送成功消息给玩家（使用配置）
     */
    public static void sendSuccess(CommandSender sender, String key, String defaultValue, String... placeholders) {
        sendConfigMessage(sender, key, defaultValue, placeholders);
    }

    /**
     * 发送错误消息给玩家（使用配置）
     */
    public static void sendError(CommandSender sender, String key, String defaultValue, String... placeholders) {
        sendConfigMessage(sender, key, defaultValue, placeholders);
    }

    /**
     * 发送警告消息给玩家（使用配置）
     */
    public static void sendWarning(CommandSender sender, String key, String defaultValue, String... placeholders) {
        sendConfigMessage(sender, key, defaultValue, placeholders);
    }

    /**
     * 发送信息消息给玩家（使用配置）
     */
    public static void sendInfo(CommandSender sender, String key, String defaultValue, String... placeholders) {
        sendConfigMessage(sender, key, defaultValue, placeholders);
    }

    /**
     * 发送普通消息给玩家（带前缀）
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(colorize(getPrefix() + message));
        }
    }

    /**
     * 发送不带前缀的消息
     */
    public static void sendRaw(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(colorize(message));
        }
    }

    /**
     * 发送分隔线
     */
    public static void sendSeparator(CommandSender sender) {
        String separator = getMessage("separator", "&8&m                                        ");
        sendRaw(sender, separator);
    }

    /**
     * 发送标题（带分隔线）
     */
    public static void sendTitle(CommandSender sender, String title) {
        sendSeparator(sender);
        sendRaw(sender, "&6&l" + title);
        sendSeparator(sender);
    }
    
    /**
     * 发送标题（从配置读取）
     */
    public static void sendTitle(CommandSender sender, String key, String defaultTitle) {
        sendSeparator(sender);
        String title = getMessage(key, defaultTitle);
        sendRaw(sender, "&6&l" + title);
        sendSeparator(sender);
    }

    /**
     * 发送帮助信息格式
     */
    public static void sendHelpLine(CommandSender sender, String command, String description) {
        sendRaw(sender, "&e" + command + " &7- &f" + description);
    }
    
    /**
     * 发送帮助信息（从配置读取）
     */
    public static void sendHelp(CommandSender sender) {
        String title = getMessage("help.title", "SnowTerritory 命令帮助");
        sendTitle(sender, title);
        sendConfigRaw(sender, "help.reinforce", "&e/snowterritory reinforce &7- &f打开物品强化界面");
        sendConfigRaw(sender, "help.reload", "&e/snowterritory reload &7- &f重载插件配置");
        sendConfigRaw(sender, "help.checkid", "&e/snowterritory checkID &7- &f查看手中物品的MMOItems ID");
        sendSeparator(sender);
    }

    // ========== 控制台日志方法 ==========

    /**
     * 控制台：信息日志（加粗）
     */
    public static void logInfo(String message) {
        if (plugin != null) {
            plugin.getLogger().info(formatConsoleMessage(message));
        }
    }

    /**
     * 控制台：警告日志（加粗）
     */
    public static void logWarning(String message) {
        if (plugin != null) {
            plugin.getLogger().warning(formatConsoleMessage(message));
        }
    }

    /**
     * 控制台：严重错误日志（加粗）
     */
    public static void logSevere(String message) {
        if (plugin != null) {
            plugin.getLogger().severe(formatConsoleMessage(message));
        }
    }

    /**
     * 控制台：调试日志（加粗）
     */
    public static void logDebug(String message) {
        if (plugin != null) {
            plugin.getLogger().log(Level.FINE, formatConsoleMessage(message));
        }
    }
    
    /**
     * 格式化控制台消息（添加加粗效果）
     */
    private static String formatConsoleMessage(String message) {
        if (consoleBoldEnabled) {
            return ANSI_BOLD + CONSOLE_PREFIX + message + ANSI_RESET;
        }
        return CONSOLE_PREFIX + message;
    }

    /**
     * 控制台：成功消息（绿色）
     */
    public static void logSuccess(String message) {
        logInfo("✓ " + message);
    }

    /**
     * 控制台：错误消息（红色）
     */
    public static void logError(String message) {
        logSevere("✗ " + message);
    }

    // ========== 特殊格式消息 ==========

    /**
     * 发送强化成功消息
     */
    public static void sendReinforceSuccess(Player player, int newLevel) {
        sendSeparator(player);
        sendConfigRaw(player, "reinforce.success-title", "&a&l✓ 强化成功！");
        String levelMsg = getMessage("reinforce.success-level", "&7等级提升至: &a&l+{level}", "level", String.valueOf(newLevel));
        sendRaw(player, levelMsg);
        sendSeparator(player);
    }

    /**
     * 发送强化失败消息
     */
    public static void sendReinforceFail(Player player, int newLevel) {
        sendSeparator(player);
        sendConfigRaw(player, "reinforce.fail-title", "&c&l✗ 强化失败！");
        String levelMsg = getMessage("reinforce.fail-level", "&7等级下降至: &c&l+{level}", "level", String.valueOf(newLevel));
        sendRaw(player, levelMsg);
        sendSeparator(player);
    }

    /**
     * 发送强化维持消息
     */
    public static void sendReinforceMaintain(Player player) {
        sendSeparator(player);
        sendConfigRaw(player, "reinforce.maintain-title", "&e&l⚠ 强化维持不变");
        sendConfigRaw(player, "reinforce.maintain-desc", "&7物品等级未发生变化");
        sendSeparator(player);
    }

    /**
     * 发送物品信息（查看ID命令）
     */
    public static void sendItemInfo(Player player, String type, String id) {
        String title = getMessage("item.info-title", "物品信息");
        sendRaw(player, title);
        sendSeparator(player);
        String typeMsg = getMessage("item.info-type", "&6物品类型: &f{type}", "type", type);
        sendRaw(player, typeMsg);
        String idMsg = getMessage("item.info-id", "&6物品 ID: &e&l{id}", "id", id);
        sendRaw(player, idMsg);
        sendSeparator(player);
    }

    /**
     * 发送启动横幅（加粗）
     */
    public static void sendStartupBanner(JavaPlugin plugin) {
        if (plugin == null) return;
        String bold = consoleBoldEnabled ? ANSI_BOLD : "";
        String reset = consoleBoldEnabled ? ANSI_RESET : "";
        plugin.getLogger().info(bold + "╔══════════════════════════════════════╗" + reset);
        plugin.getLogger().info(bold + "      SnowTerritory 插件已启用          " + reset);
        plugin.getLogger().info(bold + "      版本: " + plugin.getPluginMeta().getVersion() + "                    " + reset);
        plugin.getLogger().info(bold + "      作者: " + String.join(", ", plugin.getPluginMeta().getAuthors()) + "              " + reset);
        plugin.getLogger().info(bold + "╚══════════════════════════════════════╝" + reset);
    }

    /**
     * 发送关闭横幅（加粗）
     */
    public static void sendShutdownBanner(JavaPlugin plugin) {
        if (plugin == null) return;
        String bold = consoleBoldEnabled ? ANSI_BOLD : "";
        String reset = consoleBoldEnabled ? ANSI_RESET : "";
        plugin.getLogger().info(bold + "╔══════════════════════════════════════╗" + reset);
        plugin.getLogger().info(bold + "║     SnowTerritory 插件已禁用          ║" + reset);
        plugin.getLogger().info(bold + "╚══════════════════════════════════════╝" + reset);
    }

    // ========== 工具方法 ==========

    /**
     * 转换颜色代码（支持传统颜色代码和16进制颜色）
     * 
     * 支持格式：
     * - 传统颜色代码: &a (绿色), &l (粗体), &r (重置) 等
     * - 16进制颜色: &{#FFFFFF} 或 <#FFFFFF>
     * 
     * 示例：
     * - "&aHello &{#FF0000}World" → 绿色Hello + 红色World
     * - "<#00FF00>Green Text" → 绿色文本
     * 
     * @param text 包含颜色代码的文本
     * @return 转换后的文本（可直接发送给玩家）
     */
    public static String colorize(String text) {
        if (text == null) return "";
        
        // 先处理16进制颜色格式（转换为Minecraft原生格式）
        text = processHexColors(text);
        
        // 然后处理传统颜色代码（&a, &l 等）
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * 处理16进制颜色格式
     * 
     * 支持的格式：
     * - &{#FFFFFF} → 转换为 &x&F&F&F&F&F&F
     * - <#FFFFFF> → 转换为 &x&F&F&F&F&F&F
     * 
     * Minecraft 1.16+ 使用 &x&R&R&G&G&B&B 格式表示16进制颜色
     * 其中每个字符前都需要 & 符号
     * 
     * @param text 包含16进制颜色代码的文本
     * @return 转换后的文本
     */
    private static String processHexColors(String text) {
        if (text == null || text.isEmpty()) return text;
        
        // 处理 &{#RRGGBB} 格式
        // 示例: &{#FF0000} → &x&F&F&0&0&0&0 (红色)
        text = text.replaceAll("&\\{#([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])\\}", 
            "&x&$1&$2&$3&$4&$5&$6");
        
        // 处理 <#RRGGBB> 格式
        // 示例: <#00FF00> → &x&0&0&F&F&0&0 (绿色)
        text = text.replaceAll("<#([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])>", 
            "&x&$1&$2&$3&$4&$5&$6");
        
        return text;
    }

    /**
     * 移除颜色代码
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(text);
    }

    /**
     * 格式化数字（带颜色）
     */
    public static String formatNumber(int number) {
        return "&e" + number;
    }

    /**
     * 格式化数字（带颜色）
     */
    public static String formatNumber(double number) {
        return "&e" + number;
    }
}

