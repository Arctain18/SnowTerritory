package top.arctain.snowTerritory.quest.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SQLite任务数据库访问实现
 */
public class SqliteQuestDatabaseDao implements QuestDatabaseDao {

    private final HikariDataSource dataSource;
    private static final int DEFAULT_MAX_MATERIAL_LEVEL = 1;

    public SqliteQuestDatabaseDao(Main plugin, File dbFile) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("ST-Quest");
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void init() {
        try (Connection conn = dataSource.getConnection()) {
            // 创建玩家等级上限表
            try (PreparedStatement ps = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS st_quest_players (
                        player_uuid CHAR(36) PRIMARY KEY,
                        max_material_level INTEGER NOT NULL DEFAULT 1
                    );
                    """)) {
                ps.execute();
            }
            
            // 创建完成任务历史表
            try (PreparedStatement ps = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS st_quest_completed (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid CHAR(36) NOT NULL,
                        quest_level INTEGER NOT NULL,
                        quest_type VARCHAR(20) NOT NULL,
                        quest_release_method VARCHAR(20) NOT NULL,
                        material_key VARCHAR(128) NOT NULL,
                        completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                    """)) {
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS st_quest_daily_usage (
                        player_uuid CHAR(36) NOT NULL,
                        day_key VARCHAR(16) NOT NULL,
                        remote_claim_used INTEGER NOT NULL DEFAULT 0,
                        free_claim_used INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, day_key)
                    );
                    """)) {
                ps.execute();
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS st_quest_instances (
                        quest_uuid TEXT PRIMARY KEY,
                        owner_player_uuid TEXT,
                        quest_type TEXT NOT NULL,
                        release_method TEXT NOT NULL,
                        material_key TEXT NOT NULL,
                        material_name TEXT NOT NULL,
                        required_amount INTEGER NOT NULL,
                        current_amount INTEGER NOT NULL,
                        start_time_ms INTEGER NOT NULL,
                        time_limit_ms INTEGER NOT NULL,
                        quest_level INTEGER NOT NULL,
                        difficulty INTEGER NOT NULL,
                        status TEXT NOT NULL
                    );
                    """)) {
                ps.execute();
            }
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE INDEX IF NOT EXISTS idx_st_quest_instances_owner ON st_quest_instances(owner_player_uuid);");
            }
        } catch (SQLException e) {
            MessageUtils.logError("初始化任务数据库表失败: " + e.getMessage());
        }
    }

    @Override
    public int getMaxMaterialLevel(UUID playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT max_material_level FROM st_quest_players WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_material_level");
                }
            }
        } catch (SQLException e) {
            MessageUtils.logError("获取玩家等级上限失败: " + e.getMessage());
        }
        return DEFAULT_MAX_MATERIAL_LEVEL;
    }

    @Override
    public void setMaxMaterialLevel(UUID playerId, int level) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO st_quest_players (player_uuid, max_material_level)
                     VALUES (?, ?)
                     ON CONFLICT(player_uuid) DO UPDATE SET max_material_level = ?
                     """)) {
            ps.setString(1, playerId.toString());
            ps.setInt(2, level);
            ps.setInt(3, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            MessageUtils.logError("设置玩家等级上限失败: " + e.getMessage());
        }
    }

    @Override
    public void recordCompletedQuest(UUID playerId, Quest quest) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO st_quest_completed 
                     (player_uuid, quest_level, quest_type, quest_release_method, material_key)
                     VALUES (?, ?, ?, ?, ?)
                     """)) {
            ps.setString(1, playerId.toString());
            ps.setInt(2, quest.getLevel());
            ps.setString(3, quest.getType().name());
            ps.setString(4, quest.getReleaseMethod().name());
            ps.setString(5, quest.getMaterialKey());
            ps.executeUpdate();
        } catch (SQLException e) {
            MessageUtils.logError("记录完成任务失败: " + e.getMessage());
        }
    }

    @Override
    public DailyUsage getDailyUsage(UUID playerId, String dayKey) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT remote_claim_used, free_claim_used FROM st_quest_daily_usage WHERE player_uuid = ? AND day_key = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, dayKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DailyUsage(rs.getInt("remote_claim_used"), rs.getInt("free_claim_used"));
                }
            }
        } catch (SQLException e) {
            MessageUtils.logError("读取每日用量失败: " + e.getMessage());
        }
        return new DailyUsage(0, 0);
    }

    @Override
    public void saveDailyUsage(UUID playerId, String dayKey, int remoteClaimUsed, int freeClaimUsed) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO st_quest_daily_usage (player_uuid, day_key, remote_claim_used, free_claim_used)
                     VALUES (?, ?, ?, ?)
                     ON CONFLICT(player_uuid, day_key) DO UPDATE SET remote_claim_used = ?, free_claim_used = ?
                     """)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, dayKey);
            ps.setInt(3, Math.max(0, remoteClaimUsed));
            ps.setInt(4, Math.max(0, freeClaimUsed));
            ps.setInt(5, Math.max(0, remoteClaimUsed));
            ps.setInt(6, Math.max(0, freeClaimUsed));
            ps.executeUpdate();
        } catch (SQLException e) {
            MessageUtils.logError("写入每日用量失败: " + e.getMessage());
        }
    }

    @Override
    public void upsertQuestInstance(Quest quest) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO st_quest_instances (
                         quest_uuid, owner_player_uuid, quest_type, release_method, material_key, material_name,
                         required_amount, current_amount, start_time_ms, time_limit_ms, quest_level, difficulty, status
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     ON CONFLICT(quest_uuid) DO UPDATE SET
                         owner_player_uuid = excluded.owner_player_uuid,
                         quest_type = excluded.quest_type,
                         release_method = excluded.release_method,
                         material_key = excluded.material_key,
                         material_name = excluded.material_name,
                         required_amount = excluded.required_amount,
                         current_amount = excluded.current_amount,
                         start_time_ms = excluded.start_time_ms,
                         time_limit_ms = excluded.time_limit_ms,
                         quest_level = excluded.quest_level,
                         difficulty = excluded.difficulty,
                         status = excluded.status
                     """)) {
            bindQuest(ps, quest);
            ps.executeUpdate();
        } catch (SQLException e) {
            MessageUtils.logError("写入任务实例失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteQuestInstance(UUID questId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM st_quest_instances WHERE quest_uuid = ?")) {
            ps.setString(1, questId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            MessageUtils.logError("删除任务实例失败: " + e.getMessage());
        }
    }

    @Override
    public void replaceBountyQuestInstances(List<Quest> bountyQuests) {
        try (Connection conn = dataSource.getConnection()) {
            boolean ac = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM st_quest_instances WHERE owner_player_uuid IS NULL")) {
                    del.executeUpdate();
                }
                if (bountyQuests != null && !bountyQuests.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement("""
                            INSERT INTO st_quest_instances (
                                quest_uuid, owner_player_uuid, quest_type, release_method, material_key, material_name,
                                required_amount, current_amount, start_time_ms, time_limit_ms, quest_level, difficulty, status
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """)) {
                        for (Quest q : bountyQuests) {
                            bindQuest(ps, q);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                MessageUtils.logError("同步悬赏任务到数据库失败: " + e.getMessage());
            } finally {
                conn.setAutoCommit(ac);
            }
        } catch (SQLException e) {
            MessageUtils.logError("同步悬赏任务到数据库失败: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, List<Quest>> loadPlayerQuestsGrouped() {
        Map<UUID, List<Quest>> map = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM st_quest_instances WHERE owner_player_uuid IS NOT NULL")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Quest q = readQuest(rs);
                    if (q == null || q.getPlayerId() == null) {
                        continue;
                    }
                    map.computeIfAbsent(q.getPlayerId(), k -> new ArrayList<>()).add(q);
                }
            }
        } catch (SQLException e) {
            MessageUtils.logError("加载玩家任务失败: " + e.getMessage());
        }
        return map;
    }

    @Override
    public List<Quest> loadBountyQuests() {
        List<Quest> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM st_quest_instances WHERE owner_player_uuid IS NULL ORDER BY start_time_ms ASC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Quest q = readQuest(rs);
                    if (q != null) {
                        list.add(q);
                    }
                }
            }
        } catch (SQLException e) {
            MessageUtils.logError("加载悬赏任务失败: " + e.getMessage());
        }
        return list;
    }

    private static void bindQuest(PreparedStatement ps, Quest quest) throws SQLException {
        ps.setString(1, quest.getQuestId().toString());
        if (quest.getPlayerId() == null) {
            ps.setNull(2, Types.VARCHAR);
        } else {
            ps.setString(2, quest.getPlayerId().toString());
        }
        ps.setString(3, quest.getType().name());
        ps.setString(4, quest.getReleaseMethod().name());
        ps.setString(5, quest.getMaterialKey());
        ps.setString(6, quest.getMaterialName());
        ps.setInt(7, quest.getRequiredAmount());
        ps.setInt(8, quest.getCurrentAmount());
        ps.setLong(9, quest.getStartTime());
        ps.setLong(10, quest.getTimeLimit());
        ps.setInt(11, quest.getLevel());
        ps.setInt(12, quest.getDifficulty());
        ps.setString(13, quest.getStatus().name());
    }

    private static Quest readQuest(ResultSet rs) throws SQLException {
        try {
            UUID questId = UUID.fromString(rs.getString("quest_uuid"));
            String ownerStr = rs.getString("owner_player_uuid");
            UUID playerId = ownerStr == null || ownerStr.isBlank() ? null : UUID.fromString(ownerStr);
            QuestType type = QuestType.valueOf(rs.getString("quest_type"));
            QuestReleaseMethod release = QuestReleaseMethod.valueOf(rs.getString("release_method"));
            String materialKey = rs.getString("material_key");
            String materialName = rs.getString("material_name");
            int required = rs.getInt("required_amount");
            int current = rs.getInt("current_amount");
            long start = rs.getLong("start_time_ms");
            long timeLimit = rs.getLong("time_limit_ms");
            int level = rs.getInt("quest_level");
            int difficulty = rs.getInt("difficulty");
            QuestStatus status = QuestStatus.valueOf(rs.getString("status"));
            return new Quest(questId, playerId, type, release, materialKey, materialName,
                    required, current, start, timeLimit, level, difficulty, status);
        } catch (Exception e) {
            MessageUtils.logWarning("跳过无效任务行: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}

