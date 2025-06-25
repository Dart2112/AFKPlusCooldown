package net.lapismc.afkpluscooldown;

import net.lapismc.afkplus.AFKPlus;
import net.lapismc.afkplus.playerdata.AFKPlusPlayer;
import net.lapismc.afkplus.util.core.commands.CommandRegistry;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public final class AFKPlusCooldown extends JavaPlugin implements Listener {

    private final AFKPlus afkPlus = (AFKPlus) AFKPlus.getInstance();
    private final HashMap<UUID, Long> commandTimes = new HashMap<>();
    private int cooldownTime;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        //The time in seconds that the cooldown should last
        cooldownTime = getConfig().getInt("Cooldown");
        getLogger().info(getName() + " v." + getDescription().getVersion() + " has been enabled!");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        boolean isAfkCommand = isMessageCommand(e.getMessage(), "afk");
        //Check if the player is running an alias of /AFK
        for (String command : CommandRegistry.getCommand("afk").getTakenAliases()) {
            if (isMessageCommand(e.getMessage(), command)) {
                isAfkCommand = true;
                break;
            }
        }
        if (isAfkCommand) {
            processCooldown(e, afkPlus.getPlayer(e.getPlayer().getUniqueId()));
        }
    }

    /**
     * Test if the given message is a command using the command string command
     *
     * @param message The message being run as a command
     * @param command The name of the command to test
     * @return True if the given command is being executed by the given message, otherwise false
     */
    private boolean isMessageCommand(String message, String command) {
        if (message.equalsIgnoreCase("/" + command)) {
            return true;
        } else return message.startsWith("/" + command + " ");
    }

    /**
     * Handle cooldown command canceling and informing the player
     *
     * @param e The command event we are handling
     * @param p The player who executed the command
     */
    private void processCooldown(PlayerCommandPreprocessEvent e, AFKPlusPlayer p) {
        //Check if the player has an active cooldown
        boolean shouldCancel = isCooldownActive(p.getUUID());
        if (shouldCancel) {
            //Cancel the /afk command
            e.setCancelled(true);
            //Calculate the time at which the cooldown will end for PrettyTime
            long cooldownEnd = commandTimes.get(p.getUUID()) + (cooldownTime * 1000L);
            //Get a PrettyTime string using cooldownEnd
            String timeRemaining = afkPlus.prettyTime.formatDuration(afkPlus.reduceDurationList
                    (afkPlus.prettyTime.calculatePreciseDuration(new Date(cooldownEnd))));
            //Get the message from config and handle color codes
            String msg = afkPlus.config.colorMessage(getConfig().getString("DisallowMessage"));
            //Insert the pretty time message if its needed
            msg = msg.replace("{TIME}", timeRemaining);
            //Send the player the formatted message
            Bukkit.getPlayer(p.getUUID()).sendMessage(msg);
        } else {
            //Record this as a successful command execution, therefore starting the cooldown
            commandTimes.put(p.getUUID(), System.currentTimeMillis());
        }
    }

    /**
     * Check if the player must wait for the cooldown to complete
     *
     * @param uuid The UUID of the player to test for
     * @return True if the player must wait longer
     */
    private boolean isCooldownActive(UUID uuid) {
        //If the player isn't stored then we return false
        if (!commandTimes.containsKey(uuid)) {
            return false;
        }
        //The time that this player last used /afk
        long lastCommand = commandTimes.get(uuid);
        //The difference in time between lastCommand and now in milliseconds
        long difference = System.currentTimeMillis() - lastCommand;
        //Check if the difference (now in seconds) is longer than the cooldownTime
        //Return true if it isn't (e.g. the player hasn't waited long enough)
        return (difference / 1000) <= cooldownTime;
    }

}
