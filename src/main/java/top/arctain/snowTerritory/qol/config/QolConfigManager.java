package top.arctain.snowTerritory.qol.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.utils.ConfigUtils;
import top.arctain.snowTerritory.utils.MessageUtils;

import java.io.File;

/** 管理 qol 模块配置。 */
public class QolConfigManager {

    private final Main plugin;
    private final File baseDir;
    private boolean preventFarmlandTrample = true;
    private boolean noArrowRequired = true;

    public QolConfigManager(Main plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "qol");
    }

    public void loadAll() {
        ensureDefaults();
        loadMainConfig();
        MessageUtils.logSuccess("QOL 配置已加载，配置目录: plugins/SnowTerritory/qol/");
    }

    private void ensureDefaults() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            MessageUtils.logWarning("创建 qol 目录失败: " + baseDir.getAbsolutePath());
        }
        ConfigUtils.copyResourceIfMissing(plugin, "qol/config.yml", new File(baseDir, "config.yml"));
    }

    private void loadMainConfig() {
        FileConfiguration mainConfig = YamlConfiguration.loadConfiguration(new File(baseDir, "config.yml"));
        ConfigurationSection qol = mainConfig.getConfigurationSection("qol");
        if (qol == null) {
            preventFarmlandTrample = true;
            noArrowRequired = true;
            return;
        }
        this.preventFarmlandTrample = qol.getBoolean("prevent-farmland-trample", true);
        this.noArrowRequired = qol.getBoolean("no-arrow-required", true);
    }

    public boolean isPreventFarmlandTrample() {
        return preventFarmlandTrample;
    }

    public boolean isNoArrowRequired() {
        return noArrowRequired;
    }

    public File getBaseDir() {
        return baseDir;
    }
}
