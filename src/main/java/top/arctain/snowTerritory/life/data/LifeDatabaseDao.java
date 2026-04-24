package top.arctain.snowTerritory.life.data;

import java.util.Map;
import java.util.UUID;

public interface LifeDatabaseDao {

    void init();

    int getExperience(UUID playerId, LifeSkillType skillType);

    Map<LifeSkillType, Integer> getAllExperience(UUID playerId);

    void setExperience(UUID playerId, LifeSkillType skillType, int exp);

    void close();
}
