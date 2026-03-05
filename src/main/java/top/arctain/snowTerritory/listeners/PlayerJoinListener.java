package top.arctain.snowTerritory.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 玩家登录监听器
 * 用于在管理员登录时发送欢迎消息
 */
public class PlayerJoinListener implements Listener {

    private final Main plugin;

    public PlayerJoinListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否有管理员权限（OP或特定权限）
        if (!player.isOp() && !player.hasPermission("mmoitemseditor.use")) {
            return;
        }

        // 延迟发送，确保玩家完全登录
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sendWelcomeMessage(player);
        }, 20L); // 延迟1秒
    }

    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(Player player) {
        // 获取插件版本
        String version = plugin.getPluginMeta().getVersion();
        
        // 获取jar文件修改日期
        String buildDate = getBuildDate();
        
        // 构建消息
        String separator = "<g:#FFFFFF:#4A95FF>═════════════════════════</g>";
        
        // 获取jar包大小
        String jarSize = getJarSize();
        
        // 内容部分
        String[] lines = {
            "",
            separator,
            "&{#99c2ff}  欢迎您使用 <g:#FFFFFF:#4A95FF>&l雪域插件</g> &{#ffffff}!",
            "&{#99c2ff}  当前版本: <g:#FFFFFF:#4A95FF>" + version + "</g>",
            "&{#99c2ff}  构建日期: <g:#FFFFFF:#4A95FF>" + buildDate + "</g>",
            "&{#99c2ff}  当前jar包大小: <g:#FFFFFF:#4A95FF>" + jarSize + "</g>",
            "&{#99c2ff}  插件作者: <g:#FFFFFF:#4A95FF>Arctain</g>",
            "&{#99c2ff}  作者QQ: <g:#FFFFFF:#4A95FF>1546025015</g>",
            separator,
            ""
        };
        
        // 发送消息（使用MessageUtils的colorize方法）
        for (String line : lines) {
            player.sendMessage(top.arctain.snowTerritory.utils.MessageUtils.colorize(line));
        }
    }

    /**
     * 获取jar文件构建日期
     */
    private String getBuildDate() {
        try {
            // 通过类文件路径获取jar文件
            java.net.URL url = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (url != null) {
                File jarFile = new File(url.toURI());
                if (jarFile.exists()) {
                    long lastModified = jarFile.lastModified();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return sdf.format(new Date(lastModified));
                }
            }
        } catch (Exception e) {
            top.arctain.snowTerritory.utils.MessageUtils.logDebug("获取构建日期失败: " + e.getMessage());
        }
        return "未知";
    }

    /** 获取jar文件大小（按KB计算，带千位分隔符） */
    private String getJarSize() {
        try {
            // 通过类文件路径获取jar文件
            java.net.URL url = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (url != null) {
                File jarFile = new File(url.toURI());
                if (jarFile.exists()) {
                    long sizeInBytes = jarFile.length();
                    long sizeInKB = sizeInBytes / 1024;
                    // 格式化数字，添加千位分隔符
                    return formatNumber(sizeInKB) + "KB";
                }
            }
        } catch (Exception e) {
            top.arctain.snowTerritory.utils.MessageUtils.logDebug("获取jar大小失败: " + e.getMessage());
        }
        return "未知";
    }

    private String formatNumber(long number) {
        return String.format("%,d", number);
    }
}

