package top.arctain.snowTerritory.quest.service.generator;

import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestReleaseMethod;
import top.arctain.snowTerritory.quest.data.QuestType;

import java.util.UUID;

/**
 * 任务生成器接口
 * 负责根据类型和发布方式生成任务
 */
public interface QuestGenerator {
    
    /**
     * 生成任务（不限制难度范围时等价于全 1–32 档）
     */
    default Quest generate(UUID playerId, QuestType type, QuestReleaseMethod releaseMethod) {
        return generate(playerId, type, releaseMethod, QuestGenerationContext.unconstrained());
    }

    /**
     * @param context 接取普通任务时按 ST VIP 档位限制难度范围；悬赏任务在生成器内会忽略
     * @return 生成的任务，失败返回null
     */
    Quest generate(UUID playerId, QuestType type, QuestReleaseMethod releaseMethod, QuestGenerationContext context);
    
    /**
     * 是否支持指定任务类型
     */
    boolean supports(QuestType type);
}

