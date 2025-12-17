package top.arctain.snowTerritory.quest.service;

import org.bukkit.entity.Player;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestType;

import java.util.List;
import java.util.UUID;

/**
 * 任务服务接口
 */
public interface QuestService {
    
    /**
     * 初始化服务
     */
    void initialize();
    
    /**
     * 关闭服务
     */
    void shutdown();
    
    /**
     * 重载配置
     */
    void reload();
    
    /**
     * 玩家接取普通任务
     */
    Quest acceptNormalQuest(Player player, QuestType type);
    
    /**
     * 获取玩家的所有活跃任务
     */
    List<Quest> getActiveQuests(UUID playerId);
    
    /**
     * 获取玩家的指定类型活跃任务
     */
    Quest getActiveQuest(UUID playerId, QuestType type);
    
    /**
     * 更新任务进度
     */
    boolean updateQuestProgress(UUID playerId, String materialKey, int amount);
    
    /**
     * 完成任务并发放奖励
     */
    boolean completeQuest(UUID playerId, UUID questId);
    
    /**
     * 获取所有活跃的悬赏任务
     */
    List<Quest> getActiveBountyQuests();
    
    /**
     * 完成悬赏任务并发放奖励
     */
    boolean completeBountyQuest(Player player, UUID questId);
    
    /**
     * 自动领取所有已完成的悬赏任务奖励
     * @return 领取的任务数量
     */
    int claimCompletedBountyQuests(Player player);
    
    /**
     * 开始悬赏任务发布调度
     */
    void startBountyScheduler();
    
    /**
     * 停止悬赏任务发布调度
     */
    void stopBountyScheduler();
}

