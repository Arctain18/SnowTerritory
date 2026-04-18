package top.arctain.snowTerritory.quest.service.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import top.arctain.snowTerritory.quest.config.QuestConfigManager;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.util.Random;

/**
 * 默认悬赏任务调度器
 * 按配置的随机间隔发布悬赏任务
 */
public class DefaultBountyScheduler implements BountyScheduler {
    
    private static final int TICKS_PER_SECOND = 20;
    private static final int SECONDS_PER_MINUTE = 60;
    
    private final Plugin plugin;
    private final QuestConfigManager configManager;
    private final Runnable onPreview;
    private final Runnable onPublish;
    private final Random random;
    
    private int taskId = -1;
    private int previewTaskId = -1;
    
    public DefaultBountyScheduler(Plugin plugin, QuestConfigManager configManager, Runnable onPreview, Runnable onPublish) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.onPreview = onPreview;
        this.onPublish = onPublish;
        this.random = new Random();
    }
    
    @Override
    public void start() {
        stop();
        
        FileConfiguration bountyConfig = configManager.getBountyConfig();
        if (bountyConfig == null) {
            return;
        }
        
        int minIntervalTicks = minutesToTicks(bountyConfig.getInt("bounty.interval-min", 20));
        int maxIntervalTicks = minutesToTicks(bountyConfig.getInt("bounty.interval-max", 40));
        
        if (!isValidInterval(minIntervalTicks, maxIntervalTicks)) {
            MessageUtils.logWarning("悬赏任务间隔配置无效，已禁用悬赏任务发布");
            return;
        }
        
        scheduleNext(minIntervalTicks, maxIntervalTicks);
    }
    
    @Override
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (previewTaskId != -1) {
            Bukkit.getScheduler().cancelTask(previewTaskId);
            previewTaskId = -1;
        }
    }
    
    @Override
    public boolean isRunning() {
        return taskId != -1;
    }
    
    private void scheduleNext(int minInterval, int maxInterval) {
        int delay = minInterval + random.nextInt(maxInterval - minInterval + 1);
        int previewTicks = 5 * SECONDS_PER_MINUTE * TICKS_PER_SECOND;
        if (delay > previewTicks) {
            previewTaskId = Bukkit.getScheduler().runTaskLater(plugin, onPreview, delay - previewTicks).getTaskId();
        } else {
            previewTaskId = -1;
        }
        
        taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            previewTaskId = -1;
            onPublish.run();
            scheduleNext(minInterval, maxInterval);
        }, delay).getTaskId();
    }
    
    private int minutesToTicks(int minutes) {
        return minutes * SECONDS_PER_MINUTE * TICKS_PER_SECOND;
    }
    
    private boolean isValidInterval(int min, int max) {
        return min > 0 && max > 0 && min <= max;
    }
}

