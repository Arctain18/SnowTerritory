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
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}

