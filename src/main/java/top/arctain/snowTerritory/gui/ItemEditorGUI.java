package top.arctain.snowTerritory.gui;

import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.Utils;
import top.arctain.snowTerritory.utils.ColorUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ItemEditorGUI {

    private final PluginConfig config;
    private final Main plugin;
    private Object economy;  // Vault经济（使用Object避免类加载时依赖）
    private Object playerPointsAPI;  // PlayerPoints API（使用Object避免类加载时依赖）
    private final Set<UUID> reinforcingPlayers;  // 正在强化的玩家集合，用于防止重复点击

    public ItemEditorGUI(PluginConfig config, Main plugin) {
        this.config = config;
        this.plugin = plugin;
        this.reinforcingPlayers = new HashSet<>();
        
        // 使用反射安全获取经济系统（避免类加载时依赖）
        this.economy = getVaultEconomy();
        
        // 使用反射安全获取 PlayerPoints（避免类加载时依赖）
        this.playerPointsAPI = getPlayerPointsAPI();
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
        if (weapon != null && Utils.isReinforceable(weapon)) {
            int currentLevel = Utils.getCurrentLevel(weapon);
            int nextLevel = currentLevel + 1;
            
            // 验证保护符
            boolean protectCharmValid = false;
            boolean protectCharmExpired = false;
            int protectCharmMaxLevel = -1;
            if (protectCharm != null) {
                if (Utils.isPreservationToken(protectCharm, config.getPreservationTokenType())) {
                    protectCharmMaxLevel = Utils.parseMaxLevelFromLore(protectCharm);
                    if (protectCharmMaxLevel >= 0 && nextLevel <= protectCharmMaxLevel) {
                        protectCharmValid = true;
                    } else if (protectCharmMaxLevel >= 0) {
                        protectCharmExpired = true;
                    }
                }
            }
            
            // 验证强化符
            boolean enhanceCharmValid = false;
            boolean enhanceCharmExpired = false;
            int enhanceCharmMaxLevel = -1;
            int enhanceCharmBonus = 0;
            if (enhanceCharm != null) {
                if (Utils.isUpgradeToken(enhanceCharm, config.getUpgradeTokenType())) {
                    enhanceCharmMaxLevel = Utils.parseMaxLevelFromLore(enhanceCharm);
                    enhanceCharmBonus = Utils.parseProbabilityBoostFromLore(enhanceCharm);
                    if (enhanceCharmMaxLevel >= 0 && nextLevel <= enhanceCharmMaxLevel && enhanceCharmBonus > 0) {
                        enhanceCharmValid = true;
                    } else if (enhanceCharmMaxLevel >= 0) {
                        enhanceCharmExpired = true;
                    }
                }
            }
            
            // 计算成功率
            double baseSuccessRate = config.getSuccessRateForLevel(nextLevel);
            if (enhanceCharmValid) {
                baseSuccessRate += enhanceCharmBonus / 100.0; // 强化符增加概率（百分比转小数）
            }
            double successRate = Math.min(1.0, baseSuccessRate); // 确保不超过100%
            
            // 计算失败率
            double failDegradeChance = config.getReinforceFailDegradeChance();
            if (protectCharmValid) {
                failDegradeChance = 0.0; // 保护符：失败不降级
            }
            double totalFailRate = 1.0 - successRate; // 总失败率（包括降级和维持）
            
            lore.add(MessageUtils.colorize(config.getConfirmButtonLoreSeparator()));
            
            // 当前等级
            String currentLevelText = config.getConfirmButtonLoreCurrentLevel()
                .replace("{currentLevel}", String.valueOf(currentLevel))
                .replace("{nextLevel}", String.valueOf(nextLevel));
            lore.add(MessageUtils.colorize(currentLevelText));
            
            // 成功率
            String successRateText = config.getConfirmButtonLoreSuccessRate()
                .replace("{successRate}", String.format("%.1f", successRate * 100));
            lore.add(MessageUtils.colorize(successRateText));
            
            // 失败率
            String failRateText = config.getConfirmButtonLoreFailRate()
                .replace("{failRate}", String.format("%.1f", totalFailRate * 100));
            lore.add(MessageUtils.colorize(failRateText));
            
            // 如果有保护符，显示保护信息或失效信息
            if (protectCharm != null) {
                if (protectCharmValid) {
                    lore.add(MessageUtils.colorize(config.getConfirmButtonLoreProtectCharmHint()));
                } else if (protectCharmExpired) {
                    lore.add(MessageUtils.colorize("&c&o保护符失效 (最高保护等级: +" + protectCharmMaxLevel + ")"));
                } else {
                    lore.add(MessageUtils.colorize("&c&o保护符无效 (类型不匹配)"));
                }
            } else if (failDegradeChance > 0) {
                // 显示失败降级概率
                String failDegradeText = config.getConfirmButtonLoreFailDegradeChance()
                    .replace("{chance}", String.format("%.1f", failDegradeChance * 100));
                lore.add(MessageUtils.colorize(failDegradeText));
            }
            
            // 如果有强化符，显示加成信息或失效信息
            if (enhanceCharm != null) {
                if (enhanceCharmValid) {
                    String enhanceCharmText = config.getConfirmButtonLoreEnhanceCharmHint()
                        .replace("{bonus}", String.valueOf(enhanceCharmBonus));
                    lore.add(MessageUtils.colorize(enhanceCharmText));
                } else if (enhanceCharmExpired) {
                    lore.add(MessageUtils.colorize("&c&o强化符失效 (最高强化等级: +" + enhanceCharmMaxLevel + ")"));
                } else {
                    lore.add(MessageUtils.colorize("&c&o强化符无效 (类型不匹配或概率解析失败)"));
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
        if (weapon == null || !Utils.isMMOItem(weapon)) {
            reinforcingPlayers.remove(playerUUID);  // 移除标记
            MessageUtils.sendError(player, "reinforce.invalid-item", "&c✗ &f请放置有效的MMO物品作为武器！");
            return;
        }

        // 检查是否可强化
        if (!Utils.isReinforceable(weapon)) {
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
        int currentLevel = Utils.getCurrentLevel(weapon);
        int nextLevel = currentLevel + 1;
        
        boolean protectCharmValid = false;
        if (protectCharm != null) {
            if (Utils.isPreservationToken(protectCharm, config.getPreservationTokenType())) {
                int protectCharmMaxLevel = Utils.parseMaxLevelFromLore(protectCharm);
                if (protectCharmMaxLevel >= 0 && nextLevel <= protectCharmMaxLevel) {
                    protectCharmValid = true;
                }
            }
        }
        
        boolean enhanceCharmValid = false;
        int enhanceCharmBonus = 0;
        if (enhanceCharm != null) {
            if (Utils.isUpgradeToken(enhanceCharm, config.getUpgradeTokenType())) {
                int enhanceCharmMaxLevel = Utils.parseMaxLevelFromLore(enhanceCharm);
                enhanceCharmBonus = Utils.parseProbabilityBoostFromLore(enhanceCharm);
                if (enhanceCharmMaxLevel >= 0 && nextLevel <= enhanceCharmMaxLevel && enhanceCharmBonus > 0) {
                    enhanceCharmValid = true;
                }
            }
        }

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
        // 只有有效的保护符和强化符才会被消耗
        if (protectCharmValid) {
            gui.setItem(config.getSlotProtectCharm(), null);  // 消耗保护符
        }
        if (enhanceCharmValid) {
            gui.setItem(config.getSlotEnhanceCharm(), null);  // 消耗强化符
        }
        
        // 计算概率（基础 + 强化符效果）
        double baseSuccessRate = config.getSuccessRateForLevel(nextLevel);  // 针对下一级
        if (enhanceCharmValid) {
            baseSuccessRate += enhanceCharmBonus / 100.0;  // 强化符增加概率（百分比转小数）
        }
        double failDegradeChance = config.getReinforceFailDegradeChance();
        double maintainChance = config.getReinforceMaintainChance();
        if (protectCharmValid) {
            failDegradeChance = 0.0;  // 保护符：失败不降级
        }

        // 执行强化
        Utils.ReinforceResult result = Utils.attemptReinforce(baseSuccessRate, failDegradeChance, maintainChance);
        
        try {
            LiveMMOItem mmoItem = new LiveMMOItem(weapon);  // 获取LiveMMOItem用于修改
            
            switch (result) {
                case SUCCESS:
                    Utils.modifyMMOAttribute(mmoItem, config.getAttributeBoostPercent(), config.getReinforceableAttributes());
                    // 从LiveMMOItem获取更新后的ItemStack
                    ItemStack updatedWeapon = Utils.getUpdatedItemStack(mmoItem);
                    if (updatedWeapon != null) {
                        Utils.updateItemName(updatedWeapon, currentLevel + 1);
                        weapon = updatedWeapon;  // 更新weapon引用
                    } else {
                        // 如果无法获取更新后的ItemStack，至少更新名字
                        Utils.updateItemName(weapon, currentLevel + 1);
                    }
                    MessageUtils.sendReinforceSuccess(player, currentLevel + 1);
                    break;
                case FAIL_DEGRADE:
                    int newLevel = Math.max(0, currentLevel - 1);
                    Utils.modifyMMOAttribute(mmoItem, 1 / config.getAttributeBoostPercent(), config.getReinforceableAttributes());
                    // 从LiveMMOItem获取更新后的ItemStack
                    ItemStack updatedWeapon2 = Utils.getUpdatedItemStack(mmoItem);
                    if (updatedWeapon2 != null) {
                        Utils.updateItemName(updatedWeapon2, newLevel);
                        weapon = updatedWeapon2;  // 更新weapon引用
                    } else {
                        // 如果无法获取更新后的ItemStack，至少更新名字
                        Utils.updateItemName(weapon, newLevel);
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