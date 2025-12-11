package top.arctain.snowTerritory.enderstorage.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class MessageProvider {

    private final Map<String, FileConfiguration> packs;
    private final String defaultLang;

    public MessageProvider(Map<String, FileConfiguration> packs, String defaultLang) {
        this.packs = packs;
        this.defaultLang = defaultLang;
    }

    public String get(CommandSender sender, String path, String def) {
        String lang = defaultLang;
        if (sender instanceof Player) {
            // 可扩展读取玩家语言
        }
        return Optional.ofNullable(packs.get(lang))
                .map(cfg -> cfg.getString(path, def))
                .orElse(def);
    }
}

