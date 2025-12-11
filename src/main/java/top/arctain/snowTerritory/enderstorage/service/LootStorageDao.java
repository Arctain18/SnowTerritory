package top.arctain.snowTerritory.enderstorage.service;

import java.util.Map;
import java.util.UUID;

public interface LootStorageDao {
    void init();

    Map<String, Integer> loadAll(UUID playerId);

    void save(UUID playerId, Map<String, Integer> data);

    void close();
}

