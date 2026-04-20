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

    record CompletionResult(int inventorySubmitted, int storageSubmitted, int completedNormal,
                            int claimedBounty, int boostedCompletions) {
        public boolean hasAnySuccess() {
            return completedNormal > 0 || claimedBounty > 0 || inventorySubmitted > 0 || storageSubmitted > 0;
        }
    }
    
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
     * 将任务当前状态写入数据库（监听器等在外部修改 {@link Quest#setStatus} 后调用）。
     */
    void persistQuestToDatabase(Quest quest);
    
    /**
     * 玩家接取普通任务
     */
    Quest acceptNormalQuest(Player player, QuestType type);
    
    /**
     * 获取玩家的所有活跃任务
     */
    List<Quest> getActiveQuests(UUID playerId);
    
    /**
     * 获取玩家的所有任务（包括所有状态：ACTIVE, COMPLETED, EXPIRED）
     */
    List<Quest> getAllQuests(UUID playerId);
    
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
     * 完成任务并按倍率发放奖励
     */
    boolean completeQuest(UUID playerId, UUID questId, double rewardMultiplier);
    
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
     * 自动领取所有已完成的悬赏任务奖励（支持倍率）
     */
    int claimCompletedBountyQuests(Player player, double rewardMultiplier);

    /**
     * 远程提交材料并领取奖励。
     */
    CompletionResult completeByCommand(Player player, boolean allowStorageSubmit, boolean freeMode, double rewardMultiplier);

    /**
     * 获取玩家今日已使用的远程提交次数。
     */
    int getDailyRemoteClaimUsed(UUID playerId);

    /**
     * 获取玩家今日已使用的免材料提交次数。
     */
    int getDailyFreeClaimUsed(UUID playerId);

    /**
     * 增加玩家今日远程提交次数。
     */
    void incrementDailyRemoteClaimUsed(UUID playerId);

    /**
     * 增加玩家今日免材料提交次数。
     */
    void incrementDailyFreeClaimUsed(UUID playerId);
    
    /**
     * 开始悬赏任务发布调度
     */
    void startBountyScheduler();
    
    /**
     * 停止悬赏任务发布调度
     */
    void stopBountyScheduler();

    /**
     * 检查任务是否激活且未过期
     */
    boolean isActiveAndNotExpired(Quest quest);
}

