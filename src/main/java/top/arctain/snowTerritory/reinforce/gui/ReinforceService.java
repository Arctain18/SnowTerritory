package top.arctain.snowTerritory.reinforce.gui;

import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.reinforce.service.CharmService;
import top.arctain.snowTerritory.reinforce.service.EconomyService;
import top.arctain.snowTerritory.reinforce.service.PlayerPointsService;
import top.arctain.snowTerritory.reinforce.utils.ReinforceUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 负责强化核心流程与结果处理
 */
public class ReinforceService {

    private final ReinforceConfigManager config;
    private final Main plugin;
    private final EconomyService economyService;
    private final PlayerPointsService playerPointsService;
    private final CharmService charmService;
    private final ConfirmButtonLoreService confirmButtonLoreService;

    // 正在强化的玩家集合，用于防止重复点击
    private final Set<UUID> reinforcingPlayers = new HashSet<>();

    public ReinforceService(ReinforceConfigManager config,
                            Main plugin,
                            EconomyService economyService,
                            PlayerPointsService playerPointsService,
                            CharmService charmService,
                            ConfirmButtonLoreService confirmButtonLoreService) {
        this.config = config;
        this.plugin = plugin;
        this.economyService = economyService;
        this.playerPointsService = playerPointsService;
        this.charmService = charmService;
        this.confirmButtonLoreService = confirmButtonLoreService;
    }

    /**
     * 执行强化逻辑（在监听器中调用）
     */
    public void applyReinforce(Player player, Inventory gui) {
        // 防止重复点击
        UUID playerUUID = player.getUniqueId();
        if (reinforcingPlayers.contains(playerUUID)) {
            MessageUtils.sendWarning(player, "reinforce.processing", "&e⚠ &f正在处理强化，请稍候...");
            return;
        }

        // 标记玩家正在强化
        reinforcingPlayers.add(playerUUID);

        // 获取槽位物品
        ItemStack weapon = gui.getItem(config.getSlotWeapon());
        ItemStack protectCharm = gui.getItem(config.getSlotProtectCharm());
        ItemStack enhanceCharm = gui.getItem(config.getSlotEnhanceCharm());
        ItemStack[] materials = new ItemStack[6];
        for (int i = 0; i < 6; i++) {
            materials[i] = gui.getItem(config.getSlotMaterials()[i]);
        }

        // 检查是否为MMOItems物品
        if (weapon == null || !ReinforceUtils.isMMOItem(weapon)) {
            reinforcingPlayers.remove(playerUUID);  // 移除标记
            MessageUtils.sendError(player, "reinforce.invalid-item", "&c✗ &f请放置有效的MMO物品作为武器！");
            return;
        }

        // 检查是否可强化
        if (!ReinforceUtils.isReinforceable(weapon)) {
            reinforcingPlayers.remove(playerUUID);  // 移除标记
            MessageUtils.sendError(player, "reinforce.not-reinforceable", "&c✗ &f此物品不可强化！");
            return;
        }

        // 检查消耗（如果启用了经济系统）
        if (economyService.isEnabled() && config.getCostVaultGold() > 0) {
            if (economyService.getBalance(player) < config.getCostVaultGold()) {
                reinforcingPlayers.remove(playerUUID);  // 移除标记
                MessageUtils.sendError(player, "reinforce.insufficient-gold", "&c✗ &f金币不足！需要: &e{cost}",
                        "cost", MessageUtils.formatNumber(config.getCostVaultGold()));
                return;
            }
        }
        if (playerPointsService.isEnabled() && config.getCostPlayerPoints() > 0) {
            if (playerPointsService.getPoints(player.getUniqueId()) < config.getCostPlayerPoints()) {
                reinforcingPlayers.remove(playerUUID);  // 移除标记
                MessageUtils.sendError(player, "reinforce.insufficient-points", "&c✗ &f点券不足！需要: &e{cost}",
                        "cost", MessageUtils.formatNumber(config.getCostPlayerPoints()));
                return;
            }
        }
        int materialCount = (int) Arrays.stream(materials).filter(item -> item != null).count();
        if (materialCount < config.getCostMaterials()) {
            reinforcingPlayers.remove(playerUUID);  // 移除标记
            MessageUtils.sendError(player, "reinforce.insufficient-materials", "&c✗ &f强化材料不足！需要: &e{count} &f个",
                    "count", MessageUtils.formatNumber(config.getCostMaterials()));
            return;
        }

        // 验证保护符和强化符（需要在消耗之前验证）
        int currentLevel = ReinforceUtils.getCurrentLevel(weapon);
        int nextLevel = currentLevel + 1;

        CharmService.CharmInfo protectCharmInfo = charmService.evaluateProtectCharm(protectCharm, nextLevel);
        CharmService.CharmInfo enhanceCharmInfo = charmService.evaluateEnhanceCharm(enhanceCharm, nextLevel);

        // 扣除消耗
        if (economyService.isEnabled() && config.getCostVaultGold() > 0) {
            economyService.withdraw(player, config.getCostVaultGold());
        }
        if (playerPointsService.isEnabled() && config.getCostPlayerPoints() > 0) {
            playerPointsService.takePoints(player.getUniqueId(), config.getCostPlayerPoints());
        }
        for (int i = 0; i < 6; i++) {
            if (materials[i] != null) gui.setItem(config.getSlotMaterials()[i], null);  // 消耗材料
        }
        // 只有有效的保护符和强化符才会被消耗（只消耗一个）
        if (protectCharmInfo.valid) {
            ItemStack protectCharmItem = gui.getItem(config.getSlotProtectCharm());
            if (protectCharmItem != null) {
                int amount = protectCharmItem.getAmount();
                if (amount > 1) {
                    protectCharmItem.setAmount(amount - 1);  // 减少一个
                    gui.setItem(config.getSlotProtectCharm(), protectCharmItem);
                } else {
                    gui.setItem(config.getSlotProtectCharm(), null);  // 消耗完
                }
            }
        }
        if (enhanceCharmInfo.valid) {
            ItemStack enhanceCharmItem = gui.getItem(config.getSlotEnhanceCharm());
            if (enhanceCharmItem != null) {
                int amount = enhanceCharmItem.getAmount();
                if (amount > 1) {
                    enhanceCharmItem.setAmount(amount - 1);  // 减少一个
                    gui.setItem(config.getSlotEnhanceCharm(), enhanceCharmItem);
                } else {
                    gui.setItem(config.getSlotEnhanceCharm(), null);  // 消耗完
                }
            }
        }

        // 计算概率（基础 + 强化符效果）
        double baseSuccessRate = config.getSuccessRateForLevel(nextLevel);  // 针对下一级
        if (enhanceCharmInfo.valid) {
            baseSuccessRate += enhanceCharmInfo.bonus / 100.0;  // 强化符增加概率（百分比转小数）
        }
        double failDegradeChance = config.getReinforceFailDegradeChance();
        double maintainChance = config.getReinforceMaintainChance();
        if (protectCharmInfo.valid) {
            failDegradeChance = 0.0;  // 保护符：失败不降级
        }

        // 执行强化
        ReinforceUtils.ReinforceResult result = ReinforceUtils.attemptReinforce(baseSuccessRate, failDegradeChance, maintainChance);

        try {
            LiveMMOItem mmoItem = new LiveMMOItem(weapon);  // 获取LiveMMOItem用于修改

            switch (result) {
                case SUCCESS:
                    ReinforceUtils.modifyMMOAttribute(mmoItem, config.getAttributeBoostPercent(), config.getReinforceableAttributes());
                    // 从LiveMMOItem获取更新后的ItemStack
                    ItemStack updatedWeapon = ReinforceUtils.getUpdatedItemStack(mmoItem);
                    if (updatedWeapon != null) {
                        ReinforceUtils.updateItemName(updatedWeapon, currentLevel + 1);
                        weapon = updatedWeapon;  // 更新weapon引用
                    } else {
                        // 如果无法获取更新后的ItemStack，至少更新名字
                        ReinforceUtils.updateItemName(weapon, currentLevel + 1);
                    }
                    MessageUtils.sendReinforceSuccess(player, currentLevel + 1);
                    break;
                case FAIL_DEGRADE:
                    int newLevel = Math.max(0, currentLevel - 1);
                    ReinforceUtils.modifyMMOAttribute(mmoItem, 1 / config.getAttributeBoostPercent(), config.getReinforceableAttributes());
                    // 从LiveMMOItem获取更新后的ItemStack
                    ItemStack updatedWeapon2 = ReinforceUtils.getUpdatedItemStack(mmoItem);
                    if (updatedWeapon2 != null) {
                        ReinforceUtils.updateItemName(updatedWeapon2, newLevel);
                        weapon = updatedWeapon2;  // 更新weapon引用
                    } else {
                        // 如果无法获取更新后的ItemStack，至少更新名字
                        ReinforceUtils.updateItemName(weapon, newLevel);
                    }
                    MessageUtils.sendReinforceFail(player, newLevel);
                    break;
                case MAINTAIN:
                    MessageUtils.sendReinforceMaintain(player);
                    break;
            }

            // 更新物品回GUI
            gui.setItem(config.getSlotWeapon(), weapon);

            // 强化完成后，延迟更新确认按钮的lore（使用调度器确保GUI已更新）
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && player.getOpenInventory().getTopInventory() == gui) {
                    confirmButtonLoreService.updateConfirmButtonLore(player, gui);
                }
                reinforcingPlayers.remove(playerUUID);  // 移除标记，允许下次强化
            }, 2L);  // 延迟2 tick，确保GUI已更新
        } catch (Exception e) {
            reinforcingPlayers.remove(playerUUID);  // 移除标记
            MessageUtils.sendError(player, "reinforce.error", "&c✗ &f强化过程中发生错误: &e{error}", "error", e.getMessage());
            MessageUtils.logError("强化过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
