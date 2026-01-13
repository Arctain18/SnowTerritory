package top.arctain.snowTerritory.reinforce.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * 负责与 Vault 经济系统交互的服务类（通过反射避免硬依赖）
 */
public class EconomyService {

    private final Object economy; // Vault Economy 提供者

    public EconomyService() {
        this.economy = initVaultEconomy();
    }

    private Object initVaultEconomy() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            var economyReg = Bukkit.getServer().getServicesManager().getRegistration(economyClass);
            return economyReg != null ? economyReg.getProvider() : null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public double getBalance(Player player) {
        if (economy == null) return 0;
        try {
            Method getBalanceMethod = economy.getClass().getMethod("getBalance", Player.class);
            Object result = getBalanceMethod.invoke(economy, player);
            return result instanceof Number ? ((Number) result).doubleValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void withdraw(Player player, double amount) {
        if (economy == null) return;
        try {
            Method withdrawMethod = economy.getClass().getMethod("withdrawPlayer", Player.class, double.class);
            withdrawMethod.invoke(economy, player, amount);
        } catch (Exception e) {
            // 忽略错误
        }
    }
}

