package top.arctain.snowTerritory.quest.data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 任务数据库访问接口
 */
public interface QuestDatabaseDao {

    record DailyUsage(int remoteClaimUsed, int freeClaimUsed) {
    }
    
    /**
     * 初始化数据库表结构
     */
    void init();
    
    /**
     * 获取玩家材料任务等级上限
     * @param playerId 玩家UUID
     * @return 等级上限，默认返回1
     */
    int getMaxMaterialLevel(UUID playerId);
    
    /**
     * 设置玩家材料任务等级上限
     * @param playerId 玩家UUID
     * @param level 等级上限
     */
    void setMaxMaterialLevel(UUID playerId, int level);
    
    /**
     * 记录完成的任务
     * @param playerId 玩家UUID
     * @param quest 完成的任务
     */
    void recordCompletedQuest(UUID playerId, Quest quest);

    /**
     * 读取指定日期的每日用量。
     */
    DailyUsage getDailyUsage(UUID playerId, String dayKey);

    /**
     * 覆盖写入指定日期的每日用量。
     */
    void saveDailyUsage(UUID playerId, String dayKey, int remoteClaimUsed, int freeClaimUsed);

    /**
     * 写入或更新一条任务实例（普通任务 owner 非空；悬赏 owner 为空）。
     */
    void upsertQuestInstance(Quest quest);

    /**
     * 删除一条任务实例（quest_uuid）。
     */
    void deleteQuestInstance(UUID questId);

    /**
     * 用内存中的列表覆盖数据库中的悬赏任务（owner 为 NULL 的行）。
     */
    void replaceBountyQuestInstances(List<Quest> bountyQuests);

    /**
     * 加载所有玩家个人任务，按玩家 UUID 分组。
     */
    Map<UUID, List<Quest>> loadPlayerQuestsGrouped();

    /**
     * 加载悬赏任务列表（全局）。
     */
    List<Quest> loadBountyQuests();
    
    /**
     * 关闭数据库连接
     */
    void close();
}

