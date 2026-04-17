package top.arctain.snowTerritory.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;

import java.util.Locale;

/** CoinsEngine 通用货币工具。 */
public final class CoinsEngineUtils {

    private CoinsEngineUtils() {
    }

    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().getPlugin("CoinsEngine") != null;
    }

    public static boolean hasCurrency(String currencyId) {
        String id = normalizeCurrencyId(currencyId);
        return isPluginEnabled() && !id.isEmpty() && CoinsEngineAPI.hasCurrency(id);
    }

    public static double getBalance(Player player, String currencyId) {
        String id = normalizeCurrencyId(currencyId);
        if (player == null || !hasCurrency(id)) {
            return 0.0;
        }
        return CoinsEngineAPI.getBalance(player.getUniqueId(), id);
    }

    public static boolean removeBalance(Player player, String currencyId, double amount) {
        String id = normalizeCurrencyId(currencyId);
        if (player == null || !hasCurrency(id) || amount < 0) {
            return false;
        }
        return CoinsEngineAPI.removeBalance(player.getUniqueId(), id, amount);
    }

    /** 预留给其它模块（例如 starember）直接加钱。 */
    public static boolean addBalance(Player player, String currencyId, double amount) {
        String id = normalizeCurrencyId(currencyId);
        if (player == null || !hasCurrency(id) || amount < 0) {
            return false;
        }
        return CoinsEngineAPI.addBalance(player.getUniqueId(), id, amount);
    }

    private static String normalizeCurrencyId(String currencyId) {
        return currencyId == null ? "" : currencyId.trim().toLowerCase(Locale.ROOT);
    }
}
