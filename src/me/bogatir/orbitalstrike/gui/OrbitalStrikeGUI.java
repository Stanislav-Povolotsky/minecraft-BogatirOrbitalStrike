package me.bogatir.orbitalstrike.gui;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import me.bogatir.orbitalstrike.strike.StrikeData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds and manages the 54-slot Orbital Strike configuration GUI.
 *
 * <h3>Layout (rows × 9)</h3>
 * <pre>
 *  Row 0  [  0– 8] glass border
 *  Row 1  [ 9–17]  glass | X | glass | Y | glass | Z | glass | DELAY | glass
 *  Row 2  [18–26]  glass border
 *  Row 3  [27–35]  glass | POWER | glass | COUNT | glass | FPOW | glass | FDUR | glass
 *  Row 4  [36–44]  glass | glass | glass | glass | ACT | glass | glass | glass | glass
 *  Row 5  [45–53]  glass border
 * </pre>
 *
 * Input is collected through {@link ChatInputListener} (player types in chat).
 */
public class OrbitalStrikeGUI {

    // ─── Slot constants ────────────────────────────────────────────────────────
    public static final int SLOT_X             = 10;
    public static final int SLOT_Y             = 12;
    public static final int SLOT_Z             = 14;
    public static final int SLOT_DELAY         = 16;
    public static final int SLOT_POWER         = 28;
    public static final int SLOT_COUNT         = 30;
    public static final int SLOT_FEAR_POWER    = 32;
    public static final int SLOT_FEAR_DURATION = 34;
    public static final int SLOT_ACTIVATE      = 40;

    public static final String TITLE = "§5§lOrbital Strike Control";

    // ─── Per-player state ──────────────────────────────────────────────────────
    private final Map<UUID, StrikeData>   playerData   = new HashMap<>();
    /** Tracks which world each player's block is in. */
    private final Map<UUID, String>       playerWorlds = new HashMap<>();

    private final BogatirOrbitalStrike plugin;

    public OrbitalStrikeGUI(BogatirOrbitalStrike plugin) {
        this.plugin = plugin;
    }

    // ─── Open / rebuild ────────────────────────────────────────────────────────

    /**
     * Opens (or re-opens) the GUI for a player.
     *
     * @param worldName The world name the block was placed in (may be null to keep current).
     */
    public void open(Player player, String worldName) {
        UUID uuid = player.getUniqueId();

        // Initialise data on first open
        StrikeData data = playerData.computeIfAbsent(uuid, id -> {
            StrikeData d = new StrikeData();
            d.setX(Math.round(player.getLocation().getX()));
            d.setY(Math.round(player.getLocation().getY()));
            d.setZ(Math.round(player.getLocation().getZ()));
            d.setWorld(player.getWorld().getName());
            return d;
        });

        if (worldName != null) {
            data.setWorld(worldName);
            playerWorlds.put(uuid, worldName);
        }

        // Build inventory
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Glass border everywhere
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // ── Coordinate inputs ──────────────────────────────────────────────────
        inv.setItem(SLOT_X, makeInput(Material.RED_DYE,    "§cTarget X",
            String.format("%.1f", data.getX()),
            "§7Click → type in chat"));
        inv.setItem(SLOT_Y, makeInput(Material.LIME_DYE,   "§aTarget Y",
            String.format("%.1f", data.getY()),
            "§7Click → type in chat"));
        inv.setItem(SLOT_Z, makeInput(Material.BLUE_DYE,   "§9Target Z",
            String.format("%.1f", data.getZ()),
            "§7Click → type in chat"));
        inv.setItem(SLOT_DELAY, makeInput(Material.CLOCK,  "§eDelay",
            data.getDelayTicks() + " ticks  §8(" + String.format("%.1f", data.getDelayTicks() / 20.0) + "s§8)",
            "§7Click → type in chat",
            "§7(20 ticks = 1 second)"));

        // ── Strike parameters ──────────────────────────────────────────────────
        inv.setItem(SLOT_POWER, makeInput(Material.BLAZE_POWDER, "§6Strike Power",
            data.getPower() + " §8/ 10",
            "§7Click → type in chat",
            "§71 = small  |  10 = devastating"));
        inv.setItem(SLOT_COUNT, makeInput(Material.GUNPOWDER,    "§fStrike Count",
            String.valueOf(data.getCount()),
            "§7Click → type in chat",
            "§7Strikes repeat at same coords"));

        // ── Fear effect ────────────────────────────────────────────────────────
        inv.setItem(SLOT_FEAR_POWER, makeInput(Material.SPIDER_EYE, "§4Fear Power",
            data.getFearPower() + " §8/ 10",
            "§7Click → type in chat",
            "§7Camera shake intensity",
            "§70 = disabled"));
        inv.setItem(SLOT_FEAR_DURATION, makeInput(Material.PHANTOM_MEMBRANE, "§4Fear Duration",
            data.getFearDuration() + " ticks  §8(" + String.format("%.1f", data.getFearDuration() / 20.0) + "s§8)",
            "§7Click → type in chat"));

        // ── Activate button ────────────────────────────────────────────────────
        inv.setItem(SLOT_ACTIVATE, makeItem(Material.LIME_STAINED_GLASS_PANE,
            "§a§l⚡  ACTIVATE STRIKE  ⚡",
            "§7World:  §f" + data.getWorld(),
            "§7Target: §f" + String.format("%.0f, %.0f, %.0f", data.getX(), data.getY(), data.getZ()),
            "§7Delay:  §f" + data.getDelayTicks() + " ticks",
            "§7Power:  §f" + data.getPower() + "/10   Count: §f" + data.getCount(),
            "§7Fear:   §fpow=" + data.getFearPower() + "  dur=" + data.getFearDuration() + " ticks",
            "",
            "§a§lClick to launch!"
        ));

        player.openInventory(inv);
    }

    // ─── Item builders ─────────────────────────────────────────────────────────

    private ItemStack makeInput(Material mat, String name, String value, String... hints) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add("§fValue: §e" + value);
        for (String hint : hints) lore.add(hint);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) meta.setLore(List.of(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    // ─── Data access ───────────────────────────────────────────────────────────

    public StrikeData getData(Player player) {
        return playerData.get(player.getUniqueId());
    }

    /** Removes per-player data (call when player leaves server). */
    public void clearData(Player player) {
        UUID uuid = player.getUniqueId();
        playerData.remove(uuid);
        playerWorlds.remove(uuid);
    }
}
