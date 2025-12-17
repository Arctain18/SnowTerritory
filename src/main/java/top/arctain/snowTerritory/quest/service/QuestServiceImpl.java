package top.arctain.snowTerritory.quest.service;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestReleaseMethod;
import top.arctain.snowTerritory.quest.data.QuestStatus;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.utils.QuestUtils;
import top.arctain.snowTerritory.utils.ColorUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 任务服务实现
 */
public class QuestServiceImpl implements QuestService {

    private final Main plugin;
    private final QuestConfigManager configManager;
    
    // 玩家任务映射: playerId -> List<Quest>
    private final Map<UUID, List<Quest>> playerQuests = new ConcurrentHashMap<>();
    
    // 悬赏任务列表
    private final List<Quest> bountyQuests = Collections.synchronizedList(new ArrayList<>());
    
    // 悬赏任务调度器任务ID
    private int bountyTaskId = -1;
    
    public QuestServiceImpl(Main plugin, QuestConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void initialize() {
        startBountyScheduler();
    }

    @Override
    public void shutdown() {
        stopBountyScheduler();
        playerQuests.clear();
        bountyQuests.clear();
    }

    @Override
    public void reload() {
        stopBountyScheduler();
        configManager.loadAll();
        startBountyScheduler();
    }

    @Override
    public Quest acceptNormalQuest(Player player, QuestType type) {
        UUID playerId = player.getUniqueId();
        
        // 检查是否已有同类型的活跃任务
        Quest existing = getActiveQuest(playerId, type);
        if (existing != null) {
            return null; // 已有活跃任务
        }
        
        // 生成新任务
        Quest quest = generateQuest(playerId, type, QuestReleaseMethod.NORMAL);
        if (quest == null) {
            return null;
        }
        
        // 添加到玩家任务列表
        playerQuests.computeIfAbsent(playerId, k -> new ArrayList<>()).add(quest);
        
        return quest;
    }

    @Override
    public List<Quest> getActiveQuests(UUID playerId) {
        List<Quest> quests = playerQuests.getOrDefault(playerId, new ArrayList<>());
        // 过滤出活跃且未过期的任务
        return quests.stream()
                .filter(q -> q.getStatus() == QuestStatus.ACTIVE && !q.isExpired())
                .collect(Collectors.toList());
    }

    @Override
    public Quest getActiveQuest(UUID playerId, QuestType type) {
        List<Quest> quests = getActiveQuests(playerId);
        return quests.stream()
                .filter(q -> q.getType() == type)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean updateQuestProgress(UUID playerId, String materialKey, int amount) {
        if (updatePlayerQuestProgress(playerId, materialKey, amount)) {
            return true;
        }
        return updateBountyQuestProgress(materialKey, amount);
    }

    private boolean updatePlayerQuestProgress(UUID playerId, String materialKey, int amount) {
        List<Quest> quests = playerQuests.get(playerId);
        if (quests == null) {
            return false;
        }
        
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            if (!isMatchingMaterialQuest(quest, materialKey)) {
                continue;
            }
            
            Quest updated = updateQuestProgress(quest, amount);
            quests.set(i, updated);
            
            if (updated.isCompleted()) {
                completeQuest(playerId, updated.getQuestId());
            }
            
            return true;
        }
        
        return false;
    }

    /**
     * 更新悬赏任务进度
     */
    private boolean updateBountyQuestProgress(String materialKey, int amount) {
        synchronized (bountyQuests) {
            for (int i = 0; i < bountyQuests.size(); i++) {
                Quest quest = bountyQuests.get(i);
                if (!isMatchingMaterialQuest(quest, materialKey)) {
                    continue;
                }
                
                Quest updated = updateQuestProgress(quest, amount);
                bountyQuests.set(i, updated);
                return true;
            }
        }
        return false;
    }

    /**
     * 检查任务是否匹配材料任务条件
     */
    private boolean isMatchingMaterialQuest(Quest quest, String materialKey) {
        return quest.getStatus() == QuestStatus.ACTIVE
                && quest.getType() == QuestType.MATERIAL
                && quest.getMaterialKey().equals(materialKey)
                && !quest.isExpired();
    }

    /**
     * 更新任务进度并返回新任务对象
     */
    private Quest updateQuestProgress(Quest quest, int amount) {
        int newAmount = Math.min(quest.getCurrentAmount() + amount, quest.getRequiredAmount());
        return quest.withProgress(newAmount);
    }

    @Override
    public boolean completeQuest(UUID playerId, UUID questId) {
        List<Quest> quests = playerQuests.get(playerId);
        if (quests == null) {
            return false;
        }
        
        Quest quest = findQuestById(quests, questId);
        if (quest == null || quest.getStatus() != QuestStatus.ACTIVE) {
            return false;
        }
        
        if (!quest.isCompleted()) {
            return false;
        }
        
        quest.setStatus(QuestStatus.COMPLETED);
        distributeRewards(Bukkit.getPlayer(playerId), quest);
        return true;
    }

    /**
     * 根据任务ID查找任务
     */
    private Quest findQuestById(List<Quest> quests, UUID questId) {
        return quests.stream()
                .filter(q -> q.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Quest> getActiveBountyQuests() {
        synchronized (bountyQuests) {
            return bountyQuests.stream()
                    .filter(q -> q.getStatus() == QuestStatus.ACTIVE && !q.isExpired())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public boolean completeBountyQuest(Player player, UUID questId) {
        Quest quest = findBountyQuestById(questId);
        if (quest == null || quest.getStatus() != QuestStatus.ACTIVE) {
            return false;
        }
        
        if (!quest.isCompleted()) {
            return false;
        }
        
        distributeRewards(player, quest);
        return true;
    }

    @Override
    public int claimCompletedBountyQuests(Player player) {
        int claimed = 0;
        synchronized (bountyQuests) {
            for (Quest quest : bountyQuests) {
                if (quest.getStatus() == QuestStatus.ACTIVE && quest.isCompleted() && !quest.isExpired()) {
                    distributeRewards(player, quest);
                    quest.setStatus(QuestStatus.COMPLETED);
                    claimed++;
                }
            }
        }
        return claimed;
    }

    /**
     * 根据任务ID查找悬赏任务
     */
    private Quest findBountyQuestById(UUID questId) {
        synchronized (bountyQuests) {
            return bountyQuests.stream()
                    .filter(q -> q.getQuestId().equals(questId))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public void startBountyScheduler() {
        stopBountyScheduler();
        
        FileConfiguration bountyConfig = configManager.getBountyConfig();
        if (bountyConfig == null) {
            return;
        }
        
        int minInterval = convertMinutesToTicks(bountyConfig.getInt("bounty.interval-min", 20));
        int maxInterval = convertMinutesToTicks(bountyConfig.getInt("bounty.interval-max", 40));
        
        if (!isValidInterval(minInterval, maxInterval)) {
            MessageUtils.logWarning("悬赏任务间隔配置无效，已禁用悬赏任务发布");
            return;
        }
        
        scheduleNextBounty(minInterval, maxInterval);
    }

    /**
     * 将分钟转换为tick
     */
    private int convertMinutesToTicks(int minutes) {
        return minutes * 60 * 20;
    }

    /**
     * 验证间隔配置是否有效
     */
    private boolean isValidInterval(int minInterval, int maxInterval) {
        return minInterval > 0 && maxInterval > 0 && minInterval <= maxInterval;
    }

    @Override
    public void stopBountyScheduler() {
        if (bountyTaskId != -1) {
            Bukkit.getScheduler().cancelTask(bountyTaskId);
            bountyTaskId = -1;
        }
    }

    /**
     * 调度下一个悬赏任务
     */
    private void scheduleNextBounty(int minInterval, int maxInterval) {
        Random random = new Random();
        int delay = minInterval + random.nextInt(maxInterval - minInterval + 1);
        
        bountyTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            publishBountyQuest();
            scheduleNextBounty(minInterval, maxInterval); // 递归调度下一个
        }, delay).getTaskId();
    }

    /**
     * 发布悬赏任务
     */
    private void publishBountyQuest() {
        FileConfiguration bountyConfig = configManager.getBountyConfig();
        if (bountyConfig == null) {
            return;
        }
        
        QuestType type = determineBountyQuestType(bountyConfig);
        if (type == null) {
            return;
        }
        
        Quest bounty = generateQuest(null, type, QuestReleaseMethod.BOUNTY);
        if (bounty == null) {
            return;
        }
        
        addBountyQuest(bounty);
        broadcastBountyQuest(bounty);
    }

    /**
     * 确定悬赏任务类型
     */
    private QuestType determineBountyQuestType(FileConfiguration bountyConfig) {
        String allowedTypes = bountyConfig.getString("bounty.allowed-types", "MATERIAL");
        
        if (allowedTypes.equalsIgnoreCase("MATERIAL")) {
            return QuestType.MATERIAL;
        }
        
        if (allowedTypes.equalsIgnoreCase("KILL")) {
            // TODO: 实现击杀任务
            return null;
        }
        
        if (allowedTypes.equalsIgnoreCase("BOTH")) {
            QuestType type = new Random().nextBoolean() ? QuestType.MATERIAL : QuestType.KILL;
            if (type == QuestType.KILL) {
                // TODO: 实现击杀任务
                return null;
            }
            return type;
        }
        
        return QuestType.MATERIAL;
    }

    /**
     * 添加悬赏任务到列表
     */
    private void addBountyQuest(Quest quest) {
        synchronized (bountyQuests) {
            bountyQuests.add(quest);
        }
    }

    /**
     * 广播悬赏任务
     */
    private void broadcastBountyQuest(Quest quest) {
        String message = formatBountyAnnouncement(quest);
        String colored = ColorUtils.colorize(message);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(colored);
        }
        
        MessageUtils.logInfo("悬赏任务已发布: " + quest.getMaterialKey() + " x" + quest.getRequiredAmount());
    }

    /**
     * 格式化悬赏任务公告
     */
    private String formatBountyAnnouncement(Quest quest) {
        String materialName = quest.getMaterialKey().split(":")[1]; // 提取物品名称
        return String.format("&6[悬赏任务] &e收集 %s x%d &7- &f完成任务可获得丰厚奖励！",
                materialName, quest.getRequiredAmount());
    }

    /**
     * 生成任务
     */
    private Quest generateQuest(UUID playerId, QuestType type, QuestReleaseMethod releaseMethod) {
        if (type == QuestType.MATERIAL) {
            return generateMaterialQuest(playerId, releaseMethod);
        } else if (type == QuestType.KILL) {
            // TODO: 实现击杀任务生成
            return null;
        }
        return null;
    }

    /**
     * 生成材料任务
     */
    private Quest generateMaterialQuest(UUID playerId, QuestReleaseMethod releaseMethod) {
        FileConfiguration whitelist = configManager.getMaterialsWhitelist();
        FileConfiguration tasksMaterial = configManager.getTasksMaterial();
        
        if (whitelist == null || tasksMaterial == null) {
            return null;
        }
        
        ConfigurationSection materialsSection = whitelist.getConfigurationSection("materials");
        if (materialsSection == null) {
            return null;
        }
        
        MaterialSelection selection = collectMaterials(materialsSection);
        if (selection.isEmpty()) {
            return null;
        }
        
        String selectedKey = selectRandomMaterial(selection);
        MaterialInfo info = selection.getInfo(selectedKey);
        
        int requiredAmount = generateRandomAmount(info);
        int level = QuestUtils.calculateQuestLevel(info.materialLevel, requiredAmount);
        long timeLimit = tasksMaterial.getLong("material.default-time-limit", 3600000);
        
        return createQuest(playerId, releaseMethod, selectedKey, requiredAmount, timeLimit, level);
    }

    /**
     * 收集材料信息
     */
    private MaterialSelection collectMaterials(ConfigurationSection materialsSection) {
        List<String> materialKeys = new ArrayList<>();
        Map<String, MaterialInfo> materialInfos = new HashMap<>();
        
        for (String type : materialsSection.getKeys(false)) {
            ConfigurationSection typeSection = materialsSection.getConfigurationSection(type);
            if (typeSection == null) {
                continue;
            }
            
            collectMaterialsFromType(type, typeSection, materialKeys, materialInfos);
        }
        
        return new MaterialSelection(materialKeys, materialInfos);
    }

    /**
     * 从类型中收集材料
     */
    private void collectMaterialsFromType(String type, ConfigurationSection typeSection,
                                          List<String> materialKeys, Map<String, MaterialInfo> materialInfos) {
        for (String name : typeSection.getKeys(false)) {
            String key = type + ":" + name;
            materialKeys.add(key);
            
            ConfigurationSection itemSection = typeSection.getConfigurationSection(name);
            MaterialInfo info = extractMaterialInfo(itemSection);
            materialInfos.put(key, info);
        }
    }

    /**
     * 提取材料信息
     */
    private MaterialInfo extractMaterialInfo(ConfigurationSection itemSection) {
        int min = itemSection != null ? itemSection.getInt("min", 16) : 16;
        int max = itemSection != null ? itemSection.getInt("max", 256) : 256;
        int materialLevel = itemSection != null ? itemSection.getInt("material-level", 1) : 1;
        return new MaterialInfo(min, max, materialLevel);
    }

    /**
     * 随机选择材料
     */
    private String selectRandomMaterial(MaterialSelection selection) {
        Random random = new Random();
        return selection.getRandomKey(random);
    }

    /**
     * 生成随机数量
     */
    private int generateRandomAmount(MaterialInfo info) {
        Random random = new Random();
        return info.min + random.nextInt(info.max - info.min + 1);
    }

    /**
     * 创建任务对象
     */
    private Quest createQuest(UUID playerId, QuestReleaseMethod releaseMethod,
                             String materialKey, int requiredAmount, long timeLimit, int level) {
        UUID questId = UUID.randomUUID();
        long startTime = System.currentTimeMillis();
        
        return new Quest(questId, playerId, QuestType.MATERIAL, releaseMethod,
                materialKey, requiredAmount, 0, startTime, timeLimit, level, QuestStatus.ACTIVE);
    }

    /**
     * 分发奖励
     */
    private void distributeRewards(Player player, Quest quest) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        QuestUtils.RewardCalculation calc = calculateReward(quest);
        giveQuestPoints(player, calc);
        giveCurrency(player, calc);
        sendCompletionMessage(player, quest, calc);
    }

    /**
     * 计算奖励
     */
    private QuestUtils.RewardCalculation calculateReward(Quest quest) {
        FileConfiguration rewardsDefault = configManager.getRewardsDefault();
        FileConfiguration rewardsLevel = configManager.getRewardsLevel();
        FileConfiguration timeBonus = configManager.getBonusTimeBonus();
        FileConfiguration bountyConfig = configManager.getBountyConfig();
        
        return QuestUtils.calculateReward(quest, rewardsDefault, rewardsLevel, timeBonus, bountyConfig);
    }

    /**
     * 发放成就点数
     */
    private void giveQuestPoints(Player player, QuestUtils.RewardCalculation calc) {
        if (calc.getQuestPoint() <= 0) {
            return;
        }
        
        String command = String.format("qp give %s %d", player.getName(), calc.getQuestPoint());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * 分发货币
     */
    private void giveCurrency(Player player, QuestUtils.RewardCalculation calc) {
        FileConfiguration rewardsDefault = configManager.getRewardsDefault();
        int baseCurrency = rewardsDefault.getInt("default.currency.amount", 1);
        int totalCurrency = calculateTotalCurrency(baseCurrency, calc);
        
        if (totalCurrency <= 0) {
            return;
        }
        
        List<QuestUtils.CurrencyStack> stacks = QuestUtils.distributeCurrency64Base(totalCurrency, rewardsDefault);
        String currencyType = calc.getCurrencyType();
        
        for (QuestUtils.CurrencyStack stack : stacks) {
            giveCurrencyStack(player, stack, currencyType);
        }
    }

    /**
     * 计算总货币数量
     */
    private int calculateTotalCurrency(int baseCurrency, QuestUtils.RewardCalculation calc) {
        double multiplier = calc.getLevelBonus() * calc.getBountyBonus() * calc.getTimeBonus();
        return (int) Math.round(baseCurrency * multiplier);
    }

    /**
     * 发放货币堆叠
     */
    private void giveCurrencyStack(Player player, QuestUtils.CurrencyStack stack, String currencyType) {
        try {
            ItemStack item = createCurrencyItem(stack, currencyType);
            if (item == null) {
                return;
            }
            
            giveItemToPlayer(player, item);
        } catch (Exception e) {
            MessageUtils.logError("发放货币失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建货币物品
     */
    private ItemStack createCurrencyItem(QuestUtils.CurrencyStack stack, String currencyType) {
        net.Indyuce.mmoitems.api.Type mmoType = MMOItems.plugin.getTypes().get(currencyType);
        if (mmoType == null) {
            MessageUtils.logWarning("货币类型不存在: " + currencyType);
            return null;
        }
        
        MMOItem mmoItem = MMOItems.plugin.getMMOItem(mmoType, stack.getItemId());
        if (mmoItem == null) {
            MessageUtils.logWarning("货币物品不存在: " + stack.getItemId());
            return null;
        }
        
        ItemStack item = mmoItem.newBuilder().build();
        item.setAmount(stack.getCount());
        return item;
    }

    /**
     * 给予玩家物品
     */
    private void giveItemToPlayer(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (leftover.isEmpty()) {
            return;
        }
        
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    /**
     * 发送完成消息
     */
    private void sendCompletionMessage(Player player, Quest quest, QuestUtils.RewardCalculation calc) {
        FileConfiguration timeBonus = configManager.getBonusTimeBonus();
        String rating = QuestUtils.getTimeRatingDisplay(quest.getElapsedTime(), timeBonus);
        double multiplier = calc.getLevelBonus() * calc.getBountyBonus() * calc.getTimeBonus();
        
        String message = String.format("&a✓ &f任务完成！评级: &e%s &7(奖励倍数: %.2fx)", rating, multiplier);
        player.sendMessage(ColorUtils.colorize(message));
    }

    /**
     * 材料信息
     */
    private static class MaterialInfo {
        final int min;
        final int max;
        final int materialLevel;

        MaterialInfo(int min, int max, int materialLevel) {
            this.min = min;
            this.max = max;
            this.materialLevel = materialLevel;
        }
    }

    /**
     * 材料选择结果
     */
    private static class MaterialSelection {
        private final List<String> materialKeys;
        private final Map<String, MaterialInfo> materialInfos;

        MaterialSelection(List<String> materialKeys, Map<String, MaterialInfo> materialInfos) {
            this.materialKeys = materialKeys;
            this.materialInfos = materialInfos;
        }

        boolean isEmpty() {
            return materialKeys.isEmpty();
        }

        String getRandomKey(Random random) {
            return materialKeys.get(random.nextInt(materialKeys.size()));
        }

        MaterialInfo getInfo(String key) {
            return materialInfos.get(key);
        }
    }
}

