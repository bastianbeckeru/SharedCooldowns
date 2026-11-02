package com.sharedcooldowns.listener;

import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.config.ConfigManager;
import com.sharedcooldowns.cooldown.CooldownGroup;
import com.sharedcooldowns.cooldown.CooldownRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces cooldowns in two steps. LOWEST blocks a command on cooldown, reading
 * the message before other plugins rewrite it (CMI turns /home into /cmi home at
 * LOW). MONITOR starts the cooldown only if no other plugin cancelled the command.
 */
public class CommandListener implements Listener {

    private final SharedCooldowns plugin;
    private final ActionExecutor actions;
    private final Map<PlayerCommandPreprocessEvent, Match> inFlight = new IdentityHashMap<>();

    public CommandListener(SharedCooldowns plugin) {
        this.plugin = plugin;
        this.actions = new ActionExecutor(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandCheck(PlayerCommandPreprocessEvent event) {
        ConfigManager config = plugin.getConfigManager();
        String command = CommandMatcher.match(event.getMessage(), config.getCommandToGroup().keySet(),
                CommandListener::registeredNames);
        if (command == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("sharedcooldowns.bypass")) {
            return;
        }
        CooldownGroup group = config.getCommandToGroup().get(command);
        long remaining = plugin.getCooldownRegistry().getRemainingSeconds(player.getUniqueId(), group);
        if (remaining > 0) {
            event.setCancelled(true);
            actions.execute(player, group.actions(), remaining, command);
            return;
        }
        inFlight.put(event, new Match(group, command));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommandCommit(PlayerCommandPreprocessEvent event) {
        Match match = inFlight.remove(event);
        if (match == null || event.isCancelled() || !canRun(event)) {
            return;
        }
        CooldownRegistry registry = plugin.getCooldownRegistry();
        UUID playerId = event.getPlayer().getUniqueId();
        if (match.group().conditional()) {
            registry.markPending(playerId, match.group(), match.command(),
                    plugin.getConfigManager().getPendingTimeoutSeconds());
        } else {
            registry.applyCooldown(playerId, match.group());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getCooldownRegistry().clearPending(event.getPlayer().getUniqueId());
    }

    /** A registered command the player lacks permission for will not run. */
    private static boolean canRun(PlayerCommandPreprocessEvent event) {
        String label = event.getMessage().substring(1).trim().split("\\s+", 2)[0];
        Command command = Bukkit.getCommandMap().getCommand(label.toLowerCase(Locale.ROOT));
        return command == null || command.testPermissionSilent(event.getPlayer());
    }

    private static Collection<String> registeredNames(String label) {
        Command command = Bukkit.getCommandMap().getCommand(label);
        if (command == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>(command.getAliases());
        names.add(0, command.getName());
        return names;
    }

    private record Match(CooldownGroup group, String command) {}
}
