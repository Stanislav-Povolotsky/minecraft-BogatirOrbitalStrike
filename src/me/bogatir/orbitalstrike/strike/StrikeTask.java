package me.bogatir.orbitalstrike.strike;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import me.bogatir.orbitalstrike.laser.LaserRenderer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
        int power = data.getPower();

        // ── 1. Visual explosion + sound (purely cosmetic, breakBlocks=false
        //       so server config / mobGriefing cannot suppress our damage) ──────
        world.createExplosion(target, power * 2.0f, false, false);

        // ── 2. Manually break blocks in a sphere (radius scales with power) ────
        //    Radius: power 1 → 3 blocks, power 10 → 15 blocks
        int radius = 1 + (int) Math.round(power * 1.4);
        int radiusSq = radius * radius;

        int cx = target.getBlockX();
        int cy = target.getBlockY();
        int cz = target.getBlockZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx*dx + dy*dy + dz*dz > radiusSq) continue;

                    Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    Material mat = block.getType();

                    if (mat == Material.AIR
                            || mat == Material.VOID_AIR
                            || mat == Material.CAVE_AIR
                            || mat == Material.BEDROCK) continue;

                    // Distance factor: blocks closer to centre break more reliably
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    double breakChance = 1.0 - (dist / (radius + 1)) * 0.5;
                    if (random.nextDouble() > breakChance) continue;

                    // Use setType(AIR) — bypasses BlockBreakEvent, WorldGuard,
                    // spawn-protection and any other server-side block protection.
                    // Drop items manually so loot isn't lost.
                    block.breakNaturally();
                    block.setType(Material.AIR);
                }
            }
        }

        // ── 3. Fire spreading ring around crater ─────────────────────────────
        int fireRadius = power * 3;
        for (int dx = -fireRadius; dx <= fireRadius; dx++) {
            for (int dz = -fireRadius; dz <= fireRadius; dz++) {
                if (dx*dx + dz*dz > fireRadius*fireRadius) continue;
                if (random.nextFloat() > 0.85f) continue;

                // Try to place fire on the exposed top surface of any solid block
                for (int dy = radius; dy >= -radius - 2; dy--) {
                    Block candidate = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    if (candidate.getType() != Material.AIR) break;
                    Block below = candidate.getRelative(0, -1, 0);
                    if (below.getType().isSolid() && below.getType() != Material.BEDROCK) {
                        candidate.setType(Material.FIRE);
                        break;
                    }
                }
            }
        }

        // ── 4. Knockback nearby entities ──────────────────────────────────────
        double knockbackRadius = radius * 2.5;
        for (Entity entity : world.getNearbyEntities(target, knockbackRadius, knockbackRadius, knockbackRadius)) {
            if (!(entity instanceof LivingEntity)) continue;
            Vector dir = entity.getLocation().toVector().subtract(target.toVector());
            double dist = dir.length();
            if (dist < 0.1) dist = 0.1;
            double strength = (knockbackRadius - dist) / knockbackRadius * power * 0.5;
            entity.setVelocity(dir.normalize().multiply(strength).add(new Vector(0, strength * 0.4, 0)));
        }

        // ── 5. Sounds ─────────────────────────────────────────────────────────
        world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10.0f, 0.5f);
        world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_IMPACT,   8.0f, 0.7f);
        world.playSound(target, Sound.BLOCK_BEACON_DEACTIVATE,        6.0f, 0.3f);
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
