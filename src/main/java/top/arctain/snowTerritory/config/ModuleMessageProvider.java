package top.arctain.snowTerritory.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.utils.ColorUtils;

import java.util.Map;
import java.util.Optional;

/** 模块消息提供者，支持占位符 {key} 与颜色解析。 */
public class ModuleMessageProvider {

    private final Map<String, FileConfiguration> packs;
    private final String defaultLang;

    public ModuleMessageProvider(Map<String, FileConfiguration> packs, String defaultLang) {
        this.packs = packs;
        this.defaultLang = defaultLang;
    }

    public String get(CommandSender sender, String path, String def) {
        return ColorUtils.colorize(getRaw(sender, path, def));
    }

    public String getRaw(CommandSender sender, String path, String def) {
        String lang = defaultLang;
        if (sender instanceof Player) {
            // 可扩展：读取玩家语言偏好
        }
        return Optional.ofNullable(packs.get(lang))
                .map(cfg -> cfg.getString(path, def))
                .orElse(def);
    }

    public String get(CommandSender sender, String path, String def, String... placeholders) {
        String message = getRaw(sender, path, def);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String k = placeholders[i];
                String v = placeholders[i + 1];
                message = message.replace("{" + k + "}", v).replace("%" + k + "%", v);
            }
        }
        return ColorUtils.colorize(message);
    }
}
