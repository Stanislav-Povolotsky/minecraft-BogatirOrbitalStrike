package me.bogatir.orbitalstrike.block;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Manages the custom "Orbital Strike" block identity.
 *
 * <p>Implementation uses a {@link Material#BEACON} as the base block because
 * Beacon extends {@link Beacon} which extends {@link BlockState} and implements
 * {@link PersistentDataHolder}, allowing PDC data to survive server restarts.
 *
 * <p>The item's {@link ItemMeta} also carries the PDC tag so
 * {@link me.bogatir.orbitalstrike.listeners.BlockPlaceListener} can detect it
 * at placement time and write the tag onto the placed block's tile state.
 */
public class OrbitalStrikeBlock {

    public static final Material BLOCK_MATERIAL = Material.BEACON;

    private final BogatirOrbitalStrike plugin;

    /** The key stored on both the item and the placed block tile-state. */
    private final NamespacedKey blockKey;

    public OrbitalStrikeBlock(BogatirOrbitalStrike plugin) {
        this.plugin   = plugin;
        this.blockKey = new NamespacedKey(plugin, "orbital_strike_block");
    }

    // ─── Item factory ──────────────────────────────────────────────────────────

    /**
     * Creates a properly tagged Orbital Strike block item.
     */
    public ItemStack createItem(int amount) {
        ItemStack item = new ItemStack(BLOCK_MATERIAL, Math.max(1, amount));
        ItemMeta  meta = item.getItemMeta();

        meta.setDisplayName("§5§lOrbital Strike");
        meta.setLore(List.of(
            "§7Right-click to configure orbital",
            "§7laser strike parameters.",
            "",
            "§8[Bogatir Orbital Strike]"
        ));
        // Mark the item so BlockPlaceListener can identify it
        meta.getPersistentDataContainer().set(blockKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    // ─── Identity checks ───────────────────────────────────────────────────────

    /**
     * Returns true if this item is an Orbital Strike block item (has PDC tag).
     */
    public boolean isOrbitalStrikeItem(ItemStack item) {
        if (item == null || item.getType() != BLOCK_MATERIAL) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta()
                   .getPersistentDataContainer()
                   .has(blockKey);
    }

    /**
     * Returns true if the given placed block is a tagged Orbital Strike block.
     */
    public boolean isOrbitalStrikeBlock(Block block) {
        if (block == null || block.getType() != BLOCK_MATERIAL) return false;
        BlockState state = block.getState();
        if (state instanceof PersistentDataHolder holder) {
            return holder.getPersistentDataContainer().has(blockKey);
        }
        return false;
    }

    // ─── Block tagging ─────────────────────────────────────────────────────────

    /**
     * Writes the PDC marker onto a freshly placed beacon tile-state.
     * Must be called synchronously (world modification).
     */
    public void markBlock(Block block) {
        if (block.getType() != BLOCK_MATERIAL) return;
        BlockState state = block.getState();
        if (state instanceof PersistentDataHolder holder) {
            holder.getPersistentDataContainer().set(blockKey, PersistentDataType.BYTE, (byte) 1);
            state.update(true, false); // force = true, applyPhysics = false
        }
    }

    // ─── Getter ────────────────────────────────────────────────────────────────

    public NamespacedKey getBlockKey() {
        return blockKey;
    }
}
