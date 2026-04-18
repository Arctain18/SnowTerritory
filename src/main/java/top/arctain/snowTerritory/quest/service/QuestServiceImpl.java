package top.arctain.snowTerritory.quest.service;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.enderstorage.service.LootStorageService;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestDatabaseDao;
import top.arctain.snowTerritory.quest.data.QuestReleaseMethod;
import top.arctain.snowTerritory.quest.data.QuestStatus;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.service.generator.CollectQuestGenerator;
import top.arctain.snowTerritory.quest.service.generator.MaterialQuestGenerator;
import top.arctain.snowTerritory.quest.service.generator.QuestGenerationContext;
import top.arctain.snowTerritory.quest.service.generator.QuestGenerator;
import top.arctain.snowTerritory.quest.service.reward.DefaultRewardDistributor;
import top.arctain.snowTerritory.quest.service.reward.RewardDistributor;
import top.arctain.snowTerritory.quest.service.scheduler.BountyScheduler;
import top.arctain.snowTerritory.quest.service.scheduler.DefaultBountyScheduler;
import top.arctain.snowTerritory.stvip.service.StvipService;
import top.arctain.snowTerritory.utils.Utils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 任务服务实现
 * 
 * <p>职责：协调任务的生命周期管理，包括接取、进度更新、完成和奖励发放。
 * 具体的任务生成、奖励计算、调度等逻辑委托给专门的组件处理。
 */
public class QuestServiceImpl implements QuestService {
    
    private final QuestConfigManager configManager;
    private final QuestDatabaseDao databaseDao;
    
    // 可替换的组件
    private final Map<QuestType, QuestGenerator> generators;
    private final RewardDistributor rewardDistributor;
    private final BountyScheduler bountyScheduler;
    private final LootStorageService lootStorageService;
    private final StvipService stvipService;
    private final ZoneId zoneId = ZoneId.systemDefault();
    
    // 数据存储
    private final Map<UUID, List<Quest>> playerQuests = new ConcurrentHashMap<>();
    private final List<Quest> bountyQuests = Collections.synchronizedList(new ArrayList<>());
    private volatile Quest pendingBountyPreview;
    private final ThreadLocal<Double> progressRewardMultiplier = ThreadLocal.withInitial(() -> 1.0);
    
    public QuestServiceImpl(Main plugin, QuestConfigManager configManager, QuestDatabaseDao databaseDao) {
        this.configManager = configManager;
        this.databaseDao = databaseDao;
        
        // 初始化组件
        this.generators = initializeGenerators();
        this.rewardDistributor = new DefaultRewardDistributor(configManager);
        this.bountyScheduler = new DefaultBountyScheduler(plugin, configManager, this::previewBountyQuest, this::publishBountyQuest);
        this.lootStorageService = plugin != null && plugin.getEnderStorageModule() != null
                ? plugin.getEnderStorageModule().getLootStorageService() : null;
        this.stvipService = plugin != null ? plugin.getStvipService() : null;
    }
    
    /**
     * 用于测试的构造函数，允许注入依赖
     */
    QuestServiceImpl(Main plugin, QuestConfigManager configManager, QuestDatabaseDao databaseDao,
                     Map<QuestType, QuestGenerator> generators,
                     RewardDistributor rewardDistributor,
                     BountyScheduler bountyScheduler) {
        this.configManager = configManager;
        this.databaseDao = databaseDao;
        this.generators = generators;
        this.rewardDistributor = rewardDistributor;
        this.bountyScheduler = bountyScheduler;
        this.lootStorageService = plugin != null && plugin.getEnderStorageModule() != null
                ? plugin.getEnderStorageModule().getLootStorageService() : null;
        this.stvipService = plugin != null ? plugin.getStvipService() : null;
    }
    
    private Map<QuestType, QuestGenerator> initializeGenerators() {
        Map<QuestType, QuestGenerator> map = new EnumMap<>(QuestType.class);
        map.put(QuestType.MATERIAL, new MaterialQuestGenerator(configManager, databaseDao));
        map.put(QuestType.COLLECT, new CollectQuestGenerator(configManager, databaseDao));
        return map;
    }
    
    // ==================== 生命周期管理 ====================
    
    @Override
    public void initialize() {
        bountyScheduler.start();
    }
    
    @Override
    public void shutdown() {
        bountyScheduler.stop();
        playerQuests.clear();
        bountyQuests.clear();
    }
    
    @Override
    public void reload() {
        bountyScheduler.stop();
        configManager.loadAll();
        bountyScheduler.start();
    }
    
    // ==================== 普通任务操作 ====================
    
    @Override
    public Quest acceptNormalQuest(Player player, QuestType type) {
        Objects.requireNonNull(player, "player不能为null");
        Objects.requireNonNull(type, "type不能为null");
        
        UUID playerId = player.getUniqueId();
        
        // 检查是否已有同类型活跃任务
        if (getActiveQuest(playerId, type) != null) {
            return null;
        }
        
        QuestGenerationContext ctx = QuestGenerationContext.forVipMinDifficultyExclusive(
                resolveMinDifficultyExclusive(player));
        Quest quest = generateQuest(playerId, type, QuestReleaseMethod.NORMAL, ctx);
        if (quest == null) {
            return null;
        }
        
        playerQuests.computeIfAbsent(playerId, k -> new ArrayList<>()).add(quest);
        return quest;
    }
    
    @Override
    public List<Quest> getActiveQuests(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId不能为null");
        
        return playerQuests.getOrDefault(playerId, Collections.emptyList()).stream()
                .filter(this::isActiveAndNotExpired)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Quest> getAllQuests(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId不能为null");
        
        return new ArrayList<>(playerQuests.getOrDefault(playerId, Collections.emptyList()));
    }
    
    @Override
    public Quest getActiveQuest(UUID playerId, QuestType type) {
        Objects.requireNonNull(playerId, "playerId不能为null");
        Objects.requireNonNull(type, "type不能为null");
        
        return getActiveQuests(playerId).stream()
                .filter(q -> q.getType() == type)
                .findFirst()
                .orElse(null);
    }
    
    // ==================== 任务进度更新 ====================
    
    @Override
    public boolean updateQuestProgress(UUID playerId, String materialKey, int amount) {
        Objects.requireNonNull(materialKey, "materialKey不能为null");
        if (amount <= 0) {
            return false;
        }
        
        // 优先更新玩家个人任务
        if (playerId != null && updatePlayerQuestProgress(playerId, materialKey, amount)) {
            return true;
        }
        
        // 其次更新悬赏任务
        return updateBountyQuestProgress(materialKey, amount);
    }
    
    private boolean updatePlayerQuestProgress(UUID playerId, String materialKey, int amount) {
        List<Quest> quests = playerQuests.get(playerId);
        if (quests == null) {
            return false;
        }
        
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            if (!isMatchingQuest(quest, materialKey)) {
                continue;
            }
            
            Quest updated = applyProgress(quest, amount);
            quests.set(i, updated);
            
            if (updated.isCompleted()) {
                completeQuest(playerId, updated.getQuestId(), progressRewardMultiplier.get());
            }
            return true;
        }
        return false;
    }
    
    private boolean updateBountyQuestProgress(String materialKey, int amount) {
        synchronized (bountyQuests) {
            for (int i = 0; i < bountyQuests.size(); i++) {
                Quest quest = bountyQuests.get(i);
                if (!isMatchingQuest(quest, materialKey)) {
                    continue;
                }
                
                bountyQuests.set(i, applyProgress(quest, amount));
                return true;
            }
        }
        return false;
    }
    
    private boolean isMatchingQuest(Quest quest, String materialKey) {
        if (!isActiveAndNotExpired(quest) || !quest.getMaterialKey().equals(materialKey)) {
            return false;
        }
        return quest.getType() == QuestType.MATERIAL || quest.getType() == QuestType.COLLECT;
    }
    
    private Quest applyProgress(Quest quest, int amount) {
        int newAmount = Math.min(quest.getCurrentAmount() + amount, quest.getRequiredAmount());
        return quest.withProgress(newAmount);
    }
    
    @Override
    public boolean isActiveAndNotExpired(Quest quest) {
        return quest.getStatus() == QuestStatus.ACTIVE && !quest.isExpired();
    }
    
    // ==================== 任务完成 ====================
    
    @Override
    public boolean completeQuest(UUID playerId, UUID questId) {
        return completeQuest(playerId, questId, 1.0);
    }

    @Override
    public boolean completeQuest(UUID playerId, UUID questId, double rewardMultiplier) {
        Objects.requireNonNull(playerId, "playerId不能为null");
        Objects.requireNonNull(questId, "questId不能为null");
        
        List<Quest> quests = playerQuests.get(playerId);
        if (quests == null) {
            return false;
        }
        
        Quest quest = findQuestById(quests, questId);
        if (quest == null || quest.getStatus() != QuestStatus.ACTIVE || !quest.isCompleted()) {
            return false;
        }
        
        quest.setStatus(QuestStatus.COMPLETED);
        
        // 记录完成任务到数据库
        databaseDao.recordCompletedQuest(playerId, quest);
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            rewardDistributor.distribute(player, quest, rewardMultiplier);
        }
        return true;
    }
    
    private Quest findQuestById(List<Quest> quests, UUID questId) {
        return quests.stream()
                .filter(q -> q.getQuestId().equals(questId))
                .findFirst()
                .orElse(null);
    }
    
    // ==================== 悬赏任务操作 ====================
    
    @Override
    public List<Quest> getActiveBountyQuests() {
        synchronized (bountyQuests) {
            return bountyQuests.stream()
                    .filter(this::isActiveAndNotExpired)
                    .collect(Collectors.toList());
        }
    }
    
    @Override
    public boolean completeBountyQuest(Player player, UUID questId) {
        Objects.requireNonNull(player, "player不能为null");
        Objects.requireNonNull(questId, "questId不能为null");
        
        Quest quest = findBountyQuestById(questId);
        if (quest == null || quest.getStatus() != QuestStatus.ACTIVE || !quest.isCompleted()) {
            return false;
        }
        
        // 记录完成任务到数据库
        databaseDao.recordCompletedQuest(player.getUniqueId(), quest);
        
        rewardDistributor.distribute(player, quest);
        return true;
    }
    
    @Override
    public int claimCompletedBountyQuests(Player player) {
        return claimCompletedBountyQuests(player, 1.0);
    }

    @Override
    public int claimCompletedBountyQuests(Player player, double rewardMultiplier) {
        Objects.requireNonNull(player, "player不能为null");
        
        int claimed = 0;
        synchronized (bountyQuests) {
            for (Quest quest : bountyQuests) {
                if (isBountyQuestCompleted(quest)) {
                    // 记录完成任务到数据库
                    databaseDao.recordCompletedQuest(player.getUniqueId(), quest);
                    rewardDistributor.distribute(player, quest, rewardMultiplier);
                    claimed++;
                }
            }
        }
        return claimed;
    }

    private boolean isBountyQuestCompleted(Quest quest) {
        return quest.getStatus() == QuestStatus.COMPLETED && quest.getReleaseMethod() == QuestReleaseMethod.BOUNTY;
    }
    
    private Quest findBountyQuestById(UUID questId) {
        synchronized (bountyQuests) {
            return bountyQuests.stream()
                    .filter(q -> q.getQuestId().equals(questId))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public CompletionResult completeByCommand(Player player, boolean allowStorageSubmit, boolean freeMode, double rewardMultiplier) {
        Objects.requireNonNull(player, "player不能为null");
        int inventorySubmitted = 0;
        int storageSubmitted = 0;
        int completedNormal = 0;
        int boostedCompletions = 0;
        UUID playerId = player.getUniqueId();
        double prevMultiplier = progressRewardMultiplier.get();
        progressRewardMultiplier.set(rewardMultiplier);
        try {
            List<Quest> normalSnapshot = new ArrayList<>(getActiveQuests(playerId));
            for (Quest quest : normalSnapshot) {
                if (quest.getType() != QuestType.MATERIAL || !isActiveAndNotExpired(quest)) {
                    continue;
                }
                int before = quest.getCurrentAmount();
                int submittedInv = freeMode ? 0 : submitFromInventory(player, quest);
                int submittedStorage = freeMode ? 0 : submitFromStorage(playerId, quest, allowStorageSubmit);
                int submittedFree = freeMode ? submitFree(quest) : 0;
                inventorySubmitted += submittedInv;
                storageSubmitted += submittedStorage;
                Quest updated = getQuestById(playerId, quest.getQuestId());
                if (updated != null && updated.getStatus() == QuestStatus.COMPLETED) {
                    completedNormal++;
                    if (submittedFree > 0 || (freeMode && updated.getRequiredAmount() > before)) {
                        boostedCompletions++;
                    }
                }
            }

            if (!freeMode) {
                for (Quest bounty : getActiveBountyQuests()) {
                    if (bounty.getType() != QuestType.MATERIAL || !isActiveAndNotExpired(bounty)) {
                        continue;
                    }
                    submitFromInventory(player, bounty);
                    submitFromStorage(playerId, bounty, allowStorageSubmit);
                    Quest updated = findBountyQuestById(bounty.getQuestId());
                    if (updated != null && updated.isCompleted()) {
                        updated.setStatus(QuestStatus.COMPLETED);
                    }
                }
            } else {
                for (Quest bounty : getActiveBountyQuests()) {
                    if (bounty.getType() != QuestType.MATERIAL || !isActiveAndNotExpired(bounty)) {
                        continue;
                    }
                    submitFree(bounty);
                    Quest updated = findBountyQuestById(bounty.getQuestId());
                    if (updated != null && updated.isCompleted()) {
                        updated.setStatus(QuestStatus.COMPLETED);
                        boostedCompletions++;
                    }
                }
            }
        } finally {
            progressRewardMultiplier.set(prevMultiplier);
        }

        int claimedBounty = claimCompletedBountyQuests(player, rewardMultiplier);
        return new CompletionResult(inventorySubmitted, storageSubmitted, completedNormal, claimedBounty, boostedCompletions);
    }

    @Override
    public int getDailyRemoteClaimUsed(UUID playerId) {
        return databaseDao.getDailyUsage(playerId, currentDayKey()).remoteClaimUsed();
    }

    @Override
    public int getDailyFreeClaimUsed(UUID playerId) {
        return databaseDao.getDailyUsage(playerId, currentDayKey()).freeClaimUsed();
    }

    @Override
    public void incrementDailyRemoteClaimUsed(UUID playerId) {
        String day = currentDayKey();
        QuestDatabaseDao.DailyUsage usage = databaseDao.getDailyUsage(playerId, day);
        databaseDao.saveDailyUsage(playerId, day, usage.remoteClaimUsed() + 1, usage.freeClaimUsed());
    }

    @Override
    public void incrementDailyFreeClaimUsed(UUID playerId) {
        String day = currentDayKey();
        QuestDatabaseDao.DailyUsage usage = databaseDao.getDailyUsage(playerId, day);
        databaseDao.saveDailyUsage(playerId, day, usage.remoteClaimUsed(), usage.freeClaimUsed() + 1);
    }

    private Quest getQuestById(UUID playerId, UUID questId) {
        List<Quest> quests = playerQuests.get(playerId);
        if (quests == null) {
            return null;
        }
        for (Quest quest : quests) {
            if (quest.getQuestId().equals(questId)) {
                return quest;
            }
        }
        return null;
    }

    private int submitFromInventory(Player player, Quest quest) {
        int need = quest.getRequiredAmount() - quest.getCurrentAmount();
        if (need <= 0) {
            return 0;
        }
        int available = countInventoryByKey(player, quest.getMaterialKey());
        int toSubmit = Math.min(need, available);
        if (toSubmit <= 0) {
            return 0;
        }
        removeInventoryByKey(player, quest.getMaterialKey(), toSubmit);
        updateQuestProgress(player.getUniqueId(), quest.getMaterialKey(), toSubmit);
        return toSubmit;
    }

    private int submitFromStorage(UUID playerId, Quest quest, boolean allowStorageSubmit) {
        if (!allowStorageSubmit || lootStorageService == null) {
            return 0;
        }
        int latestCurrent = resolveCurrentAmount(playerId, quest);
        int need = quest.getRequiredAmount() - latestCurrent;
        if (need <= 0) {
            return 0;
        }
        int available = lootStorageService.getAmount(playerId, quest.getMaterialKey());
        int toSubmit = Math.min(need, available);
        if (toSubmit <= 0) {
            return 0;
        }
        if (!lootStorageService.consume(playerId, quest.getMaterialKey(), toSubmit)) {
            return 0;
        }
        updateQuestProgress(playerId, quest.getMaterialKey(), toSubmit);
        return toSubmit;
    }

    private int submitFree(Quest quest) {
        int need = quest.getRequiredAmount() - quest.getCurrentAmount();
        if (need <= 0) {
            return 0;
        }
        updateQuestProgress(quest.getPlayerId(), quest.getMaterialKey(), need);
        return need;
    }

    private int resolveCurrentAmount(UUID playerId, Quest quest) {
        if (quest.getReleaseMethod() == QuestReleaseMethod.BOUNTY) {
            Quest bounty = findBountyQuestById(quest.getQuestId());
            return bounty != null ? bounty.getCurrentAmount() : quest.getCurrentAmount();
        }
        Quest latest = getQuestById(playerId, quest.getQuestId());
        return latest != null ? latest.getCurrentAmount() : quest.getCurrentAmount();
    }

    private int countInventoryByKey(Player player, String materialKey) {
        int total = 0;
        for (org.bukkit.inventory.ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (!Utils.isMMOItem(stack)) {
                continue;
            }
            String key = top.arctain.snowTerritory.quest.utils.QuestUtils.getMMOItemKey(stack);
            if (materialKey.equals(key)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeInventoryByKey(Player player, String materialKey, int amount) {
        int remaining = amount;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            org.bukkit.inventory.ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (!Utils.isMMOItem(stack)) {
                continue;
            }
            String key = top.arctain.snowTerritory.quest.utils.QuestUtils.getMMOItemKey(stack);
            if (!materialKey.equals(key)) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            remaining -= take;
            int left = stack.getAmount() - take;
            if (left <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(left);
                player.getInventory().setItem(slot, stack);
            }
        }
    }

    private String currentDayKey() {
        return LocalDate.now(zoneId).toString();
    }
    
    // ==================== 悬赏调度 ====================
    
    @Override
    public void startBountyScheduler() {
        bountyScheduler.start();
    }
    
    @Override
    public void stopBountyScheduler() {
        bountyScheduler.stop();
    }
    
    /**
     * 悬赏预告（刷新前 5 分钟）
     */
    private void previewBountyQuest() {
        FileConfiguration bountyConfig = configManager.getBountyConfig();
        if (bountyConfig == null) {
            return;
        }
        QuestType type = determineBountyQuestType(bountyConfig);
        if (type == null) {
            return;
        }
        Quest preview = generateQuest(null, type, QuestReleaseMethod.BOUNTY, QuestGenerationContext.unconstrained());
        if (preview == null) {
            return;
        }
        pendingBountyPreview = preview;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (stvipService == null || !stvipService.canReceiveBountyPreannounce(online)) {
                continue;
            }
            MessageUtils.sendConfigRaw(online, "quest.bounty-vip3-preview",
                    "&3✦ &f悬赏预告：&e{material} &7x&e{amount} &8(5分钟后刷新)",
                    "material", preview.getMaterialName(), "amount", String.valueOf(preview.getRequiredAmount()));
        }
    }

    /**
     * 发布悬赏任务（由调度器回调）
     * 优先使用 5 分钟前预告缓存的 {@link #pendingBountyPreview}，避免与预告分两次随机任务类型导致不一致。
     */
    private void publishBountyQuest() {
        FileConfiguration bountyConfig = configManager.getBountyConfig();
        if (bountyConfig == null) {
            return;
        }
        Quest bounty = pendingBountyPreview;
        pendingBountyPreview = null;
        if (bounty == null) {
            QuestType type = determineBountyQuestType(bountyConfig);
            if (type == null) {
                return;
            }
            bounty = generateQuest(null, type, QuestReleaseMethod.BOUNTY, QuestGenerationContext.unconstrained());
        }
        if (bounty == null) {
            return;
        }
        addBountyQuest(bounty);
        broadcastBountyQuest(bounty);
    }
    
    private QuestType determineBountyQuestType(FileConfiguration config) {
        String allowedTypes = config.getString("bounty.allowed-types", "MATERIAL");
        String[] types = allowedTypes.toUpperCase().split("[, ]+");
        List<QuestType> valid = new ArrayList<>();
        for (String t : types) {
            QuestType qt = parseQuestType(t.trim());
            if (qt != null) {
                valid.add(qt);
            }
        }
        if (valid.isEmpty()) {
            return QuestType.MATERIAL;
        }
        return valid.get(new Random().nextInt(valid.size()));
    }

    private QuestType parseQuestType(String s) {
        return switch (s) {
            case "MATERIAL" -> QuestType.MATERIAL;
            case "COLLECT" -> QuestType.COLLECT;
            case "KILL" -> null;
            default -> null;
        };
    }
    
    private void addBountyQuest(Quest quest) {
        synchronized (bountyQuests) {
            // 移除旧的活跃悬赏任务，保持只有一个活跃悬赏
            bountyQuests.removeIf(q -> q.getStatus() == QuestStatus.ACTIVE);
            bountyQuests.add(quest);
        }
    }
    
    private void broadcastBountyQuest(Quest quest) {
        String questDesc = quest.getDescription();
        // 任务区域占位符（暂用，后续可接入实际区域配置）
        String areaPlaceholder = getBountyAreaPlaceholder(quest);
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendBountyAnnouncementWithHover(player, questDesc, areaPlaceholder);
        }
        MessageUtils.logInfo("悬赏任务已发布: " + questDesc);
    }

    /** 获取悬赏任务区域占位文本（暂用占位符，后续可接入实际区域配置） */
    private String getBountyAreaPlaceholder(Quest quest) {
        return "待配置";
    }
    
    // ==================== 任务生成 ====================
    
    private Quest generateQuest(UUID playerId, QuestType type, QuestReleaseMethod releaseMethod,
            QuestGenerationContext context) {
        QuestGenerator generator = generators.get(type);
        if (generator == null) {
            MessageUtils.logWarning("未找到任务类型的生成器: " + type);
            return null;
        }
        if (context == null) {
            context = QuestGenerationContext.unconstrained();
        }
        return generator.generate(playerId, type, releaseMethod, context);
    }

    private int resolveMinDifficultyExclusive(Player player) {
        if (stvipService == null || player == null) {
            return 0;
        }
        return Math.max(0, stvipService.getQuestMinDifficultyExclusive(player));
    }
}
