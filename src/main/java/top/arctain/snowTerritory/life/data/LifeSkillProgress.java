package top.arctain.snowTerritory.life.data;

public record LifeSkillProgress(
        LifeSkillType skillType,
        int level,
        int currentExp,
        int neededExp,
        int totalExp,
        double progressRatio
) {
}
