package top.arctain.snowTerritory.enderstorage.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SqliteLootStorageDao implements LootStorageDao {

    private final Main plugin;
    private final HikariDataSource dataSource;

    public SqliteLootStorageDao(Main plugin, File dbFile) {
        this.plugin = plugin;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("ST-EnderStorage");
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void init() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     CREATE TABLE IF NOT EXISTS st_enderstorage (
                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                         player_uuid CHAR(36) NOT NULL,
                         item_key VARCHAR(64) NOT NULL,
                         amount INTEGER NOT NULL,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE(player_uuid, item_key)
                     );
                     """)) {
            ps.execute();
        } catch (SQLException e) {
            MessageUtils.logError("初始化 enderstorage 表失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Integer> loadAll(UUID playerId) {
        Map<String, Integer> data = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT item_key, amount FROM st_enderstorage WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.put(rs.getString("item_key"), rs.getInt("amount"));
                }
            }
        } catch (SQLException e) {
            MessageUtils.logError("加载玩家仓库失败: " + e.getMessage());
        }
        return data;
    }

    @Override
    public void save(UUID playerId, Map<String, Integer> data) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM st_enderstorage WHERE player_uuid = ?")) {
                deletePs.setString(1, playerId.toString());
                deletePs.executeUpdate();
            }
            try (PreparedStatement insertPs = conn.prepareStatement("INSERT INTO st_enderstorage (player_uuid, item_key, amount) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Integer> entry : data.entrySet()) {
                    insertPs.setString(1, playerId.toString());
                    insertPs.setString(2, entry.getKey());
                    insertPs.setInt(3, entry.getValue());
                    insertPs.addBatch();
                }
                insertPs.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            MessageUtils.logError("保存玩家仓库失败: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}

