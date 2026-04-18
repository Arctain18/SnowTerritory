package top.arctain.snowTerritory.quest.service.generator;

/**
 * 接取/生成普通任务时使用的难度范围（1–32 与 {@link top.arctain.snowTerritory.quest.utils.QuestUtils} 一致）。
 */
public final class QuestGenerationContext {

    public static final QuestGenerationContext UNCONSTRAINED = new QuestGenerationContext(1, 32);
    public static final int MAX_DIFFICULTY = 32;

    private final int minDifficultyInclusive;
    private final int maxDifficultyInclusive;

    private QuestGenerationContext(int min, int max) {
        this.minDifficultyInclusive = min;
        this.maxDifficultyInclusive = max;
    }

    public static QuestGenerationContext unconstrained() {
        return UNCONSTRAINED;
    }

    /**
     * 根据 stvip 的 quest-min-difficulty-exclusive：可生成难度为大于该门槛的整数，即
     * [exclusiveFloor+1, 32] 闭区间与 1-32 求交（通常为 VIP3: 17-32，VIP2: 9-32）。
     */
    public static QuestGenerationContext forVipMinDifficultyExclusive(int exclusiveFloor) {
        if (exclusiveFloor <= 0) {
            return UNCONSTRAINED;
        }
        int low = Math.min(MAX_DIFFICULTY, exclusiveFloor + 1);
        return new QuestGenerationContext(low, MAX_DIFFICULTY);
    }

    public int getMinDifficultyInclusive() {
        return minDifficultyInclusive;
    }

    public int getMaxDifficultyInclusive() {
        return maxDifficultyInclusive;
    }
}
