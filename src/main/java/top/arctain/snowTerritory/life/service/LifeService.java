package top.arctain.snowTerritory.life.service;

import org.bukkit.entity.Player;
import top.arctain.snowTerritory.life.data.LifeSkillProgress;
import top.arctain.snowTerritory.life.data.LifeSkillType;

import java.util.List;
import java.util.UUID;

public interface LifeService {

    int addExperience(UUID playerId, LifeSkillType skillType, int amount);

    LifeSkillProgress getProgress(UUID playerId, LifeSkillType skillType);

    List<LifeSkillProgress> getAllProgress(UUID playerId);

    int resolveQualityLevel(Player player, String cropType);

    void handleGatheringQuestProgress(Player player, String cropType);
}
