package top.arctain.snowTerritory.reinforce.gui;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.reinforce.service.*;
import top.arctain.snowTerritory.reinforce.utils.ReinforceUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责确认按钮 Lore 的计算和渲染
 */
public class ConfirmButtonLoreService {

    private final ReinforceConfigManager config;
    private final EconomyService economyService;
    private final PlayerPointsService playerPointsService;
    private final CharmService charmService;
    private final CostCalculationService costCalculationService;
    private final MMOCoreService mmocoreService;

    public ConfirmButtonLoreService(ReinforceConfigManager config,
                                    EconomyService economyService,
                                    PlayerPointsService playerPointsService,
                                    CharmService charmService,
                                    CostCalculationService costCalculationService,
                                    MMOCoreService mmocoreService) {
        this.config = config;
        this.economyService = economyService;
        this.playerPointsService = playerPointsService;
        this.charmService = charmService;
        this.costCalculationService = costCalculationService;
        this.mmocoreService = mmocoreService;
    }

    /**
     * 更新确认按钮的lore，显示成功率、失败率和消耗资源
     */
    public void updateConfirmButtonLore(Player player, Inventory gui) {
        ItemStack confirmButton = gui.getItem(config.getSlotConfirm());
        if (confirmButton == null) return;

        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta == null) return;

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize(config.getConfirmButtonLoreClickHint()));

        // 获取槽位物品
        ItemStack weapon = gui.getItem(config.getSlotWeapon());
        ItemStack protectCharm = gui.getItem(config.getSlotProtectCharm());
        ItemStack enhanceCharm = gui.getItem(config.getSlotEnhanceCharm());

        // 只有当武器存在且可强化时才显示信息
        if (weapon != null && ReinforceUtils.isReinforceable(weapon)) {
            int currentLevel = ReinforceUtils.getCurrentLevel(weapon);
            int nextLevel = currentLevel + 1;
            int nextNextLevel = nextLevel + 1;
            
            String itemId = MMOItems.getID(weapon);

            CharmService.CharmInfo protectCharmInfo = charmService.evaluateProtectCharm(protectCharm, nextLevel);
            CharmService.CharmInfo enhanceCharmInfo = charmService.evaluateEnhanceCharm(enhanceCharm, nextLevel);

            double successRate = calculateSuccessRate(nextLevel, enhanceCharmInfo);
            double failDegradeChance = calculateFailDegradeChance(protectCharmInfo);

            lore.add(MessageUtils.colorize(config.getConfirmButtonLoreSeparator()));

            String currentLevelText = config.getConfirmButtonLoreCurrentLevel()
                    .replace("{currentLevel}", String.valueOf(currentLevel))
                    .replace("{nextLevel}", String.valueOf(nextLevel));
            lore.add(MessageUtils.colorize(currentLevelText));

            String bonusText = getBonusText(enhanceCharmInfo);
            String successRateText = config.getConfirmButtonLoreSuccessRate()
                    .replace("{successRate}", String.format("%.1f", successRate * 100))
                    .replace("{bonus}", bonusText);
            lore.add(MessageUtils.colorize(successRateText));

            String protectText = getProtectText(protectCharmInfo);
            String failDegradeText = config.getConfirmButtonLoreFailDegradeChance()
                    .replace("{chance}", String.format("%.1f", failDegradeChance * 100))
                    .replace("{protect}", protectText);
            lore.add(MessageUtils.colorize(failDegradeText));

            if (protectCharm != null && !protectCharmInfo.valid) {
                invalidProtectCharm(lore, protectCharmInfo);
            }

            if (enhanceCharm != null && !enhanceCharmInfo.valid) {
                invalidEnhanceCharm(lore, enhanceCharmInfo);
            }

            List<String> costLines = new ArrayList<>();
            
            calculateGoldCost(player, nextLevel, itemId, costLines);
            calculatePlayerPointsCount(player, nextLevel, itemId, costLines);
            
            if (config.getCostMaterials() > 0) {
                int materialCount = 0;
                for (int i = 0; i < 6; i++) {
                    ItemStack material = gui.getItem(config.getSlotMaterials()[i]);
                    if (material != null && !material.getType().isAir()) {
                        materialCount++;
                    }
                }
                String color = materialCount >= config.getCostMaterials() ? "&a" : "&c";
                String materialsText = config.getConfirmButtonLoreCostMaterials()
                        .replace("{color}", color)
                        .replace("{current}", String.valueOf(materialCount))
                        .replace("{required}", String.valueOf(config.getCostMaterials()));
                costLines.add(MessageUtils.colorize(materialsText));
            }

            if (!costLines.isEmpty()) {
                lore.add(""); // 空行分隔
                lore.add(MessageUtils.colorize(config.getConfirmButtonLoreSeparator()));
                lore.add(MessageUtils.colorize(config.getConfirmButtonLoreCostTitle())); // 本次升级消耗
                lore.addAll(costLines);
            }

            // 预估下一级（再往上一档）升级所需的货币消耗（仅展示金币和点券）
            List<String> nextCostLines = new ArrayList<>();

            if (itemId != null) {
                // 金币预估消耗
                if (costCalculationService.hasGoldCost(itemId)) {
                    double nextGoldCost = costCalculationService.calculateGoldCost(player, itemId, nextNextLevel);
                    if (nextGoldCost > 0) {
                        double balance = economyService.getBalance(player);
                        String color = balance >= nextGoldCost ? "&a" : "&c";
                        String goldText = config.getConfirmButtonLoreCostGold()
                                .replace("{color}", color)
                                .replace("{amount}", MessageUtils.formatNumber((long) nextGoldCost));
                        nextCostLines.add(MessageUtils.colorize(goldText));
                    }
                }

                // 点券预估消耗
                if (costCalculationService.hasPointsCost(itemId)) {
                    int nextPointsCost = costCalculationService.calculatePointsCost(player, itemId, nextNextLevel);
                    if (nextPointsCost > 0) {
                        int points = playerPointsService.getPoints(player.getUniqueId());
                        String color = points >= nextPointsCost ? "&a" : "&c";
                        String pointsText = config.getConfirmButtonLoreCostPoints()
                                .replace("{color}", color)
                                .replace("{amount}", MessageUtils.formatNumber(nextPointsCost));
                        nextCostLines.add(MessageUtils.colorize(pointsText));
                    }
                }
            }

            if (!nextCostLines.isEmpty()) {
                lore.add(""); // 空行分隔
                lore.add(MessageUtils.colorize(config.getConfirmButtonLoreSeparator()));
                lore.add(MessageUtils.colorize(config.getConfirmButtonLoreNextCostTitle())); // 下一级升级预估消耗
                lore.addAll(nextCostLines);
            }
        }

        confirmMeta.setLore(lore);
        confirmButton.setItemMeta(confirmMeta);
        gui.setItem(config.getSlotConfirm(), confirmButton);
    }

    private void calculatePlayerPointsCount(Player player, int nextLevel, String itemId, List<String> costLines) {
        if (itemId != null && costCalculationService.hasPointsCost(itemId)) {
            int pointsCost = costCalculationService.calculatePointsCost(player, itemId, nextLevel);
            if (pointsCost > 0) {
                int points = playerPointsService.getPoints(player.getUniqueId());
                String color = points >= pointsCost ? "&a" : "&c";
                String pointsText = config.getConfirmButtonLoreCostPoints()
                        .replace("{color}", color)
                        .replace("{amount}", MessageUtils.formatNumber(pointsCost));
                costLines.add(MessageUtils.colorize(pointsText));
            }
        } else if (playerPointsService.isEnabled() && config.getCostPlayerPoints() > 0) {
            // 回退到全局配置
            int points = playerPointsService.getPoints(player.getUniqueId());
            String color = points >= config.getCostPlayerPoints() ? "&a" : "&c";
            String pointsText = config.getConfirmButtonLoreCostPoints()
                    .replace("{color}", color)
                    .replace("{amount}", MessageUtils.formatNumber(config.getCostPlayerPoints()));
            costLines.add(MessageUtils.colorize(pointsText));
        }
    }

    private void calculateGoldCost(Player player, int nextLevel, String itemId, List<String> costLines) {
        if (itemId != null && costCalculationService.hasGoldCost(itemId)) {
            double goldCost = costCalculationService.calculateGoldCost(player, itemId, nextLevel);
            if (goldCost > 0) {
                double balance = economyService.getBalance(player);
                String color = balance >= goldCost ? "&a" : "&c";
                String goldText = config.getConfirmButtonLoreCostGold()
                        .replace("{color}", color)
                        .replace("{amount}", MessageUtils.formatNumber((long) goldCost));
                costLines.add(MessageUtils.colorize(goldText));
            }
        } else if (economyService.isEnabled() && config.getCostVaultGold() > 0) {
            // 回退到全局配置
            double balance = economyService.getBalance(player);
            String color = balance >= config.getCostVaultGold() ? "&a" : "&c";
            String goldText = config.getConfirmButtonLoreCostGold()
                    .replace("{color}", color)
                    .replace("{amount}", MessageUtils.formatNumber(config.getCostVaultGold()));
            costLines.add(MessageUtils.colorize(goldText));
        }
    }

    private void invalidEnhanceCharm(List<String> lore, CharmService.CharmInfo enhanceCharmInfo) {
        if (enhanceCharmInfo.expired) {
            String expiredText = config.getConfirmButtonLoreEnhanceCharmExpired()
                    .replace("{maxLevel}", String.valueOf(enhanceCharmInfo.maxLevel));
            lore.add(MessageUtils.colorize(expiredText));
        } else {
            lore.add(MessageUtils.colorize(config.getConfirmButtonLoreEnhanceCharmInvalid()));
        }
    }

    private void invalidProtectCharm(List<String> lore, CharmService.CharmInfo protectCharmInfo) {
        if (protectCharmInfo.expired) {
            String expiredText = config.getConfirmButtonLoreProtectCharmExpired()
                    .replace("{maxLevel}", String.valueOf(protectCharmInfo.maxLevel));
            lore.add(MessageUtils.colorize(expiredText));
        } else {
            lore.add(MessageUtils.colorize(config.getConfirmButtonLoreProtectCharmInvalid()));
        }
    }

    private String getProtectText(CharmService.CharmInfo protectCharmInfo) {
        String protectText = "";
        if (protectCharmInfo.valid) {
            double originalFailDegradeChance = config.getReinforceFailDegradeChance();
            if (originalFailDegradeChance > 0) {
                protectText = "&7(&c-" + String.format("%.1f", originalFailDegradeChance * 100) + "%&7)";
            }
        }
        return protectText;
    }

    private String getBonusText(CharmService.CharmInfo enhanceCharmInfo) {
        String bonusText = "";
        if (enhanceCharmInfo.valid) {
            bonusText = "&7(&a+" + enhanceCharmInfo.bonus + "%&7)";
        }
        return bonusText;
    }

    private double calculateFailDegradeChance(CharmService.CharmInfo protectCharmInfo) {
        double failDegradeChance = config.getReinforceFailDegradeChance();
        if (protectCharmInfo.valid) {
            failDegradeChance = 0.0; // 保护符：失败不降级
        }
        return failDegradeChance;
    }

    private double calculateSuccessRate(int nextLevel, CharmService.CharmInfo enhanceCharmInfo) {
        double baseSuccessRate = config.getSuccessRateForLevel(nextLevel);
        if (enhanceCharmInfo.valid) {
            baseSuccessRate += enhanceCharmInfo.bonus / 100.0; // 强化符增加概率（百分比转小数）
        }
        double successRate = Math.min(1.0, baseSuccessRate); // 确保不超过100%
        return successRate;
    }
}
