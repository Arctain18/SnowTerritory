package top.arctain.snowTerritory.reinforce.gui;

import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.reinforce.utils.ReinforceUtils;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.ColorUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReinforceGUI {

    private final ReinforceConfigManager config;
    private final Main plugin;
    private Object economy;  // Vault经济（使用Object避免类加载时依赖）
    private Object playerPointsAPI;  // PlayerPoints API（使用Object避免类加载时依赖）
    private final Set<UUID> reinforcingPlayers;  // 正在强化的玩家集合，用于防止重复点击

    public ReinforceGUI(ReinforceConfigManager config, Main plugin) {
        this.config = config;
        this.plugin = plugin;
        this.reinforcingPlayers = new HashSet<>();
        
        // 使用反射安全获取经济系统（避免类加载时依赖）
        this.economy = getVaultEconomy();
        
        // 使用反射安全获取 PlayerPoints（避免类加载时依赖）
        this.playerPointsAPI = getPlayerPointsAPI();
    }

    /**
     * 符文校验结果，复用保护符和强化符的判定逻辑
     */
    private static class CharmInfo {
        final boolean valid;
        final boolean expired;
        final int maxLevel;
        final int bonus; // 仅强化符使用，保护符为0

        CharmInfo(boolean valid, boolean expired, int maxLevel, int bonus) {
            this.valid = valid;
            this.expired = expired;
            this.maxLevel = maxLevel;
            this.bonus = bonus;
        }

        static CharmInfo invalid() {
            return new CharmInfo(false, false, -1, 0);
        }
    }

    private CharmInfo evaluateProtectCharm(ItemStack protectCharm, int nextLevel) {
        if (protectCharm == null) return CharmInfo.invalid();
        if (!ReinforceUtils.isPreservationToken(protectCharm, config.getPreservationTokenType())) return CharmInfo.invalid();

        int maxLevel = ReinforceUtils.parseMaxLevelFromLore(protectCharm);
        boolean valid = maxLevel >= 0 && nextLevel <= maxLevel;
        boolean expired = maxLevel >= 0 && nextLevel > maxLevel;
        return new CharmInfo(valid, expired, maxLevel, 0);
    }

    private CharmInfo evaluateEnhanceCharm(ItemStack enhanceCharm, int nextLevel) {
        if (enhanceCharm == null) return CharmInfo.invalid();
        if (!ReinforceUtils.isUpgradeToken(enhanceCharm, config.getUpgradeTokenType())) return CharmInfo.invalid();

        int maxLevel = ReinforceUtils.parseMaxLevelFromLore(enhanceCharm);
        int bonus = ReinforceUtils.parseProbabilityBoostFromLore(enhanceCharm);
        boolean valid = maxLevel >= 0 && nextLevel <= maxLevel && bonus > 0;
        boolean expired = maxLevel >= 0 && nextLevel > maxLevel;
        return new CharmInfo(valid, expired, maxLevel, bonus);
    }
    
    /**
     * 使用反射获取 Vault Economy（避免直接导入类）
     */
    private Object getVaultEconomy() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            var economyReg = Bukkit.getServer().getServicesManager().getRegistration(economyClass);
            return economyReg != null ? economyReg.getProvider() : null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 使用反射获取 PlayerPoints API（避免直接导入类）
     */
    private Object getPlayerPointsAPI() {
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
    
    /**
     * 获取玩家金币余额（使用反射）
     */
    private double getBalance(Player player) {
        if (economy == null) return 0;
        try {
            Method getBalanceMethod = economy.getClass().getMethod("getBalance", Player.class);
            Object result = getBalanceMethod.invoke(economy, player);
            return result instanceof Number ? ((Number) result).doubleValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 扣除玩家金币（使用反射）
     */
    private void withdrawPlayer(Player player, double amount) {
        if (economy == null) return;
        try {
            Method withdrawMethod = economy.getClass().getMethod("withdrawPlayer", Player.class, double.class);
            withdrawMethod.invoke(economy, player, amount);
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 获取玩家点券余额（使用反射）
     */
    private int getPlayerPoints(UUID uuid) {
        if (playerPointsAPI == null) return 0;
        try {
            Method lookMethod = playerPointsAPI.getClass().getMethod("look", UUID.class);
            Object result = lookMethod.invoke(playerPointsAPI, uuid);
            return result instanceof Number ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 扣除玩家点券（使用反射）
     */
    private void takePlayerPoints(UUID uuid, int amount) {
        if (playerPointsAPI == null) return;
        try {
            Method takeMethod = playerPointsAPI.getClass().getMethod("take", UUID.class, int.class);
            takeMethod.invoke(playerPointsAPI, uuid, amount);
        } catch (Exception e) {
            // 忽略错误
        }
    }

    public void openGUI(Player player) {
        // 从配置创建GUI
        Inventory gui = Bukkit.createInventory(null, config.getGuiSize(), ColorUtils.colorize(config.getGuiTitle()));

        // 添加自定义槽位（装饰等）
        config.getCustomSlots().forEach((slot, itemConfig) -> {
            ItemStack customItem = new ItemStack(itemConfig.getMaterial());
            ItemMeta meta = customItem.getItemMeta();
            if (meta != null) {
                if (itemConfig.getName() != null && !itemConfig.getName().isEmpty()) {
                    meta.setDisplayName(MessageUtils.colorize(itemConfig.getName()));
                }
                if (itemConfig.getLore() != null && !itemConfig.getLore().isEmpty()) {
                    List<String> lore = new ArrayList<>();
                    for (String line : itemConfig.getLore()) {
                        lore.add(MessageUtils.colorize(line));
                    }
                    meta.setLore(lore);
                }
                customItem.setItemMeta(meta);
            }
            gui.setItem(slot, customItem);
        });

        // 添加确认和取消按钮（可点击）
        ItemStack confirmButton = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(MessageUtils.colorize(config.getGuiConfirmButtonName()));
            confirmMeta.setLore(Collections.singletonList(MessageUtils.colorize("&7点击确认强化")));
            confirmButton.setItemMeta(confirmMeta);
        }
        gui.setItem(config.getSlotConfirm(), confirmButton);

        ItemStack cancelButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(MessageUtils.colorize(config.getGuiCancelButtonName()));
            cancelMeta.setLore(Collections.singletonList(MessageUtils.colorize("&7点击取消操作")));
            cancelButton.setItemMeta(cancelMeta);
        }
        gui.setItem(config.getSlotCancel(), cancelButton);

        // 空槽位已由玩家放置，无需预填充
        player.openInventory(gui);
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
            
            // 验证保护符
            CharmInfo protectCharmInfo = evaluateProtectCharm(protectCharm, nextLevel);
            
            // 验证强化符
            CharmInfo enhanceCharmInfo = evaluateEnhanceCharm(enhanceCharm, nextLevel);
            
            // 计算成功率
            double baseSuccessRate = config.getSuccessRateForLevel(nextLevel);
            if (enhanceCharmInfo.valid) {
                baseSuccessRate += enhanceCharmInfo.bonus / 100.0; // 强化符增加概率（百分比转小数）
            }
            double successRate = Math.min(1.0, baseSuccessRate); // 确保不超过100%
            
            // 计算失败率
            double failDegradeChance = config.getReinforceFailDegradeChance();
            if (protectCharmInfo.valid) {
                failDegradeChance = 0.0; // 保护符：失败不降级
            }
            
            lore.add(MessageUtils.colorize(config.getConfirmButtonLoreSeparator()));
            
            // 当前等级
            String currentLevelText = config.getConfirmButtonLoreCurrentLevel()
                .replace("{currentLevel}", String.valueOf(currentLevel))
                .replace("{nextLevel}", String.valueOf(nextLevel));
            lore.add(MessageUtils.colorize(currentLevelText));
            
            // 成功率（包含强化符加成）
            String bonusText = "";
            if (enhanceCharmInfo.valid) {
                bonusText = "&7(&a+" + enhanceCharmInfo.bonus + "%&7)";
            }
            String successRateText = config.getConfirmButtonLoreSuccessRate()
                .replace("{successRate}", String.format("%.1f", successRate * 100))
                .replace("{bonus}", bonusText);
            lore.add(MessageUtils.colorize(successRateText));
            
            // 失败降级概率（包含保护符保护）
            String protectText = "";
            if (protectCharmInfo.valid) {
                double originalFailDegradeChance = config.getReinforceFailDegradeChance();
                if (originalFailDegradeChance > 0) {
                    protectText = "&7(&c-" + String.format("%.1f", originalFailDegradeChance * 100) + "%&7)";
                }
            }
            String failDegradeText = config.getConfirmButtonLoreFailDegradeChance()
                .replace("{chance}", String.format("%.1f", failDegradeChance * 100))
                .replace("{protect}", protectText);
            lore.add(MessageUtils.colorize(failDegradeText));
            
            // 如果有保护符但无效，显示失效信息
            if (protectCharm != null && !protectCharmInfo.valid) {
                if (protectCharmInfo.expired) {
                    String expiredText = config.getConfirmButtonLoreProtectCharmExpired()
                        .replace("{maxLevel}", String.valueOf(protectCharmInfo.maxLevel));
                    lore.add(MessageUtils.colorize(expiredText));
                } else {
                    lore.add(MessageUtils.colorize(config.getConfirmButtonLoreProtectCharmInvalid()));
                }
            }
            
            // 如果有强化符但无效，显示失效信息
            if (enhanceCharm != null && !enhanceCharmInfo.valid) {
                if (enhanceCharmInfo.expired) {
                    String expiredText = config.getConfirmButtonLoreEnhanceCharmExpired()
                        .replace("{maxLevel}", String.valueOf(enhanceCharmInfo.maxLevel));
                    lore.add(MessageUtils.colorize(expiredText));
                } else {
                    lore.add(MessageUtils.colorize(config.getConfirmButtonLoreEnhanceCharmInvalid()));
                }
            }
            
            // 显示消耗资源
            List<String> costLines = new ArrayList<>();
            if (economy != null && config.getCostVaultGold() > 0) {
                double balance = getBalance(player);
                String color = balance >= config.getCostVaultGold() ? "&a" : "&c";
                String goldText = config.getConfirmButtonLoreCostGold()
                    .replace("{color}", color)
                    .replace("{amount}", MessageUtils.formatNumber(config.getCostVaultGold()));
                costLines.add(MessageUtils.colorize(goldText));
            }
            if (playerPointsAPI != null && config.getCostPlayerPoints() > 0) {
                int points = getPlayerPoints(player.getUniqueId());
                String color = points >= config.getCostPlayerPoints() ? "&a" : "&c";
                String pointsText = config.getConfirmButtonLoreCostPoints()
                    .replace("{color}", color)
                    .replace("{amount}", MessageUtils.formatNumber(config.getCostPlayerPoints()));
                costLines.add(MessageUtils.colorize(pointsText));
            }
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
                lore.add(MessageUtils.colorize(config.getConfirmButtonLoreCostTitle()));
                lore.addAll(costLines);
            }
        }

        confirmMeta.setLore(lore);
        confirmButton.setItemMeta(confirmMeta);
        gui.setItem(config.getSlotConfirm(), confirmButton);
    }

    // 执行强化逻辑（在监听器中调用）
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
        if (economy != null && config.getCostVaultGold() > 0) {
            if (getBalance(player) < config.getCostVaultGold()) {
                reinforcingPlayers.remove(playerUUID);  // 移除标记
                MessageUtils.sendError(player, "reinforce.insufficient-gold", "&c✗ &f金币不足！需要: &e{cost}", 
                    "cost", MessageUtils.formatNumber(config.getCostVaultGold()));
                return;
            }
        }
        if (playerPointsAPI != null && config.getCostPlayerPoints() > 0) {
            if (getPlayerPoints(player.getUniqueId()) < config.getCostPlayerPoints()) {
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
        
        CharmInfo protectCharmInfo = evaluateProtectCharm(protectCharm, nextLevel);
        
        CharmInfo enhanceCharmInfo = evaluateEnhanceCharm(enhanceCharm, nextLevel);

        // 扣除消耗
        if (economy != null && config.getCostVaultGold() > 0) {
            withdrawPlayer(player, config.getCostVaultGold());
        }
        if (playerPointsAPI != null && config.getCostPlayerPoints() > 0) {
            takePlayerPoints(player.getUniqueId(), config.getCostPlayerPoints());
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
                    updateConfirmButtonLore(player, gui);
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

