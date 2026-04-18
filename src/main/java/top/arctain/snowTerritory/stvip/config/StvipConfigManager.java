package top.arctain.snowTerritory.stvip.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stvip.data.StvipTier;
import top.arctain.snowTerritory.utils.ConfigUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** 加载 plugins/SnowTerritory/stvip 下 config.yml 与 messages/zh_CN.yml。 */
public class StvipConfigManager {

    private final Main plugin;
    private final File baseDir;
    private File configFile;
    private FileConfiguration config;
    private List<StvipTier> tiers = new ArrayList<>();
    private boolean joinMessageEnabled;
    private String joinMessage = "";
    private List<Integer> remainDaysThresholds = List.of(7, 3, 2, 1);

    public StvipConfigManager(Main plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "stvip");
    }

    public void loadAll() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 stvip 目录失败: " + baseDir.getAbsolutePath());
        }
        File msgDir = new File(baseDir, "messages");
        if (!msgDir.exists() && !msgDir.mkdirs()) {
            MessageUtils.logWarning("创建 stvip/messages 目录失败: " + msgDir.getAbsolutePath());
        }
        File messagePack = new File(msgDir, "zh_CN.yml");
        ConfigUtils.copyResourceIfMissing(plugin, "stvip/messages/zh_CN.yml", messagePack);
        this.configFile = new File(baseDir, "config.yml");
        ConfigUtils.copyResourceIfMissing(plugin, "stvip/config.yml", configFile);
        this.config = YamlConfiguration.loadConfiguration(configFile);
        parseTiers();
        ConfigurationSection join = config.getConfigurationSection("join-message");
        if (join != null) {
            joinMessageEnabled = join.getBoolean("enabled", false);
            joinMessage = join.getString("message", "");
            List<Integer> loaded = join.getIntegerList("remain-days-thresholds");
            if (loaded == null || loaded.isEmpty()) {
                remainDaysThresholds = List.of(7, 3, 2, 1);
            } else {
                remainDaysThresholds = loaded.stream().filter(v -> v > 0).distinct().sorted().collect(Collectors.toList());
            }
        } else {
            joinMessageEnabled = false;
            joinMessage = "";
            remainDaysThresholds = List.of(7, 3, 2, 1);
        }
    }

    private void parseTiers() {
        tiers = new ArrayList<>();
        ConfigurationSection root = config.getConfigurationSection("tiers");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            String perm = sec.getString("permission", "");
            if (perm.isEmpty()) {
                MessageUtils.logWarning("stvip 档位 " + key + " 缺少 permission，已跳过");
                continue;
            }
            String display = sec.getString("display-name", key);
            int priority = sec.getInt("priority", 0);
            ConfigurationSection perks = sec.getConfigurationSection("perks");
            double rMult = 1.0;
            double fMult = 1.0;
            int extraSlots = 0;
            int extraPerMax = 0;
            double armorMult = 1.0;
            int remoteClaimLimit = 0;
            int freeClaimLimit = 0;
            int minDifficultyExclusive = 0;
            boolean bountyPreannounce = false;
            if (perks != null) {
                rMult = perks.getDouble("reinforce-cost-multiplier", 1.0);
                fMult = perks.getDouble("fish-sell-multiplier", 1.0);
                extraSlots = perks.getInt("loot-extra-slots", 0);
                extraPerMax = perks.getInt("loot-extra-per-item-max", 0);
                armorMult = perks.getDouble("armor-cost-multiplier", 1.0);
                remoteClaimLimit = perks.getInt("quest-daily-remote-claim-limit", 0);
                freeClaimLimit = perks.getInt("quest-daily-free-claim-limit", 0);
                minDifficultyExclusive = perks.getInt("quest-min-difficulty-exclusive", 0);
                bountyPreannounce = perks.getBoolean("bounty-preannounce", false);
            }
            tiers.add(new StvipTier(key, perm, display, priority, rMult, fMult, extraSlots, extraPerMax,
                    armorMult, remoteClaimLimit, freeClaimLimit, minDifficultyExclusive, bountyPreannounce));
        }
        tiers.sort(Comparator.comparingInt(StvipTier::getPriority));
    }

    /** 玩家拥有的最高 priority 档位（同 priority 时后加载的覆盖，通常应避免重复 priority）。 */
    public Optional<StvipTier> resolveTier(Player player) {
        return tiers.stream()
                .filter(t -> player.hasPermission(t.getPermission()))
                .max(Comparator.comparingInt(StvipTier::getPriority));
    }

    public Optional<StvipTier> getTierById(String tierId) {
        if (tierId == null || tierId.isBlank()) {
            return Optional.empty();
        }
        return tiers.stream()
                .filter(t -> t.getId().equalsIgnoreCase(tierId.trim()))
                .findFirst();
    }

    public boolean isJoinMessageEnabled() {
        return joinMessageEnabled;
    }

    public String getJoinMessage() {
        return joinMessage != null ? joinMessage : "";
    }

    public List<Integer> getRemainDaysThresholds() {
        return remainDaysThresholds;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public Map<String, String> getMessagesForMerge() {
        File messagePack = new File(baseDir, "messages/zh_CN.yml");
        if (!messagePack.exists()) {
            return Map.of();
        }
        return ConfigUtils.loadMessagesRecursive("messages.stvip", YamlConfiguration.loadConfiguration(messagePack));
    }
}
