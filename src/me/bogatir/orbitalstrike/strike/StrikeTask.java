package me.bogatir.orbitalstrike.strike;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import me.bogatir.orbitalstrike.laser.LaserRenderer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Runs the full orbital strike animation sequence and deals the impact.
 *
 * <p>Scheduled via {@code runTaskTimer(plugin, delayTicks, 1L)} so that
 * each server tick advances the animation state machine.
 *
 * <h3>Cycle (135 ticks per strike)</h3>
 * <pre>
 *   [  0 –  59] Phase 1 — Hexagon of 6 red dots at sky height
 *   [ 60 –  84] Phase 2 — Hexagon + white centre glow
 *   [ 85 – 104] Phase 3 — Laser beam descends (progress 0→1)
 *   [       105] Phase 4 — IMPACT: explosion, fire, fear effect
 *   [106 – 134] Pause    — quiet gap between consecutive strikes
 * </pre>
 */
public class StrikeTask extends BukkitRunnable {

    // ─── Phase boundaries ──────────────────────────────────────────────────────
    private static final int HEX_START     = 0;
    private static final int HEX_END       = 60;   // exclusive
    private static final int WHITE_END     = 85;   // exclusive
    private static final int LASER_END     = 105;  // exclusive
    private static final int IMPACT_TICK   = 105;
    private static final int CYCLE         = 135;  // ticks per strike

    // ─── Fields ────────────────────────────────────────────────────────────────
    private final BogatirOrbitalStrike plugin;
    private final StrikeData           data;
    private final Player               initiator;
    private final LaserRenderer        renderer  = new LaserRenderer();
    private final Random               random    = new Random();

    private int tick = 0;

    public StrikeTask(BogatirOrbitalStrike plugin, StrikeData data, Player initiator) {
        this.plugin    = plugin;
        this.data      = data;
        this.initiator = initiator;
    }

    // ─── BukkitRunnable ────────────────────────────────────────────────────────

    @Override
    public void run() {
        // Stop after all strikes have completed their full cycle
        if (tick >= data.getCount() * CYCLE) {
            plugin.getStrikeManager().removeTask(this);
            cancel();
            return;
        }

        World world = plugin.getServer().getWorld(data.getWorld());
        if (world == null) {
            plugin.getStrikeManager().removeTask(this);
            cancel();
            return;
        }

        Location target = new Location(world, data.getX(), data.getY(), data.getZ());
        int tickInCycle = tick % CYCLE;

        // ── Phase 1: Hexagon ──────────────────────────────────────────────────
        if (tickInCycle >= HEX_START && tickInCycle < HEX_END) {
            if (tickInCycle % 3 == 0) {
                renderer.spawnHexagonPoints(world, target);
            }
            // Play distant eerie warning at the very start of each strike
            if (tickInCycle == HEX_START) {
                world.playSound(target, Sound.ENTITY_WITHER_AMBIENT, 4.0f, 0.4f);
                notifyNearby(world, target,
                    "§e§l[⚡] §fOrbital strike inbound at §c"
                    + String.format("%.0f, %.0f, %.0f", target.getX(), target.getY(), target.getZ())
                    + "§f! Brace for impact!");
            }
        }

        // ── Phase 2: Hexagon + white centre ───────────────────────────────────
        else if (tickInCycle < WHITE_END) {
            if (tickInCycle % 3 == 0) renderer.spawnHexagonPoints(world, target);
            renderer.spawnWhiteCenter(world, target);

            if (tickInCycle == HEX_END) {
                world.playSound(target, Sound.BLOCK_BEACON_POWER_SELECT, 4.0f, 1.8f);
            }
        }

        // ── Phase 3: Laser descending ─────────────────────────────────────────
        else if (tickInCycle < LASER_END) {
            double progress = (double)(tickInCycle - WHITE_END) / (LASER_END - WHITE_END);
            renderer.spawnLaserProgress(world, target, progress);

            // Guardian laser sound while beam is active
            if (tickInCycle % 4 == 0) {
                world.playSound(target, Sound.ENTITY_GUARDIAN_ATTACK, 3.0f, 0.5f + (float)progress * 0.8f);
            }
        }

        // ── Phase 4: Impact ───────────────────────────────────────────────────
        else if (tickInCycle == IMPACT_TICK) {
            renderer.spawnImpactBurst(world, target);
            executeImpact(world, target);

            // Apply fear to nearby players for each impact
            plugin.getFearEffectManager().applyFear(
                target,
                data.getFearPower(),
                data.getFearDuration()
            );

            int strikeNumber = tick / CYCLE + 1;
            notifyNearby(world, target,
                "§c§l[⚡] §fOrbital strike §e#" + strikeNumber + "§f of §e" + data.getCount()
                + " §fimpacted at §c"
                + String.format("%.0f, %.0f, %.0f", target.getX(), target.getY(), target.getZ()));
        }

        // ── Pause phase: silence between strikes ──────────────────────────────
        // (nothing to do here — visual breathing room)

        tick++;
    }

    // ─── Impact logic ──────────────────────────────────────────────────────────

    private void executeImpact(World world, Location target) {
        // Explosion — power scales 2f (p=1) to 20f (p=10)
        float explosionPower = data.getPower() * 2.0f;
        world.createExplosion(target, explosionPower, /* setFire */ true, /* breakBlocks */ true);

        // Extra fire spreading in a circle around the impact
        int fireRadius = data.getPower();
        for (int dx = -fireRadius; dx <= fireRadius; dx++) {
            for (int dz = -fireRadius; dz <= fireRadius; dz++) {
                if (dx * dx + dz * dz > fireRadius * fireRadius) continue;
                if (random.nextFloat() > 0.35f) continue;

                Block candidate = world.getBlockAt(
                    (int) target.getX() + dx,
                    (int) target.getY() + 1,
                    (int) target.getZ() + dz
                );
                if (candidate.getType() == Material.AIR) {
                    Block below = candidate.getRelative(0, -1, 0);
                    // Only ignite if there's a solid block beneath
                    if (below.getType().isSolid()) {
                        candidate.setType(Material.FIRE);
                    }
                }
            }
        }

        // Sounds
        world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,   10.0f, 0.5f);
        world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_IMPACT,      8.0f, 0.7f);
        world.playSound(target, Sound.BLOCK_BEACON_DEACTIVATE,           6.0f, 0.3f);
    }

    // ─── Utility ───────────────────────────────────────────────────────────────

    private void notifyNearby(World world, Location center, String message) {
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distance(center) <= 300) {
                p.sendMessage(message);
            }
        }
    }
}
