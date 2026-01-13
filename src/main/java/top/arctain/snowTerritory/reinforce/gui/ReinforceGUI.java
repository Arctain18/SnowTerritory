package top.arctain.snowTerritory.reinforce.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.reinforce.config.ReinforceConfigManager;
import top.arctain.snowTerritory.reinforce.service.EconomyService;
import top.arctain.snowTerritory.reinforce.service.PlayerPointsService;
import top.arctain.snowTerritory.reinforce.service.CharmService;
import top.arctain.snowTerritory.utils.MessageUtils;
import top.arctain.snowTerritory.utils.ColorUtils;

import java.util.Collections;

public class ReinforceGUI {

    private final ReinforceConfigManager config;
    private final Main plugin;
    private final EconomyService economyService;     // Vault 经济服务
    private final PlayerPointsService playerPointsService; // PlayerPoints 服务
    private final CharmService charmService; // 符文判定服务
    private final ConfirmButtonLoreService confirmButtonLoreService; // 确认按钮 lore 服务
    private final ReinforceService reinforceService; // 强化核心服务

    public ReinforceGUI(ReinforceConfigManager config, Main plugin) {
        this.config = config;
        this.plugin = plugin;
        this.economyService = new EconomyService();
        this.playerPointsService = new PlayerPointsService();
        this.charmService = new CharmService(config);
        this.confirmButtonLoreService = new ConfirmButtonLoreService(config, economyService, playerPointsService, charmService);
        this.reinforceService = new ReinforceService(config, plugin, economyService, playerPointsService, charmService, confirmButtonLoreService);
    }

    public void openGUI(Player player) {
        // 从配置创建GUI
        Inventory gui = Bukkit.createInventory(null, config.getGuiSize(), ColorUtils.colorize(config.getGuiTitle()));

        // 添加自定义槽位（装饰等） - 复用通用工具
        top.arctain.snowTerritory.utils.GuiSlotUtils.applySlotItems(gui, config.getCustomSlots(), true);

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

    public void updateConfirmButtonLore(Player player, Inventory gui) {
        confirmButtonLoreService.updateConfirmButtonLore(player, gui);
    }

    public void applyReinforce(Player player, Inventory gui) {
        reinforceService.applyReinforce(player, gui);
    }
}

