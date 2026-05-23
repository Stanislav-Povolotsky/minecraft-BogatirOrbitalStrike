package me.bogatir.orbitalstrike.strike;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import me.bogatir.orbitalstrike.gui.OrbitalStrikeGUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages scheduled StrikeTask instances and owns the singleton GUI.
 */
public class StrikeManager {

    private final BogatirOrbitalStrike plugin;
    private final OrbitalStrikeGUI     gui;
    private final List<StrikeTask>     activeTasks = new ArrayList<>();

    public StrikeManager(BogatirOrbitalStrike plugin) {
        this.plugin = plugin;
        this.gui    = new OrbitalStrikeGUI(plugin);
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Clones the given data and schedules a full strike sequence.
     *
     * @param initiator The player who activated the strike.
     * @param data      Strike configuration snapshot.
     */
    public void scheduleStrike(Player initiator, StrikeData data) {
        int delay = data.getDelayTicks();
        initiator.sendMessage(
            "§a§l[⚡ Orbital Strike] §fStrike scheduled! ETA: §e" + delay
            + " §fticks §7(" + String.format("%.1f", delay / 20.0) + "s)§f."
        );

        StrikeTask task = new StrikeTask(plugin, data.clone(), initiator);
        activeTasks.add(task);
        // runTaskTimer: initial delay = delayTicks, then every 1 tick
        task.runTaskTimer(plugin, delay, 1L);
    }

    /** Called by StrikeTask itself when it finishes. */
    public void removeTask(StrikeTask task) {
        activeTasks.remove(task);
    }

    /** Cancel all in-flight strikes (called on plugin disable). */
    public void cancelAll() {
        for (StrikeTask task : new ArrayList<>(activeTasks)) {
            task.cancel();
        }
        activeTasks.clear();
    }

    // ─── Getter ────────────────────────────────────────────────────────────────

    public OrbitalStrikeGUI getGUI() {
        return gui;
    }
}
