package me.bogatir.orbitalstrike.fear;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Applies the "Fear" effect to all players near an impact location.
 *
 * <p>Components:
 * <ul>
 *   <li>DARKNESS potion — flashing black vignette</li>
 *   <li>NAUSEA potion — disorientation / screen wobble</li>
 *   <li>Camera shake — scheduled yaw/pitch jitter via teleport</li>
 *   <li>Black title flash — "⚠ ORBITAL STRIKE ⚠"</li>
 * </ul>
 */
public class FearEffectManager {

    /** Radius (blocks) within which players are affected. */
    public static final double EFFECT_RADIUS = 100.0;

    private final BogatirOrbitalStrike plugin;
    private final Random random = new Random();

    public FearEffectManager(BogatirOrbitalStrike plugin) {
        this.plugin = plugin;
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Applies Fear to every player within {@link #EFFECT_RADIUS} of {@code epicenter}.
     *
     * @param epicenter     Impact location
     * @param fearPower     Intensity 0–10
     * @param fearDurationTicks Duration in server ticks
     */
    public void applyFear(Location epicenter, int fearPower, int fearDurationTicks) {
        if (fearPower <= 0 || fearDurationTicks <= 0) return;

        World world = epicenter.getWorld();
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(epicenter) <= EFFECT_RADIUS) {
                applyFearToPlayer(player, fearPower, fearDurationTicks);
            }
        }
    }

    // ─── Per-player ────────────────────────────────────────────────────────────

    private void applyFearToPlayer(Player player, int fearPower, int fearDurationTicks) {

        // 1. DARKNESS — causes the black vignette pulse effect (Paper 1.19+)
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.DARKNESS,
            fearDurationTicks,
            Math.min(fearPower - 1, 9),
            false, false, true
        ));

        // 2. NAUSEA — screen wobble / disorientation
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.NAUSEA,
            fearDurationTicks,
            0,
            false, false, false
        ));

        // 3. Camera shake — yaw/pitch jitter every N ticks
        // Higher power = more jitter and faster interval
        final int shakeInterval = Math.max(1, 5 - fearPower / 2);   // 1–5 ticks
        final float maxYaw   = fearPower * 2.0f;
        final float maxPitch = fearPower * 1.0f;
        final int totalShakeTicks = fearDurationTicks;

        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= totalShakeTicks || !player.isOnline()) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation();
                float yawOffset   = (random.nextFloat() - 0.5f) * maxYaw;
                float pitchOffset = (random.nextFloat() - 0.5f) * maxPitch;
                loc.setYaw(loc.getYaw() + yawOffset);
                loc.setPitch(Math.max(-89.9f, Math.min(89.9f, loc.getPitch() + pitchOffset)));
                player.teleport(loc);
                elapsed += shakeInterval;
            }
        }.runTaskTimer(plugin, 0L, shakeInterval);

        // 4. Black title flash — fires every 20 ticks while fear is active
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= fearDurationTicks || !player.isOnline()) {
                    cancel();
                    return;
                }
                // §k = obfuscated (scrambled glyphs) for a chaotic overlay effect
                player.sendTitle(
                    "§0§k|||||||",
                    "§4§l⚠ ORBITAL STRIKE ⚠",
                    0, 18, 4
                );
                elapsed += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        player.sendMessage("§8§o[Fear effect applied for " + fearDurationTicks + " ticks]");
    }
}
