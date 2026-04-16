package top.arctain.snowTerritory.reinforce.service;

import org.bukkit.entity.Player;
import top.arctain.snowTerritory.reinforce.config.CostConfigManager;
import top.arctain.snowTerritory.reinforce.data.CostConfig;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.stvip.service.StvipService;

import java.util.HashMap;
import java.util.Map;

/**
 * 消耗计算服务
 * 根据物品ID、玩家信息和强化等级计算最终消耗
 */
public class CostCalculationService {

    private final ReinforceConfigManager configManager;
    private final CostConfigManager costConfigManager;
    private final MMOCoreService mmocoreService;
    private final ExpressionService expressionService;
    private final StvipService stvipService;

    public CostCalculationService(ReinforceConfigManager configManager,
                                  CostConfigManager costConfigManager,
                                  MMOCoreService mmocoreService,
                                  ExpressionService expressionService,
                                  StvipService stvipService) {
        this.configManager = configManager;
        this.costConfigManager = costConfigManager;
        this.mmocoreService = mmocoreService;
        this.expressionService = expressionService;
        this.stvipService = stvipService;
    }

    /**
     * 计算金币消耗
     * @param player 玩家
     * @param itemId 物品ID
     * @param reinforceLevel 强化等级
     * @return 金币消耗，如果没有配置则返回0
     */
    public double calculateGoldCost(Player player, String itemId, int reinforceLevel) {
        CostConfigManager.ItemCostConfig itemConfig = costConfigManager.getItemCostConfig(itemId);
        
        // 如果物品没有配置，使用全局默认配置
        if (itemConfig == null || !itemConfig.isEnabled()) {
            return applyReinforceVipGold(player, configManager.getCostVaultGold());
        }
        
        CostConfig goldConfig = itemConfig.getGoldCost();
        if (goldConfig == null || !goldConfig.isValid()) {
            return applyReinforceVipGold(player, configManager.getCostVaultGold());
        }
        
        return applyReinforceVipGold(player, calculateCost(player, goldConfig, reinforceLevel));
    }

    /**
     * 计算点券消耗
     * @param player 玩家
     * @param itemId 物品ID
     * @param reinforceLevel 强化等级
     * @return 点券消耗，如果没有配置则返回0
     */
    public int calculatePointsCost(Player player, String itemId, int reinforceLevel) {
        CostConfigManager.ItemCostConfig itemConfig = costConfigManager.getItemCostConfig(itemId);
        
        // 如果物品没有配置，使用全局默认配置
        if (itemConfig == null || !itemConfig.isEnabled()) {
            return applyReinforceVipPoints(player, configManager.getCostPlayerPoints());
        }
        
        CostConfig pointsConfig = itemConfig.getPointsCost();
        if (pointsConfig == null || !pointsConfig.isValid()) {
            return applyReinforceVipPoints(player, configManager.getCostPlayerPoints());
        }
        
        return applyReinforceVipPoints(player, (int) Math.round(calculateCost(player, pointsConfig, reinforceLevel)));
    }

    private double applyReinforceVipGold(Player player, double base) {
        if (stvipService == null) {
            return Math.max(0, base);
        }
        return Math.max(0, base * stvipService.getReinforceCostMultiplier(player));
    }

    private int applyReinforceVipPoints(Player player, int base) {
        if (stvipService == null) {
            return Math.max(0, base);
        }
        return Math.max(0, (int) Math.round(base * stvipService.getReinforceCostMultiplier(player)));
    }

    /**
     * 计算消耗（基础消耗 * 强化等级倍率）
     */
    private double calculateCost(Player player, CostConfig costConfig, int reinforceLevel) {
        // 获取玩家信息
        int playerLevel = mmocoreService.getPlayerLevel(player);
        
        // 准备变量
        Map<String, Double> baseVariables = new HashMap<>();
        baseVariables.put("playerLevel", (double) playerLevel);
        
        // 计算基础消耗
        double baseCost = expressionService.evaluate(costConfig.getBaseExpression(), baseVariables);
        
        // 准备等级倍率变量
        Map<String, Double> levelVariables = new HashMap<>();
        levelVariables.put("level", (double) reinforceLevel);
        
        // 计算强化等级倍率
        double levelMultiplier = expressionService.evaluate(costConfig.getLevelMultiplier(), levelVariables);
        
        // 最终消耗 = 基础消耗 * 强化等级倍率
        return baseCost * levelMultiplier;
    }

    /**
     * 检查物品是否配置了金币消耗
     */
    public boolean hasGoldCost(String itemId) {
        CostConfigManager.ItemCostConfig itemConfig = costConfigManager.getItemCostConfig(itemId);
        if (itemConfig == null || !itemConfig.isEnabled()) {
            return configManager.getCostVaultGold() > 0;
        }
        CostConfig goldConfig = itemConfig.getGoldCost();
        return goldConfig != null && goldConfig.isValid();
    }

    /**
     * 检查物品是否配置了点券消耗
     */
    public boolean hasPointsCost(String itemId) {
        CostConfigManager.ItemCostConfig itemConfig = costConfigManager.getItemCostConfig(itemId);
        if (itemConfig == null || !itemConfig.isEnabled()) {
            return configManager.getCostPlayerPoints() > 0;
        }
        CostConfig pointsConfig = itemConfig.getPointsCost();
        return pointsConfig != null && pointsConfig.isValid();
    }
}
