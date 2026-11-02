package com.sharedcooldowns.config;

import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.cooldown.CooldownGroup;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import com.sharedcooldowns.util.TimeFormatter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages loading and reloading of the plugin configuration.
 * Parses cooldown-groups and individual-cooldowns into {@link CooldownGroup} instances.
 */
public class ConfigManager {

    private static final int DEFAULT_PENDING_TIMEOUT = 120;
    private static final List<String> DEFAULT_TELEPORT_CAUSES = List.of("COMMAND", "PLUGIN");

    private final SharedCooldowns plugin;

    private List<CooldownGroup> groups = List.of();
    private Map<String, CooldownGroup> commandToGroup = Map.of();
    private String prefix = "";
    private String compactFormat = "&b%days%&7d &b%hours%&7h &b%minutes%&7m &b%seconds%&7s";
    private String singleFormat = "&b%value% %unit%";
    private Map<String, String> timeUnits = Map.of();
    private int pendingTimeoutSeconds = DEFAULT_PENDING_TIMEOUT;
    private Set<TeleportCause> teleportCauses = Set.of();

    public ConfigManager(SharedCooldowns plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        load();
    }

    /**
     * Reloads the configuration from disk and re-parses all groups.
     */
    public void reload() {
        plugin.reloadConfig();
        load();
    }

    private void load() {
        FileConfiguration config = plugin.getConfig();
        List<CooldownGroup> parsedGroups = new ArrayList<>();

        // --- Parse prefix ---
        prefix = config.getString("prefix", "");
        if (prefix == null) {
            prefix = "";
        }

        // --- Parse conditional settings ---
        pendingTimeoutSeconds = config.getInt("pending-timeout", DEFAULT_PENDING_TIMEOUT);
        List<String> causeNames = config.contains("teleport-causes")
                ? config.getStringList("teleport-causes") : DEFAULT_TELEPORT_CAUSES;
        Set<TeleportCause> causes = EnumSet.noneOf(TeleportCause.class);
        for (String name : causeNames) {
            try {
                causes.add(TeleportCause.valueOf(name.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown teleport cause in config.yml: " + name);
            }
        }
        teleportCauses = Set.copyOf(causes);

        // --- Parse time formats ---
        compactFormat = config.getString("time.compact", "&b%days%&7d &b%hours%&7h &b%minutes%&7m &b%seconds%&7s");
        if (compactFormat == null) {
            compactFormat = "&b%days%&7d &b%hours%&7h &b%minutes%&7m &b%seconds%&7s";
        }
        singleFormat = config.getString("time.single", "&b%value% %unit%");
        if (singleFormat == null) {
            singleFormat = "&b%value% %unit%";
        }

        Map<String, String> unitsMap = new HashMap<>();
        ConfigurationSection unitsSection = config.getConfigurationSection("time.units");
        if (unitsSection != null) {
            for (String key : unitsSection.getKeys(false)) {
                String val = unitsSection.getString(key);
                unitsMap.put(key, val != null ? val : key);
            }
        } else {
            // Default units fallback
            unitsMap.put("day", "day");
            unitsMap.put("days", "days");
            unitsMap.put("hour", "hour");
            unitsMap.put("hours", "hours");
            unitsMap.put("minute", "minute");
            unitsMap.put("minutes", "minutes");
            unitsMap.put("second", "second");
            unitsMap.put("seconds", "seconds");
        }
        timeUnits = Map.copyOf(unitsMap);

        // --- Parse cooldowns ---
        ConfigurationSection cooldownsSection = config.getConfigurationSection("cooldowns");
        if (cooldownsSection != null) {
            for (String groupId : cooldownsSection.getKeys(false)) {
                ConfigurationSection section = cooldownsSection.getConfigurationSection(groupId);
                if (section == null) {
                    continue;
                }
                
                List<String> rawCommands = section.getStringList("commands");
                Set<String> commands = rawCommands.stream()
                        .map(this::normalizeCommand)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                
                int cooldown = section.getInt("cooldown", 30);
                
                List<String> actions;
                if (section.contains("actions")) {
                    actions = section.getStringList("actions");
                } else {
                    String msg = section.getString("message");
                    if (msg != null) {
                        actions = List.of(msg);
                    } else {
                        actions = List.of("&cCooldown active. Wait %time_compact%.");
                    }
                }
                
                boolean conditional = section.getBoolean("conditional", false);

                if (!commands.isEmpty()) {
                    parsedGroups.add(new CooldownGroup(
                        groupId,
                        commands,
                        cooldown,
                        actions,
                        conditional
                    ));
                }
            }
        }

        groups = List.copyOf(parsedGroups);

        // Build command → group lookup map
        Map<String, CooldownGroup> lookup = new HashMap<>();
        for (CooldownGroup group : parsedGroups) {
            for (String cmd : group.commands()) {
                lookup.put(cmd, group);
            }
        }
        commandToGroup = Map.copyOf(lookup);

        plugin.getLogger().info("Loaded " + parsedGroups.size() + " cooldown group(s) with " + lookup.size() + " command(s).");
    }

    /**
     * Formats remaining time in seconds into a compact string (%time_compact%).
     */
    public String formatTimeCompact(long remainingSeconds) {
        return TimeFormatter.compact(compactFormat, remainingSeconds);
    }

    /**
     * Formats remaining time in seconds to the single largest unit only (%time_single%).
     */
    public String formatTimeSingle(long remainingSeconds) {
        long value;
        String unitKey;
        if (remainingSeconds >= 86400) {
            value = remainingSeconds / 86400;
            unitKey = (value == 1) ? "day" : "days";
        } else if (remainingSeconds >= 3600) {
            value = remainingSeconds / 3600;
            unitKey = (value == 1) ? "hour" : "hours";
        } else if (remainingSeconds >= 60) {
            value = remainingSeconds / 60;
            unitKey = (value == 1) ? "minute" : "minutes";
        } else {
            value = remainingSeconds;
            unitKey = (value == 1) ? "second" : "seconds";
        }
        
        String unit = timeUnits.getOrDefault(unitKey, unitKey);
        return singleFormat
            .replace("%value%", String.valueOf(value))
            .replace("%unit%", unit);
    }

    /**
     * Normalizes a command string: lowercase, strip leading '/'.
     * Example: "/Home" → "home", "spawn" → "spawn"
     */
    private String normalizeCommand(String cmd) {
        if (cmd == null) {
            return "";
        }
        String trimmed = cmd.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.toLowerCase();
    }

    public List<CooldownGroup> getGroups() {
        return groups;
    }

    public Map<String, CooldownGroup> getCommandToGroup() {
        return commandToGroup;
    }

    public int getPendingTimeoutSeconds() {
        return pendingTimeoutSeconds;
    }

    public Set<TeleportCause> getTeleportCauses() {
        return teleportCauses;
    }

    /** Returns a message from the config, falling back to the plugin's bundled defaults. */
    public String getMessage(String key) {
        return Objects.requireNonNullElse(plugin.getConfig().getString("messages." + key), "");
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCompactFormat() {
        return compactFormat;
    }

    public String getSingleFormat() {
        return singleFormat;
    }

    public Map<String, String> getTimeUnits() {
        return timeUnits;
    }
}
