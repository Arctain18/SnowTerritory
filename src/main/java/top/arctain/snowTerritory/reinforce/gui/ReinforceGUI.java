package top.arctain.snowTerritory.reinforce.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.reinforce.service.*;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.ColorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReinforceGUI {

    private final ReinforceConfigManager config;
    private final Main plugin;
    private final EconomyService economyService;     // Vault 经济服务
    private final PlayerPointsService playerPointsService; // PlayerPoints 服务
    private final CharmService charmService; // 符文判定服务
    private final MMOCoreService mmocoreService; // MMOCore 服务
    private final CostCalculationService costCalculationService; // 消耗计算服务
    private final ConfirmButtonLoreService confirmButtonLoreService; // 确认按钮 lore 服务
    private final ReinforceService reinforceService; // 强化核心服务

    public ReinforceGUI(ReinforceConfigManager config, Main plugin) {
        this.config = config;
        this.plugin = plugin;
        this.economyService = new EconomyService();
        this.playerPointsService = new PlayerPointsService();
        this.charmService = new CharmService(config);
        this.mmocoreService = new MMOCoreService();
        this.costCalculationService = new CostCalculationService(
            config, 
            config.getCostConfigManager(), 
            mmocoreService,
            new ExpressionService()
        );
        this.confirmButtonLoreService = new ConfirmButtonLoreService(
            config, 
            economyService, 
            playerPointsService, 
            charmService,
            costCalculationService,
            mmocoreService
        );
        this.reinforceService = new ReinforceService(
            config, 
            plugin, 
            economyService, 
            playerPointsService, 
            charmService, 
            confirmButtonLoreService,
            costCalculationService
        );
    }

    public void openGUI(Player player) {
        // 从配置创建GUI
        Inventory gui = Bukkit.createInventory(null, config.getGuiSize(), ColorUtils.colorize(config.getGuiTitle()));

        // 添加自定义槽位（装饰等） - 复用通用工具
        top.arctain.snowTerritory.utils.GuiSlotUtils.applySlotItems(gui, config.getCustomSlots(), true);

        // 添加确认和取消按钮（可点击）
        ItemStack confirmButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(MessageUtils.colorize(config.getGuiConfirmButtonName()));
            confirmMeta.setLore(Collections.singletonList(MessageUtils.colorize(config.getConfirmButtonLoreClickHint())));
            confirmButton.setItemMeta(confirmMeta);
        }
        gui.setItem(config.getSlotConfirm(), confirmButton);

        ItemStack cancelButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(MessageUtils.colorize(config.getGuiCancelButtonName()));
            cancelMeta.setLore(Collections.singletonList(MessageUtils.colorize("&8▸ 点击此处取消操作")));
            cancelButton.setItemMeta(cancelMeta);
        }
        gui.setItem(config.getSlotCancel(), cancelButton);

        // 添加玩家信息显示
        updatePlayerInfo(player, gui);

        // 空槽位已由玩家放置，无需预填充
        player.openInventory(gui);
    }

    /**
     * 更新玩家信息显示
     */
    public void updatePlayerInfo(Player player, Inventory gui) {
        if (!config.isPlayerInfoEnabled()) {
            return;
        }

        int slot = config.getPlayerInfoSlot();
        if (slot < 0 || slot >= gui.getSize()) {
            return;
        }

        // 获取玩家信息
        String playerName = player.getName();
        int playerLevel = mmocoreService.getPlayerLevel(player);
        String className = mmocoreService.getClassName(player);
        double gold = economyService.getBalance(player);
        int points = playerPointsService.getPoints(player.getUniqueId());

        // 创建显示物品
        ItemStack infoItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = infoItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.colorize(config.getPlayerInfoTitle()));
            
            // 构建lore
            List<String> lore = new ArrayList<>();
            for (String line : config.getPlayerInfoFormat()) {
                String formatted = line
                    .replace("{playerName}", playerName)
                    .replace("{playerLevel}", String.valueOf(playerLevel))
                    .replace("{className}", className != null ? className : "无")
                    .replace("{gold}", MessageUtils.formatNumber((long) gold))
                    .replace("{points}", MessageUtils.formatNumber(points));
                lore.add(MessageUtils.colorize(formatted));
            }
            meta.setLore(lore);
            infoItem.setItemMeta(meta);
        }

        gui.setItem(slot, infoItem);
    }

    public void updateConfirmButtonLore(Player player, Inventory gui) {
        confirmButtonLoreService.updateConfirmButtonLore(player, gui);
    }

    public void applyReinforce(Player player, Inventory gui) {
        reinforceService.applyReinforce(player, gui);
    }
}

