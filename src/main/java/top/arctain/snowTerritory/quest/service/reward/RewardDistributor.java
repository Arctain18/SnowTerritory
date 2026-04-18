package top.arctain.snowTerritory.quest.service.reward;

import org.bukkit.entity.Player;
import top.arctain.snowTerritory.quest.data.Quest;

/**
 * 奖励分发接口
 * 负责任务完成后的奖励发放逻辑
 */
public interface RewardDistributor {
    
    /**
     * 向玩家分发任务奖励
     * 
     * @param player 目标玩家
     * @param quest 已完成的任务
     */
    void distribute(Player player, Quest quest);

    /**
     * 按倍率发放奖励（例如 VIP3 免材料领取 0.6 倍）。
     */
    void distribute(Player player, Quest quest, double rewardMultiplier);
}

