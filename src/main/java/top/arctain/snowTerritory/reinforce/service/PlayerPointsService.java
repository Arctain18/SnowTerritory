package top.arctain.snowTerritory.reinforce.service;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 负责与 PlayerPoints 插件交互的服务类（通过反射避免硬依赖）
 */
public class PlayerPointsService {

    private final Object playerPointsAPI;

    public PlayerPointsService() {
        this.playerPointsAPI = initPlayerPointsAPI();
    }

    private Object initPlayerPointsAPI() {
        try {
            Class<?> playerPointsClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Method getInstanceMethod = playerPointsClass.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);
            if (instance == null) return null;
            Method getAPIMethod = playerPointsClass.getMethod("getAPI");
            return getAPIMethod.invoke(instance);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isEnabled() {
        return playerPointsAPI != null;
    }

    public int getPoints(UUID uuid) {
        if (playerPointsAPI == null) return 0;
        try {
            Method lookMethod = playerPointsAPI.getClass().getMethod("look", UUID.class);
            Object result = lookMethod.invoke(playerPointsAPI, uuid);
            return result instanceof Number ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void takePoints(UUID uuid, int amount) {
        if (playerPointsAPI == null) return;
        try {
            Method takeMethod = playerPointsAPI.getClass().getMethod("take", UUID.class, int.class);
            takeMethod.invoke(playerPointsAPI, uuid, amount);
        } catch (Exception e) {
            // 忽略错误
        }
    }
}

