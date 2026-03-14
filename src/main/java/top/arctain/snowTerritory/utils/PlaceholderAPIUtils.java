package top.arctain.snowTerritory.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.PlaceholderAPI;

/** PlaceholderAPI 占位符解析，PAPI 未安装时原样返回。 */
public final class PlaceholderAPIUtils {

    private static Boolean papiAvailable;

    /** 解析字符串中的 PAPI 占位符，需玩家上下文时传入 Player。PAPI 未安装则原样返回。 */
    public static String parse(OfflinePlayer player, String text) {
        if (text == null) return "";
        if (!isPapiAvailable()) return text;
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable ignored) {
            return text;
        }
    }

    private static boolean isPapiAvailable() {
        if (papiAvailable == null) {
            papiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        }
        return papiAvailable;
    }
}
