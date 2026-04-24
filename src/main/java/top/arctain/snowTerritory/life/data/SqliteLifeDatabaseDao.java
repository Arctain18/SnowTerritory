package top.arctain.snowTerritory.life.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class SqliteLifeDatabaseDao implements LifeDatabaseDao {

    private final HikariDataSource dataSource;

    public SqliteLifeDatabaseDao(Main plugin, File dbFile) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("ST-Life");
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void init() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     CREATE TABLE IF NOT EXISTS st_life_player_skill (
                         player_uuid CHAR(36) NOT NULL,
                         skill_type VARCHAR(32) NOT NULL,
                         exp INTEGER NOT NULL DEFAULT 0,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (player_uuid, skill_type)
                     );
                     """)) {
            ps.execute();
        } catch (SQLException e) {
            MessageUtils.logError("初始化 life 数据库失败: " + e.getMessage());
        }
    }

    @Override
    public int getExperience(UUID playerId, LifeSkillType skillType) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT exp FROM st_life_player_skill WHERE player_uuid = ? AND skill_type = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, skillType.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getInt("exp"));
                }
            }
        } catch (SQLException e) {
            MessageUtils.logError("读取生活经验失败: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public Map<LifeSkillType, Integer> getAllExperience(UUID playerId) {
        Map<LifeSkillType, Integer> result = new EnumMap<>(LifeSkillType.class);
        for (LifeSkillType skillType : LifeSkillType.values()) {
            result.put(skillType, 0);
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT skill_type, exp FROM st_life_player_skill WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String skillName = rs.getString("skill_type");
                    int exp = Math.max(0, rs.getInt("exp"));
                    try {
                        LifeSkillType skillType = LifeSkillType.valueOf(skillName);
                        result.put(skillType, exp);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            MessageUtils.logError("读取全部生活经验失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void setExperience(UUID playerId, LifeSkillType skillType, int exp) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO st_life_player_skill (player_uuid, skill_type, exp, updated_at)
                     VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                     ON CONFLICT(player_uuid, skill_type) DO UPDATE SET
                     exp = excluded.exp,
                     updated_at = CURRENT_TIMESTAMP
                     """)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, skillType.name());
            ps.setInt(3, Math.max(0, exp));
            ps.executeUpdate();
        } catch (SQLException e) {
            MessageUtils.logError("写入生活经验失败: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
