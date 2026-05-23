package me.bogatir.orbitalstrike.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Captures the next chat message typed by a specific player and routes it
 * to a registered callback.  The message is cancelled (not broadcast) and
 * the callback is executed on the main thread.
 *
 * <p>Usage:
 * <pre>{@code
 * chatInputListener.expectInput(player, value -> {
 *     // value is the raw string the player typed
 *     // runs on main thread
 * });
 * }</pre>
 */
public class ChatInputListener implements Listener {

    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    /**
     * Registers a one-shot chat callback for the given player.
     * Any previous pending callback for the same player is replaced.
     */
    public void expectInput(Player player, Consumer<String> callback) {
        pending.put(player.getUniqueId(), callback);
    }

    /** Removes a pending callback (e.g. when player disconnects). */
    public void cancelPending(Player player) {
        pending.remove(player.getUniqueId());
    }

    public boolean hasPending(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    // ─── Event ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Consumer<String> callback = pending.remove(uuid);
        if (callback == null) return;

        // Cancel the chat message so it is not broadcast
        event.setCancelled(true);

        // Convert Adventure Component → plain String
        String message = PlainTextComponentSerializer.plainText()
                             .serialize(event.message()).trim();

        if (message.equalsIgnoreCase("cancel")) {
            event.getPlayer().sendMessage("§7Input cancelled.");
            return;
        }

        // Execute callback on main thread (Bukkit API is not thread-safe)
        event.getPlayer().getServer().getScheduler().runTask(
            event.getPlayer().getServer().getPluginManager()
                 .getPlugin("BogatirOrbitalStrike"),
            () -> callback.accept(message)
        );
    }
}
