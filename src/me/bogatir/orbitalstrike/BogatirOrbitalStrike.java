package me.bogatir.orbitalstrike;

import me.bogatir.orbitalstrike.block.OrbitalStrikeBlock;
import me.bogatir.orbitalstrike.commands.OrbitalStrikeCommand;
import me.bogatir.orbitalstrike.fear.FearEffectManager;
import me.bogatir.orbitalstrike.gui.ChatInputListener;
import me.bogatir.orbitalstrike.gui.GUIListener;
import me.bogatir.orbitalstrike.listeners.BlockInteractListener;
import me.bogatir.orbitalstrike.listeners.BlockPlaceListener;
import me.bogatir.orbitalstrike.strike.StrikeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BogatirOrbitalStrike extends JavaPlugin {

    private static BogatirOrbitalStrike instance;

    private OrbitalStrikeBlock orbitalStrikeBlock;
    private StrikeManager strikeManager;
    private FearEffectManager fearEffectManager;
    private ChatInputListener chatInputListener;

    @Override
    public void onEnable() {
        instance = this;

        // Core systems
        orbitalStrikeBlock = new OrbitalStrikeBlock(this);
        fearEffectManager  = new FearEffectManager(this);
        chatInputListener  = new ChatInputListener();
        strikeManager      = new StrikeManager(this);   // depends on above

        // Listeners
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this),    this);
        getServer().getPluginManager().registerEvents(new BlockInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this),           this);
        getServer().getPluginManager().registerEvents(chatInputListener,               this);

        // Commands
        var cmd = getCommand("orbitalstrike");
        if (cmd != null) cmd.setExecutor(new OrbitalStrikeCommand(this));

        getLogger().info("BogatirOrbitalStrike enabled! Targeting Paper 26.1.2 / Java 21.");
    }

    @Override
    public void onDisable() {
        if (strikeManager != null) strikeManager.cancelAll();
        getLogger().info("BogatirOrbitalStrike disabled.");
    }

    // ─── Static accessor ───────────────────────────────────────────────────────

    public static BogatirOrbitalStrike getInstance() {
        return instance;
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public OrbitalStrikeBlock getOrbitalStrikeBlock() { return orbitalStrikeBlock; }
    public StrikeManager      getStrikeManager()      { return strikeManager; }
    public FearEffectManager  getFearEffectManager()  { return fearEffectManager; }
    public ChatInputListener  getChatInputListener()  { return chatInputListener; }
}
