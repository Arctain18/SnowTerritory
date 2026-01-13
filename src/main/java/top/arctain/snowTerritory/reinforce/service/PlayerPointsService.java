package top.arctain.snowTerritory.reinforce.service;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;

import java.util.UUID;

/**
 * 负责与 PlayerPoints 插件交互的服务类
 */
public class PlayerPointsService {

    private final PlayerPointsAPI playerPointsAPI;

    public PlayerPointsService() {
        this.playerPointsAPI = initPlayerPointsAPI();
    }

    private PlayerPointsAPI initPlayerPointsAPI() {
        PlayerPoints playerPoints = PlayerPoints.getInstance();
        return playerPoints != null ? playerPoints.getAPI() : null;
    }

    public boolean isEnabled() {
        return playerPointsAPI != null;
    }

    public int getPoints(UUID uuid) {
        if (playerPointsAPI == null) return 0;
        return playerPointsAPI.look(uuid);
    }

    public void takePoints(UUID uuid, int amount) {
        if (playerPointsAPI == null) return;
        playerPointsAPI.take(uuid, amount);
    }
}

