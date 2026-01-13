package top.arctain.snowTerritory.reinforce.service;

import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import top.arctain.snowTerritory.utils.MessageUtils;

/**
 * MMOCore集成服务
 * 提供获取玩家职业和等级的方法
 * 使用硬依赖，如果MMOCore不可用则禁用功能
 */
public class MMOCoreService {

    private final boolean enabled;

    public MMOCoreService() {
        Plugin mmocore = Bukkit.getPluginManager().getPlugin("MMOCore");
        this.enabled = mmocore != null && mmocore.isEnabled();
        
        if (!enabled) {
            MessageUtils.logWarning("MMOCore 未安装或未启用，相关功能将被禁用");
        }
    }

    /**
     * 检查MMOCore是否可用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取玩家职业名称
     * @param player 玩家
     * @return 职业名称，如果MMOCore不可用则返回null
     */
    public String getClassName(Player player) {
        if (!enabled) return null;
        try {
            PlayerData playerData = PlayerData.get(player);
            if (playerData == null) return null;
            return playerData.getProfess().getName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取玩家总等级
     * @param player 玩家
     * @return 玩家总等级，如果MMOCore不可用则返回0
     */
    public int getPlayerLevel(Player player) {
        if (!enabled) return 0;
        try {
            PlayerData playerData = PlayerData.get(player);
            if (playerData == null) return 0;
            return playerData.getLevel();
        } catch (Exception e) {
            return 0;
        }
    }
}
