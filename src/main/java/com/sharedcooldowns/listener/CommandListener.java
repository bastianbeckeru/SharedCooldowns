package com.sharedcooldowns.listener;

import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.config.ConfigManager;
import com.sharedcooldowns.cooldown.CooldownGroup;
import com.sharedcooldowns.cooldown.CooldownRegistry;
import com.sharedcooldowns.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Intercepts player commands to enforce cooldowns.
 *
 * Uses LOWEST priority to run before most other plugins have processed
 * the event, allowing us to check command preprocess event first.
 */
public class CommandListener implements Listener {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final SharedCooldowns plugin;

    public CommandListener(SharedCooldowns plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Bypass permission check
        if (player.hasPermission("sharedcooldowns.bypass")) {
            return;
        }

        // Extract base command from the message: "/home arg1 arg2" → "home"
        String message = event.getMessage();
        String baseCommand = extractBaseCommand(message);
        if (baseCommand == null) {
            return;
        }

        // Find the cooldown group for this command
        ConfigManager configManager = plugin.getConfigManager();
        CooldownGroup group = configManager.getCommandToGroup().get(baseCommand);
        if (group == null) {
            return;
        }

        CooldownRegistry registry = plugin.getCooldownRegistry();
        UUID playerUUID = player.getUniqueId();

        // Check if player has an active cooldown for this group
        long remaining = registry.getRemainingSeconds(playerUUID, group.id());
        if (remaining > 0) {
            // Block the command and execute cooldown actions
            event.setCancelled(true);
            executeActions(player, group.actions(), remaining, baseCommand);
            return;
        }

        // No active cooldown — decide whether to apply immediately or defer
        if (group.conditional()) {
            // Conditional: mark as pending, cooldown applied only when action completes
            registry.markPending(playerUUID, group);
        } else {
            // Non-conditional: apply cooldown immediately
            registry.applyCooldown(playerUUID, group.id(), group.cooldownSeconds());
        }
    }

    /**
     * Clean up player data when they disconnect.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getCooldownRegistry().cancelPending(event.getPlayer().getUniqueId());
    }

    /**
     * Extracts the configured base command or subcommand match from a chat message.
     * Supports multi-word commands (like "cmi home"), CMI subcommands fallback, and namespaces.
     */
    private String extractBaseCommand(String message) {
        if (message == null || !message.startsWith("/")) {
            return null;
        }

        String cleanMessage = message.substring(1).trim().toLowerCase();
        if (cleanMessage.isEmpty()) {
            return null;
        }

        String[] partsArray = WHITESPACE_PATTERN.split(cleanMessage);
        List<String> parts = Arrays.asList(partsArray);

        // 1. Try matching the full command parts directly (handles "/cmi home args" matching "cmi home")
        String directMatch = findMatch(parts);
        if (directMatch != null) {
            return directMatch;
        }

        // 2. CMI Subcommand fallback: If player ran "/cmi home args" but config has only "home" (not "cmi home")
        if (parts.size() >= 2 && "cmi".equals(parts.get(0))) {
            List<String> subParts = parts.subList(1, parts.size());
            String cmiMatch = findMatch(subParts);
            if (cmiMatch != null) {
                return cmiMatch;
            }
        }

        // 3. General namespace fallback for the first command part (e.g., "/cmi:home args" -> check "home args")
        if (parts.size() >= 1 && parts.get(0).contains(":")) {
            String cleanFirstPart = stripNamespace(parts.get(0));
            List<String> subParts = new ArrayList<>();
            subParts.add(cleanFirstPart);
            if (parts.size() > 1) {
                subParts.addAll(parts.subList(1, parts.size()));
            }
            String nsMatch = findMatch(subParts);
            if (nsMatch != null) {
                return nsMatch;
            }
        }

        return null;
    }

    private String stripNamespace(String cmd) {
        int colonIndex = cmd.indexOf(':');
        if (colonIndex != -1) {
            return cmd.substring(colonIndex + 1);
        }
        return cmd;
    }

    private String findMatch(List<String> cmdParts) {
        Map<String, CooldownGroup> commandToGroup = plugin.getConfigManager().getCommandToGroup();

        // Check prefixes from longest to shortest
        for (int i = cmdParts.size(); i >= 1; i--) {
            List<String> subsegment = cmdParts.subList(0, i);
            String checkCmd = String.join(" ", subsegment);

            // Direct match
            if (commandToGroup.containsKey(checkCmd)) {
                return checkCmd;
            }

            // Namespace check on the first part of this subsegment
            String firstPart = cmdParts.get(0);
            if (firstPart.contains(":")) {
                String cleanFirstPart = stripNamespace(firstPart);
                List<String> newParts = new ArrayList<>();
                newParts.add(cleanFirstPart);
                if (subsegment.size() > 1) {
                    newParts.addAll(subsegment.subList(1, subsegment.size()));
                }
                String checkCmdNoNamespace = String.join(" ", newParts);
                if (commandToGroup.containsKey(checkCmdNoNamespace)) {
                    return checkCmdNoNamespace;
                }
            }
        }
        return null;
    }

    /**
     * Executes sequential actions for a player under cooldown.
     */
    private void executeActions(Player player, List<String> actions, long remaining, String baseCommand) {
        ConfigManager configManager = plugin.getConfigManager();
        String actionbarPrefix = "[actionbar]";
        String titlePrefix = "[title]";
        String soundPrefix = "[sound]";
        String messagePrefix = "[message]";

        for (String action : actions) {
            String replaced = action
                .replace("%time_compact%", configManager.formatTimeCompact(remaining))
                .replace("%time_single%", configManager.formatTimeSingle(remaining))
                .replace("%player%", player.getName())
                .replace("%command%", "/" + baseCommand);

            if (replaced.regionMatches(true, 0, actionbarPrefix, 0, actionbarPrefix.length())) {
                String content = replaced.substring(actionbarPrefix.length()).trim();
                player.sendActionBar(ColorUtil.colorize(content));
            } else if (replaced.regionMatches(true, 0, titlePrefix, 0, titlePrefix.length())) {
                String content = replaced.substring(titlePrefix.length()).trim();
                String[] titleParts = content.split(";", 2);
                String titleText = titleParts.length > 0 ? titleParts[0].trim() : "";
                String subtitleText = titleParts.length > 1 ? titleParts[1].trim() : "";
                player.showTitle(
                    net.kyori.adventure.title.Title.title(
                        ColorUtil.colorize(titleText),
                        ColorUtil.colorize(subtitleText)
                    )
                );
            } else if (replaced.regionMatches(true, 0, soundPrefix, 0, soundPrefix.length())) {
                String content = replaced.substring(soundPrefix.length()).trim();
                String[] soundParams = WHITESPACE_PATTERN.split(content);
                if (soundParams.length == 0 || soundParams[0].isEmpty()) {
                    continue;
                }
                String soundKey = soundParams[0];
                float volume = 1.0f;
                float pitch = 1.0f;
                if (soundParams.length > 1) {
                    try {
                        volume = Float.parseFloat(soundParams[1]);
                    } catch (NumberFormatException ignored) {}
                }
                if (soundParams.length > 2) {
                    try {
                        pitch = Float.parseFloat(soundParams[2]);
                    } catch (NumberFormatException ignored) {}
                }
                try {
                    player.playSound(player.getLocation(), soundKey, volume, pitch);
                } catch (Exception ignored) {
                    // Ignore sound play exceptions
                }
            } else {
                String content;
                if (replaced.regionMatches(true, 0, messagePrefix, 0, messagePrefix.length())) {
                    content = replaced.substring(messagePrefix.length()).trim();
                } else {
                    content = replaced;
                }
                player.sendMessage(ColorUtil.colorize(configManager.getPrefix() + content));
            }
        }
    }
}
