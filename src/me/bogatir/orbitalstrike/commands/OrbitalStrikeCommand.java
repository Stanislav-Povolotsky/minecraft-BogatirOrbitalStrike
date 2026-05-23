package me.bogatir.orbitalstrike.commands;

import me.bogatir.orbitalstrike.BogatirOrbitalStrike;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the {@code /orbitalstrike} command.
 *
 * <pre>
 * /orbitalstrike give [player]  — gives 1 Orbital Strike block
 * /orbitalstrike give [player] [amount]
 * /orbitalstrike help           — shows usage
 * </pre>
 */
public class OrbitalStrikeCommand implements CommandExecutor {

    private final BogatirOrbitalStrike plugin;

    public OrbitalStrikeCommand(BogatirOrbitalStrike plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            return handleGive(sender, args);
        }

        sendHelp(sender);
        return true;
    }

    // ─── Subcommands ───────────────────────────────────────────────────────────

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("orbitalstrike.give")) {
            sender.sendMessage("§cYou don't have permission to give Orbital Strike blocks.");
            return true;
        }

        // Resolve target player
        Player target;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer §f" + args[1] + " §cnot found or offline.");
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("§cUsage: /orbitalstrike give <player> [amount]");
            return true;
        }

        // Resolve amount
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.min(64, Math.max(1, Integer.parseInt(args[2])));
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cInvalid amount: §f" + args[2]);
                return true;
            }
        }

        ItemStack item = plugin.getOrbitalStrikeBlock().createItem(amount);
        target.getInventory().addItem(item);
        target.sendMessage("§5§l[Orbital Strike] §fYou received §e" + amount + "x §5Orbital Strike §fblock(s).");

        if (!sender.equals(target)) {
            sender.sendMessage("§aGave §e" + amount + "x §5Orbital Strike §fblock(s) to §f" + target.getName() + "§a.");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§5§lOrbital Strike §7— Commands");
        sender.sendMessage("§f/orbitalstrike give §7[player] [amount] §8— Give the custom block");
        sender.sendMessage("§f/orbitalstrike help §8— Show this help");
        sender.sendMessage("§7Place the §5Orbital Strike §7block and right-click it to configure.");
    }
}
