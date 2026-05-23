package me.bogatir.orbitalstrike.listeners;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Detects when a player places an Orbital Strike item and writes the
 * PDC marker onto the resulting BEACON tile-state so it survives reloads.
 */
public class BlockPlaceListener implements Listener {

    private final BogatirOrbitalStrike plugin;

    public BlockPlaceListener(BogatirOrbitalStrike plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Check that the item in hand is our tagged Orbital Strike item
        if (!plugin.getOrbitalStrikeBlock().isOrbitalStrikeItem(event.getItemInHand())) return;

        Block placed = event.getBlockPlaced();

        // Write PDC onto the placed beacon tile-state
        plugin.getOrbitalStrikeBlock().markBlock(placed);

        player.sendMessage("§5§l[Orbital Strike] §fBlock placed. Right-click to configure.");
    }
}
