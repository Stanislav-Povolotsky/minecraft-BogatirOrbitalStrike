package me.bogatir.orbitalstrike.laser;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Renders all particle effects for the orbital strike animation sequence.
 *
 * <p>Phases:
 * <ol>
 *   <li>Hexagon of 6 red dust particles at sky height.</li>
 *   <li>White {@code END_ROD} glow at hexagon centre.</li>
 *   <li>Laser beam descending from sky to target (progress 0.0→1.0).</li>
 *   <li>Impact burst.</li>
 * </ol>
 */
public class LaserRenderer {

    /** Horizontal radius of the warning hexagon (blocks). */
    public static final double HEXAGON_RADIUS = 8.0;

    /** Y-level used as the top of the laser / hexagon origin. */
    public static final double SKY_OFFSET = 150.0;

    // ─── Phase helpers ─────────────────────────────────────────────────────────

    /**
     * Spawns 6 red dust particles in a hexagon pattern at sky height above {@code center}.
     */
    public void spawnHexagonPoints(World world, Location center) {
        double skyY = getSkyY(world, center);
        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 3.0f);

        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            double x = center.getX() + HEXAGON_RADIUS * Math.cos(angle);
            double z = center.getZ() + HEXAGON_RADIUS * Math.sin(angle);
            world.spawnParticle(Particle.DUST, x, skyY, z, 6, 0.3, 0.3, 0.3, 0, red);
        }
    }

    /**
     * Spawns a bright white glow at the hexagon centre, indicating imminent strike.
     */
    public void spawnWhiteCenter(World world, Location center) {
        double skyY = getSkyY(world, center);
        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 4.0f);

        world.spawnParticle(Particle.END_ROD, center.getX(), skyY, center.getZ(),
                            25, 0.6, 0.6, 0.6, 0.05);
        world.spawnParticle(Particle.DUST,    center.getX(), skyY, center.getZ(),
                            15, 0.5, 0.5, 0.5, 0, white);
    }

    /**
     * Renders the laser beam from sky to target at the given progress (0.0 = top, 1.0 = impact).
     *
     * @param progress 0.0–1.0
     */
    public void spawnLaserProgress(World world, Location target, double progress) {
        double skyY   = getSkyY(world, target);
        double groundY = target.getY();
        double beamTip = skyY - (skyY - groundY) * progress;

        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 2.0f);

        // Main beam: END_ROD column from sky to current tip
        for (double y = skyY; y > beamTip; y -= 0.6) {
            world.spawnParticle(Particle.END_ROD,
                                target.getX(), y, target.getZ(),
                                3, 0.08, 0, 0.08, 0.01);
        }

        // Bright tip glow
        world.spawnParticle(Particle.DUST,
                            target.getX(), beamTip, target.getZ(),
                            12, 0.2, 0.2, 0.2, 0, white);
        world.spawnParticle(Particle.END_ROD,
                            target.getX(), beamTip, target.getZ(),
                            6, 0.15, 0.15, 0.15, 0.05);
    }

    /**
     * Spawns the visual burst at point of impact.
     */
    public void spawnImpactBurst(World world, Location target) {
        world.spawnParticle(Particle.END_ROD,       target, 60, 3.0, 3.0, 3.0, 0.3);
        world.spawnParticle(Particle.LAVA,           target, 80, 4.0, 4.0, 4.0, 0.8);
        world.spawnParticle(Particle.FLASH,          target, 5,  2.0, 2.0, 2.0, 0, Color.WHITE);
        world.spawnParticle(Particle.EXPLOSION,      target, 10, 2.0, 2.0, 2.0, 0);
    }

    // ─── Utility ───────────────────────────────────────────────────────────────

    private double getSkyY(World world, Location center) {
        return Math.min(center.getY() + SKY_OFFSET, world.getMaxHeight() - 5);
    }
}
