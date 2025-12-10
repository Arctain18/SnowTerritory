package top.arctain.snowTerritory.gui;

import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.arctain.snowTerritory.config.PluginConfig;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ItemEditorGUI {

    private final PluginConfig config;
    private Object economy;  // Vault经济（使用Object避免类加载时依赖）
    private Object playerPointsAPI;  // PlayerPoints API（使用Object避免类加载时依赖）

    public ItemEditorGUI(PluginConfig config) {
        this.config = config;
        
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
        Inventory gui = Bukkit.createInventory(null, config.getGuiSize(), config.getGuiTitle());

        // 添加自定义槽位（装饰等）
        config.getCustomSlots().forEach((slot, itemConfig) -> {
            ItemStack customItem = new ItemStack(itemConfig.getMaterial());
            ItemMeta meta = customItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtils.colorize(itemConfig.getName()));
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
        lore.add(MessageUtils.colorize("&7点击确认强化"));

        // 获取槽位物品
        ItemStack weapon = gui.getItem(config.getSlotWeapon());
        ItemStack protectCharm = gui.getItem(config.getSlotProtectCharm());
        ItemStack enhanceCharm = gui.getItem(config.getSlotEnhanceCharm());

        // 只有当武器存在且可强化时才显示信息
        if (weapon != null && Utils.isReinforceable(weapon)) {
            int currentLevel = Utils.getCurrentLevel(weapon);
            int nextLevel = currentLevel + 1;
            
            // 计算成功率
            double baseSuccessRate = config.getSuccessRateForLevel(nextLevel);
            if (enhanceCharm != null) {
                baseSuccessRate += 0.1; // 强化符增加10%成功率
            }
            double successRate = Math.min(1.0, baseSuccessRate); // 确保不超过100%
            
            // 计算失败率
            double failDegradeChance = config.getReinforceFailDegradeChance();
            if (protectCharm != null) {
                failDegradeChance = 0.0; // 保护符：失败不降级
            }
            double maintainChance = config.getReinforceMaintainChance();
            double totalFailRate = 1.0 - successRate; // 总失败率（包括降级和维持）
            
            lore.add(""); // 空行分隔
            lore.add(MessageUtils.colorize("&8━━━━━━━━━━━━━━━━━━━━"));
            lore.add(MessageUtils.colorize("&7当前等级: &e+" + currentLevel + " &7→ &a+" + nextLevel));
            lore.add(MessageUtils.colorize("&7成功率: &a" + String.format("%.1f", successRate * 100) + "%"));
            lore.add(MessageUtils.colorize("&7失败率: &c" + String.format("%.1f", totalFailRate * 100) + "%"));
            
            // 如果有保护符，显示保护信息
            if (protectCharm != null) {
                lore.add(MessageUtils.colorize("&7&o失败时不会降级"));
            } else if (failDegradeChance > 0) {
                // 显示失败降级概率
                lore.add(MessageUtils.colorize("&7&o失败降级概率: &c" + String.format("%.1f", failDegradeChance * 100) + "%"));
            }
            
            // 如果有强化符，显示加成信息
            if (enhanceCharm != null) {
                lore.add(MessageUtils.colorize("&7&o强化符加成: &a+10%"));
            }
            
            // 显示消耗资源
            List<String> costLines = new ArrayList<>();
            if (economy != null && config.getCostVaultGold() > 0) {
                double balance = getBalance(player);
                String color = balance >= config.getCostVaultGold() ? "&a" : "&c";
                costLines.add(MessageUtils.colorize("&7金币: " + color + MessageUtils.formatNumber(config.getCostVaultGold())));
            }
            if (playerPointsAPI != null && config.getCostPlayerPoints() > 0) {
                int points = getPlayerPoints(player.getUniqueId());
                String color = points >= config.getCostPlayerPoints() ? "&a" : "&c";
                costLines.add(MessageUtils.colorize("&7点券: " + color + MessageUtils.formatNumber(config.getCostPlayerPoints())));
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
                costLines.add(MessageUtils.colorize("&7材料: " + color + materialCount + "&7/" + config.getCostMaterials()));
            }
            
            if (!costLines.isEmpty()) {
                lore.add(""); // 空行分隔
                lore.add(MessageUtils.colorize("&8━━━━━━━━━━━━━━━━━━━━"));
                lore.add(MessageUtils.colorize("&7消耗资源:"));
                lore.addAll(costLines);
            }
        }

        confirmMeta.setLore(lore);
        confirmButton.setItemMeta(confirmMeta);
        gui.setItem(config.getSlotConfirm(), confirmButton);
    }

    // 执行强化逻辑（在监听器中调用）
    public void applyReinforce(Player player, Inventory gui) {
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
            MessageUtils.sendError(player, "reinforce.invalid-item", "&c✗ &f请放置有效的MMO物品作为武器！");
            return;
        }

        // 检查是否可强化
        if (!Utils.isReinforceable(weapon)) {
            MessageUtils.sendError(player, "reinforce.not-reinforceable", "&c✗ &f此物品不可强化！");
            return;
        }

        // 检查消耗（如果启用了经济系统）
        if (economy != null && config.getCostVaultGold() > 0) {
            if (getBalance(player) < config.getCostVaultGold()) {
                MessageUtils.sendError(player, "reinforce.insufficient-gold", "&c✗ &f金币不足！需要: &e{cost}", 
                    "cost", MessageUtils.formatNumber(config.getCostVaultGold()));
                return;
            }
        }
        if (playerPointsAPI != null && config.getCostPlayerPoints() > 0) {
            if (getPlayerPoints(player.getUniqueId()) < config.getCostPlayerPoints()) {
                MessageUtils.sendError(player, "reinforce.insufficient-points", "&c✗ &f点券不足！需要: &e{cost}", 
                    "cost", MessageUtils.formatNumber(config.getCostPlayerPoints()));
                return;
            }
        }
        int materialCount = (int) Arrays.stream(materials).filter(item -> item != null).count();
        if (materialCount < config.getCostMaterials()) {
            MessageUtils.sendError(player, "reinforce.insufficient-materials", "&c✗ &f强化材料不足！需要: &e{count} &f个", 
                "count", MessageUtils.formatNumber(config.getCostMaterials()));
            return;
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
        if (protectCharm != null) gui.setItem(config.getSlotProtectCharm(), null);  // 消耗保护符
        if (enhanceCharm != null) gui.setItem(config.getSlotEnhanceCharm(), null);  // 消耗强化符

        // 计算概率（基础 + 强化符效果）
        int currentLevel = Utils.getCurrentLevel(weapon);
        double baseSuccessRate = config.getSuccessRateForLevel(currentLevel + 1);  // 针对下一级
        if (enhanceCharm != null) baseSuccessRate += 0.1;  // 示例：强化符加0.1概率
        double failDegradeChance = config.getReinforceFailDegradeChance();
        double maintainChance = config.getReinforceMaintainChance();
        if (protectCharm != null) failDegradeChance = 0.0;  // 保护符：失败不降级

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
        } catch (Exception e) {
            MessageUtils.sendError(player, "reinforce.error", "&c✗ &f强化过程中发生错误: &e{error}", "error", e.getMessage());
            MessageUtils.logError("强化过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}