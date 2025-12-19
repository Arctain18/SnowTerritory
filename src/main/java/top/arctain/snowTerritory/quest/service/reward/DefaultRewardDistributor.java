package top.arctain.snowTerritory.quest.service.reward;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.utils.QuestUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.HashMap;
import java.util.List;

/**
 * 默认奖励分发实现
 * 处理成就点、货币物品的发放及完成消息
 */
public class DefaultRewardDistributor implements RewardDistributor {
    
    private final QuestConfigManager configManager;
    
    public DefaultRewardDistributor(QuestConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public void distribute(Player player, Quest quest) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        QuestUtils.RewardCalculation calc = calculateReward(quest);
        giveQuestPoints(player, calc);
        giveCurrency(player, calc);
        sendCompletionMessage(player, quest, calc);
    }
    
    private QuestUtils.RewardCalculation calculateReward(Quest quest) {
        return QuestUtils.calculateReward(
                quest,
                configManager.getRewardsDefault(),
                configManager.getRewardsLevel(),
                configManager.getBonusTimeBonus(),
                configManager.getBountyConfig()
        );
    }
    
    private void giveQuestPoints(Player player, QuestUtils.RewardCalculation calc) {
        int questPoints = calc.getQuestPoint();
        if (questPoints <= 0) {
            return;
        }
        
        String command = String.format("qp give %s %d", player.getName(), questPoints);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
    
    private void giveCurrency(Player player, QuestUtils.RewardCalculation calc) {
        FileConfiguration rewardsDefault = configManager.getRewardsDefault();
        int baseCurrency = rewardsDefault.getInt("default.currency.amount", 1);
        double multiplier = calc.getLevelBonus() * calc.getBountyBonus() * calc.getTimeBonus();
        int totalCurrency = (int) Math.round(baseCurrency * multiplier);
        
        if (totalCurrency <= 0) {
            return;
        }
        
        List<QuestUtils.CurrencyStack> stacks = QuestUtils.distributeCurrency64Base(totalCurrency, rewardsDefault);
        String currencyType = calc.getCurrencyType();
        
        for (QuestUtils.CurrencyStack stack : stacks) {
            giveCurrencyStack(player, stack, currencyType);
        }
    }
    
    private void giveCurrencyStack(Player player, QuestUtils.CurrencyStack stack, String currencyType) {
        try {
            ItemStack item = createCurrencyItem(stack, currencyType);
            if (item == null) {
                return;
            }
            giveItemToPlayer(player, item);
        } catch (Exception e) {
            MessageUtils.logError("发放货币失败: " + e.getMessage());
        }
    }
    
    private ItemStack createCurrencyItem(QuestUtils.CurrencyStack stack, String currencyType) {
        MMOItem mmoitem = MMOItems.plugin.getMMOItem(
                MMOItems.plugin.getTypes().get(currencyType),
                stack.getItemId()
        );
        
        if (mmoitem == null) {
            MessageUtils.logWarning("货币物品不存在: " + currencyType + " " + stack.getItemId());
            return null;
        }
        
        ItemStack item = mmoitem.newBuilder().build();
        item.setAmount(stack.getCount());
        return item;
    }
    
    private void giveItemToPlayer(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
    
    private void sendCompletionMessage(Player player, Quest quest, QuestUtils.RewardCalculation calc) {
        FileConfiguration timeBonus = configManager.getBonusTimeBonus();
        FileConfiguration rewardsDefault = configManager.getRewardsDefault();
        
        String rating = QuestUtils.getTimeRatingDisplay(quest.getElapsedTime(), timeBonus);
        double multiplier = calc.getLevelBonus() * calc.getBountyBonus() * calc.getTimeBonus();
        
        int totalQuestPoint = (int) Math.round(rewardsDefault.getInt("default.questpoint", 12) * multiplier);
        int totalCurrency = (int) Math.round(rewardsDefault.getInt("default.currency.amount", 1) * multiplier);
        
        MessageUtils.sendConfigMessage(player, "quest.completion",
                "&a✓ &f任务完成！评级: &e{rating} &7(奖励倍数: {multiplier}x) &f获得: &b{questpoint} 成就点 &f+ &e{currency} 货币",
                "rating", rating,
                "multiplier", String.format("%.2f", multiplier),
                "questpoint", String.valueOf(totalQuestPoint),
                "currency", String.valueOf(totalCurrency));
    }
}

