package top.arctain.snowTerritory.stvip.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import top.arctain.snowTerritory.Main;
import top.arctain.snowTerritory.stvip.data.StvipTier;
import top.arctain.snowTerritory.stvip.service.StvipService;

/** PlaceholderAPI：%stvip_tier_id%、%stvip_display%、%stvip_priority%、%stvip_has%。 */
public class StvipPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final StvipService service;

    public StvipPlaceholderExpansion(Main plugin, StvipService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public String getIdentifier() {
        return "stvip";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        return switch (params.toLowerCase()) {
            case "tier_id", "id" -> service.resolveTier(player).map(StvipTier::getId).orElse("");
            case "display", "display_name" -> service.resolveTier(player).map(StvipTier::getDisplayName).orElse("");
            case "priority", "level" -> service.resolveTier(player).map(t -> String.valueOf(t.getPriority())).orElse("0");
            case "has", "has_vip" -> service.hasAnyVip(player) ? "true" : "false";
            default -> null;
        };
    }
}
