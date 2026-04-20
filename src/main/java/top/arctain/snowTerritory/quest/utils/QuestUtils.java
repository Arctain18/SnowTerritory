package top.arctain.snowTerritory.quest.utils;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.Type;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestReleaseMethod;
import top.arctain.snowTerritory.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务工具类
 */
public class QuestUtils {

    public static boolean isMMOItem(ItemStack item) {
        return Utils.isMMOItem(item);
    }

    /**
     * 获取物品的 MMOItems key (TYPE:NAME格式)
     */
    public static String getMMOItemKey(ItemStack item) {
        if (!isMMOItem(item)) {
            return null;
        }
        try {
            Type type = MMOItems.getType(item);
            String id = MMOItems.getID(item);
            if (type != null && id != null && !id.isEmpty()) {
                return type.getId() + ":" + id;
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 根据 key (TYPE:NAME) 获取 MMOItem 的展示名称
     */
    public static String getMMOItemDisplayName(String key) {
        if (key == null || !key.contains(":")) {
            return null;
        }
        
        try {
            String[] parts = key.split(":", 2);
            if (parts.length != 2) {
                return null;
            }
            
            String typeId = parts[0];
            String itemId = parts[1];
            
            Type type = MMOItems.plugin.getTypes().get(typeId);
            if (type == null) {
                return null;
            }
            
            MMOItem mmoItem = MMOItems.plugin.getMMOItem(type, itemId);
            if (mmoItem == null) {
                return null;
            }
            
            ItemStack itemStack = mmoItem.newBuilder().build();
            return getMMOItemName(itemStack);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取物品的展示名称（含原版 / MMOItems 写入的颜色与样式）
     * 优先序列化 ItemMeta 的 Adventure 展示名，与物品栏中显示一致。
     */
    public static String getMMOItemName(ItemStack item) {
        if (!isMMOItem(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        if (meta.hasDisplayName()) {
            var comp = meta.displayName();
            if (comp != null) {
                String legacy = LegacyComponentSerializer.legacySection().serialize(comp);
                if (legacy != null && !legacy.isEmpty()) {
                    return legacy;
                }
            }
        }
        String displayName = meta.getDisplayName();
        return (displayName == null || displayName.isEmpty()) ? null : displayName;
    }

    /**
     * 匹配物品是否与指定的key匹配
     */
    public static boolean matchesItemKey(ItemStack item, String key) {
        String itemKey = getMMOItemKey(item);
        return itemKey != null && itemKey.equals(key);
    }

    /**
     * 计算任务等级（占位符算法，待以后实现）
     * 基于材料等级和数量
     */
    public static int calculateQuestLevel(int materialLevel, int quantity) {
        // TODO: 实现任务等级算法
        // 目前简单实现：基于材料等级和数量
        if (materialLevel <= 0) materialLevel = 1;
        if (quantity <= 0) quantity = 1;
        
        // 简单算法：等级 = 材料等级 + (数量/64)的整数部分
        int level = materialLevel + (quantity / 64);
        return Math.max(1, Math.min(level, 10)); // 限制在1-10级
    }

    /**
     * 计算任务难度
     * 根据需求数量计算难度(1-32)，32个等级均匀分布，从min到max线性映射
     */
    public static int calculateDifficulty(int requiredAmount, int min, int max) {
        if (min >= max) {
            return 1; // 如果min >= max，返回最低难度
        }
        
        // 边界处理
        if (requiredAmount <= min) {
            return 1;
        }
        if (requiredAmount >= max) {
            return 32;
        }
        
        // 线性映射：difficulty = 1 + (amount - min) * 31 / (max - min)
        double ratio = (double)(requiredAmount - min) / (max - min);
        int difficulty = 1 + (int)(ratio * 31.0);
        
        // 确保在1-32范围内
        return Math.max(1, Math.min(32, difficulty));
    }

    public static int calculateRequiredAmount(int difficulty, int min, int max) {
        return min + (int)((difficulty - 1) * (max - min) / 31.0);
    }

    /**
     * 计算时间奖励系数
     * 根据完成时间匹配time-bonus.yml中的评级
     */
    public static double calculateTimeBonus(long elapsedTime, FileConfiguration timeBonusConfig) {
        if (timeBonusConfig == null) {
            return 1.0;
        }

        // 按评级顺序查找（1=最好，数字越大越差）
        for (int rating = 1; rating <= 10; rating++) {
            String path = String.valueOf(rating);
            if (!timeBonusConfig.contains(path)) {
                continue;
            }
            
            long maxLimit = timeBonusConfig.getLong(path + ".max-limit", -1);
            if (maxLimit < 0) {
                // -1表示无限制，这是最差的评级
                double bonus = timeBonusConfig.getDouble(path + ".time-bonus", 0.8);
                return bonus;
            }
            
            if (elapsedTime <= maxLimit) {
                double bonus = timeBonusConfig.getDouble(path + ".time-bonus", 1.0);
                return bonus;
            }
        }
        
        // 如果都不匹配，返回默认值
        return 0.8;
    }

    /**
     * 获取时间评级的显示名称
     */
    public static String getTimeRatingDisplay(long elapsedTime, FileConfiguration timeBonusConfig) {
        if (timeBonusConfig == null) {
            return "Unknown";
        }

        for (int rating = 1; rating <= 10; rating++) {
            String path = String.valueOf(rating);
            if (!timeBonusConfig.contains(path)) {
                continue;
            }
            
            long maxLimit = timeBonusConfig.getLong(path + ".max-limit", -1);
            if (maxLimit < 0) {
                return timeBonusConfig.getString(path + ".display", "Poor");
            }
            
            if (elapsedTime <= maxLimit) {
                return timeBonusConfig.getString(path + ".display", "Normal");
            }
        }
        
        return "Poor";
    }

    /**
     * 计算任务奖励
     * 公式: basic * level-bonus * bounty-bonus(if valid) * time-bonus
     */
    public static RewardCalculation calculateReward(Quest quest, FileConfiguration rewardsDefault,
                                                     FileConfiguration rewardsLevel,
                                                     FileConfiguration timeBonusConfig,
                                                     FileConfiguration bountyConfig) {
        // 基础奖励
        int baseQuestPoint = rewardsDefault.getInt("default.questpoint", 12);
        String currencyType = rewardsDefault.getString("default.currency.type", "CURRENCY");
        
        // 等级系数
        double levelBonus = getLevelBonus(quest.getLevel(), quest.getDifficulty(), rewardsLevel);
        
        // 悬赏系数（如果是悬赏任务）
        double bountyBonus = 1.0;
        if (quest.getReleaseMethod() == QuestReleaseMethod.BOUNTY && bountyConfig != null) {
            bountyBonus = bountyConfig.getDouble("bounty.bounty-bonus", 1.5);
        }
        
        // 时间系数
        long elapsedTime = quest.getElapsedTime();
        double timeBonus = calculateTimeBonus(elapsedTime, timeBonusConfig);
        
        // 计算最终奖励
        int finalQuestPoint = (int) Math.round(baseQuestPoint * levelBonus * bountyBonus * timeBonus);
        
        return new RewardCalculation(finalQuestPoint, currencyType, levelBonus, bountyBonus, timeBonus);
    }

    /**
     * 获取等级系数
     * 公式: level * difficulty
     */
    private static double getLevelBonus(int level, int difficulty, FileConfiguration rewardsLevel) {
        // 使用 level * difficulty 作为等级系数
        return level * difficulty;
    }

    /**
     * 将货币数量转换为64进制分发列表
     * 例如: 324 → [5个stack-1, 4个stack-0]
     */
    public static List<CurrencyStack> distributeCurrency64Base(int totalAmount, FileConfiguration rewardsDefault) {
        List<CurrencyStack> result = new ArrayList<>();
        
        if (rewardsDefault == null) {
            return result;
        }
        
        // 从配置中读取所有stack定义
        Map<String, Object> currencyConfig = rewardsDefault.getConfigurationSection("default.currency").getValues(false);
        
        // 找出所有stack键并排序（从大到小）
        List<String> stackKeys = new ArrayList<>();
        for (String key : currencyConfig.keySet()) {
            if (key.startsWith("stack-")) {
                stackKeys.add(key);
            }
        }
        
        // 按stack编号排序（从大到小）
        stackKeys.sort((a, b) -> {
            int numA = Integer.parseInt(a.replace("stack-", ""));
            int numB = Integer.parseInt(b.replace("stack-", ""));
            return Integer.compare(numB, numA); // 降序
        });
        
        int remaining = totalAmount;
        
        // 从最大的stack开始分发
        for (String stackKey : stackKeys) {
            if (remaining <= 0) {
                break;
            }
            
            int stackValue = (int) Math.pow(64, Integer.parseInt(stackKey.replace("stack-", "")));
            int count = remaining / stackValue;
            
            if (count > 0) {
                String itemId = rewardsDefault.getString("default.currency." + stackKey);
                result.add(new CurrencyStack(stackKey, itemId, count));
                remaining = remaining % stackValue;
            }
        }
        
        return result;
    }

    /**
     * 货币堆叠信息
     */
    public static class CurrencyStack {
        private final String stackKey;
        private final String itemId;
        private final int count;

        public CurrencyStack(String stackKey, String itemId, int count) {
            this.stackKey = stackKey;
            this.itemId = itemId;
            this.count = count;
        }

        public String getStackKey() {
            return stackKey;
        }

        public String getItemId() {
            return itemId;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * 奖励计算结果
     */
    public static class RewardCalculation {
        private final int questPoint;
        private final String currencyType;
        private final double levelBonus;
        private final double bountyBonus;
        private final double timeBonus;

        public RewardCalculation(int questPoint, String currencyType, double levelBonus, double bountyBonus, double timeBonus) {
            this.questPoint = questPoint;
            this.currencyType = currencyType;
            this.levelBonus = levelBonus;
            this.bountyBonus = bountyBonus;
            this.timeBonus = timeBonus;
        }

        public int getQuestPoint() {
            return questPoint;
        }

        public String getCurrencyType() {
            return currencyType;
        }

        public double getLevelBonus() {
            return levelBonus;
        }

        public double getBountyBonus() {
            return bountyBonus;
        }

        public double getTimeBonus() {
            return timeBonus;
        }
    }
}

