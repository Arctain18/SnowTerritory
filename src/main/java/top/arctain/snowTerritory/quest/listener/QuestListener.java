package top.arctain.snowTerritory.quest.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.quest.config.MessageProvider;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.quest.data.Quest;
import top.arctain.snowTerritory.quest.data.QuestReleaseMethod;
import top.arctain.snowTerritory.quest.data.QuestStatus;
import top.arctain.snowTerritory.quest.data.QuestType;
import top.arctain.snowTerritory.quest.service.QuestService;
import top.arctain.snowTerritory.quest.utils.QuestUtils;
import top.arctain.snowTerritory.utils.ColorUtils;

import java.util.List;

/**
 * 任务监听器
 * 处理材料提交等事件
 */
public class QuestListener implements Listener {

    private final QuestService questService;
    private final MessageProvider messages;
    
    // 当前事件处理的上下文（事件处理器在单线程中同步执行，因此可以安全使用）
    private MaterialSubmissionContext currentContext;

    public QuestListener(Main plugin, QuestService questService, QuestConfigManager configManager, MessageProvider messages) {
        this.questService = questService;
        this.messages = messages;
    }

    /**
     * 材料提交上下文
     * 封装材料提交过程中需要的所有信息
     */
    private static class MaterialSubmissionContext {
        final Player player;
        final ItemStack item;
        final String itemKey;
        final String materialName;

        MaterialSubmissionContext(Player player, ItemStack item, String itemKey) {
            this.player = player;
            this.item = item;
            this.itemKey = itemKey;
            this.materialName = extractMaterialName(itemKey);
        }

        private String extractMaterialName(String itemKey) {
            String[] parts = itemKey.split(":");
            return parts.length > 1 ? parts[1] : itemKey;
        }
    }

    /**
     * 处理玩家交互事件（用于材料提交）
     * 玩家右键点击物品时，如果该物品匹配任务需求，则提交材料
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = getValidItem(player);
        if (item == null) {
            return;
        }

        String itemKey = QuestUtils.getMMOItemKey(item);
        if (itemKey == null) {
            return;
        }

        currentContext = new MaterialSubmissionContext(player, item, itemKey);
        try {
            Quest matchingQuest = findMatchingQuest();
            if (matchingQuest == null) {
                return;
            }

            if (!validateQuestState(matchingQuest)) {
                return;
            }

            int toSubmit = calculateSubmissionAmount(matchingQuest);
            if (toSubmit <= 0) {
                return;
            }

            submitMaterials(matchingQuest, toSubmit);
        } finally {
            // 清理上下文，避免内存泄漏
            currentContext = null;
        }
    }

    private ItemStack getValidItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return null;
        }
        if (!QuestUtils.isMMOItem(item)) {
            return null;
        }
        return item;
    }

    private Quest findMatchingQuest() {
        Quest quest = findInNormalQuests();
        if (quest != null) {
            return quest;
        }
        return findInBountyQuests();
    }

    private Quest findInNormalQuests() {
        List<Quest> activeQuests = questService.getActiveQuests(currentContext.player.getUniqueId());
        for (Quest quest : activeQuests) {
            if (isMaterialQuestMatching(quest)) {
                return quest;
            }
        }
        return null;
    }

    private Quest findInBountyQuests() {
        List<Quest> bountyQuests = questService.getActiveBountyQuests();
        for (Quest quest : bountyQuests) {
            if (isMaterialQuestMatching(quest)) {
                return quest;
            }
        }
        return null;
    }

    private boolean isMaterialQuestMatching(Quest quest) {
        return quest.getType() == QuestType.MATERIAL && quest.getMaterialKey().equals(currentContext.itemKey);
    }

    private boolean validateQuestState(Quest quest) {
        if (quest.isCompleted()) {
            currentContext.player.sendMessage(ColorUtils.colorize(messages.get(currentContext.player, "quest-already-completed", "&c✗ &f任务已完成")));
            return false;
        }
        if (quest.isExpired()) {
            currentContext.player.sendMessage(ColorUtils.colorize(messages.get(currentContext.player, "quest-expired", "&c✗ &f任务已过期")));
            return false;
        }
        return true;
    }

    private int calculateSubmissionAmount(Quest quest) {
        int itemAmount = currentContext.item.getAmount();
        int needed = quest.getRequiredAmount() - quest.getCurrentAmount();
        return Math.min(itemAmount, needed);
    }

    /**
     * 提交材料并更新任务进度
     */
    private void submitMaterials(Quest matchingQuest, int toSubmit) {
        boolean updated = questService.updateQuestProgress(currentContext.player.getUniqueId(), currentContext.itemKey, toSubmit);
        if (!updated) {
            return;
        }

        removeItemsFromHand(toSubmit);
        Quest updatedQuest = getUpdatedQuest(matchingQuest);
        sendProgressMessages(updatedQuest, toSubmit);
    }

    private void removeItemsFromHand(int amount) {
        if (currentContext.item.getAmount() == amount) {
            currentContext.player.getInventory().setItemInMainHand(null);
        } else {
            currentContext.item.setAmount(currentContext.item.getAmount() - amount);
        }
    }

    /**
     * 获取更新后的任务
     */
    private Quest getUpdatedQuest(Quest matchingQuest) {
        if (matchingQuest.getReleaseMethod() == QuestReleaseMethod.NORMAL) {
            return questService.getActiveQuest(currentContext.player.getUniqueId(), matchingQuest.getType());
        }
        return findBountyQuestById(matchingQuest.getQuestId());
    }

    /**
     * 根据任务ID查找悬赏任务
     */
    private Quest findBountyQuestById(java.util.UUID questId) {
        List<Quest> bounties = questService.getActiveBountyQuests();
        for (Quest quest : bounties) {
            if (quest.getQuestId().equals(questId)) {
                return quest;
            }
        }
        return null;
    }

    private void sendProgressMessages(Quest updatedQuest, int toSubmit) {
        if (updatedQuest == null) {
            return;
        }

        sendSubmissionMessage(updatedQuest, toSubmit);
        if (isBountyQuestCompleted(updatedQuest)) {
            sendBountyQuestCompletionMessage(updatedQuest);
            updatedQuest.setStatus(QuestStatus.COMPLETED);
        }
    }

    private void sendSubmissionMessage(Quest updatedQuest, int toSubmit) {
        String progressMsg = messages.get(currentContext.player, "material-submitted",
                "&a✓ &f已提交 &e%amount%x %item% &7(进度: %current%/%required%)");
        progressMsg = progressMsg.replace("%amount%", String.valueOf(toSubmit))
                .replace("%item%", currentContext.materialName)
                .replace("%current%", String.valueOf(updatedQuest.getCurrentAmount()))
                .replace("%required%", String.valueOf(updatedQuest.getRequiredAmount()));
        currentContext.player.sendMessage(ColorUtils.colorize(progressMsg));
    }

    private boolean isBountyQuestCompleted(Quest updatedQuest) {
        return updatedQuest.isCompleted() && updatedQuest.getReleaseMethod() == QuestReleaseMethod.BOUNTY;
    }

    private void sendBountyQuestCompletionMessage(Quest updatedQuest) {
        String msg = messages.get(currentContext.player, "bounty-quest-completed",
                "&a✓ &f悬赏任务完成！使用 &e/sn q complete &f领取奖励");
        currentContext.player.sendMessage(ColorUtils.colorize(msg));
    }
}

