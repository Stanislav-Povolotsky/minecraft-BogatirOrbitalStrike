package me.bogatir.orbitalstrike.gui;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import me.bogatir.orbitalstrike.strike.StrikeData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles all click events inside the Orbital Strike GUI inventory.
 *
 * <p>Each input slot closes the inventory, sends a prompt, and registers a
 * {@link ChatInputListener} callback so the player can type the new value.
 * The ACTIVATE slot launches the strike immediately.
 */
public class GUIListener implements Listener {

    private final BogatirOrbitalStrike plugin;

    public GUIListener(BogatirOrbitalStrike plugin) {
        this.plugin = plugin;
    }

    // ─── Inventory click ───────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!OrbitalStrikeGUI.TITLE.equals(event.getView().getTitle())) return;

        event.setCancelled(true);   // always prevent item movement

        int slot = event.getRawSlot();
        OrbitalStrikeGUI gui  = plugin.getStrikeManager().getGUI();
        StrikeData        data = gui.getData(player);
        if (data == null) return;

        switch (slot) {
            case OrbitalStrikeGUI.SLOT_X ->
                promptDouble(player, "Enter target §cX§f coordinate:", "x");
            case OrbitalStrikeGUI.SLOT_Y ->
                promptDouble(player, "Enter target §aY§f coordinate:", "y");
            case OrbitalStrikeGUI.SLOT_Z ->
                promptDouble(player, "Enter target §9Z§f coordinate:", "z");
            case OrbitalStrikeGUI.SLOT_DELAY ->
                promptInt(player, "Enter §edelay§f in ticks §7(20 = 1 second)§f:", "delay", 1, Integer.MAX_VALUE);
            case OrbitalStrikeGUI.SLOT_POWER ->
                promptInt(player, "Enter §6strike power§f §7(1–10)§f:", "power", 1, 10);
            case OrbitalStrikeGUI.SLOT_COUNT ->
                promptInt(player, "Enter §fstrike count§f §7(≥1)§f:", "count", 1, Integer.MAX_VALUE);
            case OrbitalStrikeGUI.SLOT_FEAR_POWER ->
                promptInt(player, "Enter §4fear power§f §7(0=off, 1–10)§f:", "fearpower", 0, 10);
            case OrbitalStrikeGUI.SLOT_FEAR_DURATION ->
                promptInt(player, "Enter §4fear duration§f in ticks:", "fearduration", 0, Integer.MAX_VALUE);
            case OrbitalStrikeGUI.SLOT_ACTIVATE ->
                activateStrike(player, gui, data);
        }
    }

    // ─── Player disconnect cleanup ─────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getStrikeManager().getGUI().clearData(event.getPlayer());
        plugin.getChatInputListener().cancelPending(event.getPlayer());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void promptDouble(Player player, String prompt, String field) {
        player.closeInventory();
        player.sendMessage("§f" + prompt + "  §8(type §7cancel§8 to abort)");

        plugin.getChatInputListener().expectInput(player, raw -> {
            try {
                double value = Double.parseDouble(raw);
                applyField(player, field, value);
            } catch (NumberFormatException ex) {
                player.sendMessage("§cInvalid number: §f" + raw);
            }
            plugin.getStrikeManager().getGUI().open(player, null);
        });
    }

    private void promptInt(Player player, String prompt, String field, int min, int max) {
        player.closeInventory();
        player.sendMessage("§f" + prompt + "  §8(type §7cancel§8 to abort)");

        plugin.getChatInputListener().expectInput(player, raw -> {
            try {
                int value = Integer.parseInt(raw);
                if (value < min || value > max) {
                    player.sendMessage("§cValue must be between §f" + min + "§c and §f" + max + "§c.");
                } else {
                    applyField(player, field, (double) value);
                }
            } catch (NumberFormatException ex) {
                player.sendMessage("§cInvalid integer: §f" + raw);
            }
            plugin.getStrikeManager().getGUI().open(player, null);
        });
    }

    private void applyField(Player player, String field, double value) {
        StrikeData data = plugin.getStrikeManager().getGUI().getData(player);
        if (data == null) return;

        switch (field) {
            case "x"            -> data.setX(value);
            case "y"            -> data.setY(value);
            case "z"            -> data.setZ(value);
            case "delay"        -> data.setDelayTicks((int) value);
            case "power"        -> data.setPower((int) value);
            case "count"        -> data.setCount((int) value);
            case "fearpower"    -> data.setFearPower((int) value);
            case "fearduration" -> data.setFearDuration((int) value);
        }
    }

    private void activateStrike(Player player, OrbitalStrikeGUI gui, StrikeData data) {
        player.closeInventory();

        // Validate world
        if (player.getServer().getWorld(data.getWorld()) == null) {
            player.sendMessage("§cWorld §f'" + data.getWorld() + "' §cnot found! Using your current world.");
            data.setWorld(player.getWorld().getName());
        }

        plugin.getStrikeManager().scheduleStrike(player, data.clone());
    }
}
