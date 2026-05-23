package me.bogatir.orbitalstrike.listeners;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Opens the Orbital Strike GUI when a player right-clicks
 * a tagged Orbital Strike block (BEACON with PDC marker).
 */
public class BlockInteractListener implements Listener {

    private final BogatirOrbitalStrike plugin;

    public BlockInteractListener(BogatirOrbitalStrike plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        // Only right-clicks on a block, main hand only (avoid double-fire)
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!plugin.getOrbitalStrikeBlock().isOrbitalStrikeBlock(block)) return;

        Player player = event.getPlayer();

        // Permission check
        if (!player.hasPermission("orbitalstrike.use")) {
            player.sendMessage("§cYou don't have permission to use this block.");
            event.setCancelled(true);
            return;
        }

        // Cancel the vanilla beacon GUI from opening
        event.setCancelled(true);

        plugin.getStrikeManager().getGUI().open(player, block.getWorld().getName());
    }
}
