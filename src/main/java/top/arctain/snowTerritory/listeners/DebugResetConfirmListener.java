package top.arctain.snowTerritory.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import top.arctain.snowTerritory.commands.DebugResetConfirmHandler;

public class DebugResetConfirmListener implements Listener {

    private final DebugResetConfirmHandler handler;

    public DebugResetConfirmListener(DebugResetConfirmHandler handler) {
        this.handler = handler;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (handler.tryConfirm(player, text)) {
            event.setCancelled(true);
        }
    }
}
