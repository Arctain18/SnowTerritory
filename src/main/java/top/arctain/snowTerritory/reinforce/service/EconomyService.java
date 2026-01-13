package top.arctain.snowTerritory.reinforce.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * 负责与 Vault 经济系统交互的服务类
 */
public class EconomyService {

    private final Economy economy;

    public EconomyService() {
        this.economy = initVaultEconomy();
    }

    private Economy initVaultEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        return rsp != null ? rsp.getProvider() : null;
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public double getBalance(Player player) {
        if (economy == null) return 0;
        return economy.getBalance(player);
    }

    public void withdraw(Player player, double amount) {
        if (economy == null) return;
        economy.withdrawPlayer(player, amount);
    }
}

