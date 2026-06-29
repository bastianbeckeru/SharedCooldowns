package com.sharedcooldowns.config

import com.sharedcooldowns.SharedCooldowns
import com.sharedcooldowns.cooldown.CooldownGroup
import org.bukkit.configuration.file.FileConfiguration

/**
 * Manages loading and reloading of the plugin configuration.
 * Parses cooldown-groups and individual-cooldowns into [CooldownGroup] instances.
 */
class ConfigManager(private val plugin: SharedCooldowns) {

    /** All configured cooldown groups. */
    var groups: List<CooldownGroup> = emptyList()
        private set

    /** Fast lookup: normalized command name → CooldownGroup it belongs to. */
    var commandToGroup: Map<String, CooldownGroup> = emptyMap()
        private set

    /** Global message prefix. */
    var prefix: String = ""
        private set

    /** Custom time formatting templates. */
    var compactFormat: String = "&b%days%&7d &b%hours%&7h &b%minutes%&7m &b%seconds%&7s"
        private set
    var singleFormat: String = "&b%value% %unit%"
        private set
    var timeUnits: Map<String, String> = emptyMap()
        private set

    init {
        plugin.saveDefaultConfig()
        load()
    }

    /**
     * Reloads the configuration from disk and re-parses all groups.
     */
    fun reload() {
        plugin.reloadConfig()
        load()
    }

    private fun load() {
        val config: FileConfiguration = plugin.config
        val parsedGroups = mutableListOf<CooldownGroup>()

        // --- Parse prefix ---
        prefix = config.getString("prefix", "") ?: ""

        // --- Parse time formats ---
        compactFormat = config.getString("time.compact", "&b%days%&7d &b%hours%&7h &b%minutes%&7m &b%seconds%&7s")
            ?: "&b%days%&7d &b%hours%&7h &b%minutes%&7m &b%seconds%&7s"
        singleFormat = config.getString("time.single", "&b%value% %unit%") ?: "&b%value% %unit%"

        val unitsMap = mutableMapOf<String, String>()
        val unitsSection = config.getConfigurationSection("time.units")
        if (unitsSection != null) {
            for (key in unitsSection.getKeys(false)) {
                unitsMap[key] = unitsSection.getString(key) ?: key
            }
        } else {
            // Default units fallback
            unitsMap["day"] = "day"
            unitsMap["days"] = "days"
            unitsMap["hour"] = "hour"
            unitsMap["hours"] = "hours"
            unitsMap["minute"] = "minute"
            unitsMap["minutes"] = "minutes"
            unitsMap["second"] = "second"
            unitsMap["seconds"] = "seconds"
        }
        timeUnits = unitsMap

        // --- Parse cooldowns ---
        val cooldownsSection = config.getConfigurationSection("cooldowns")
        cooldownsSection?.getKeys(false)?.forEach { groupId ->
            val section = cooldownsSection.getConfigurationSection(groupId) ?: return@forEach
            val commands = section.getStringList("commands")
                .map { normalizeCommand(it) }
                .toSet()
            val cooldown = section.getInt("cooldown", 30)
            val actions = if (section.contains("actions")) {
                section.getStringList("actions")
            } else {
                val msg = section.getString("message")
                if (msg != null) listOf(msg) else listOf("&cCooldown active. Wait %time_compact%.")
            }
            val conditional = section.getBoolean("conditional", false)

            if (commands.isNotEmpty()) {
                parsedGroups += CooldownGroup(
                    id = "group:$groupId",
                    commands = commands,
                    cooldownSeconds = cooldown,
                    actions = actions,
                    conditional = conditional
                )
            }
        }

        groups = parsedGroups

        // Build command → group lookup map
        val lookup = mutableMapOf<String, CooldownGroup>()
        for (group in parsedGroups) {
            for (cmd in group.commands) {
                lookup[cmd] = group
            }
        }
        commandToGroup = lookup

        plugin.logger.info("Loaded ${parsedGroups.size} cooldown group(s) with ${lookup.size} command(s).")
    }

    /**
     * Formats remaining time in seconds into a compact string (%time_compact%).
     */
    fun formatTimeCompact(remainingSeconds: Long): String {
        val days = remainingSeconds / 86400
        val hours = (remainingSeconds % 86400) / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60

        val parts = compactFormat.split("\\s+".toRegex())
        val resultParts = mutableListOf<String>()

        for (part in parts) {
            when {
                part.contains("%days%") -> {
                    if (days > 0) resultParts.add(part.replace("%days%", days.toString()))
                }
                part.contains("%hours%") -> {
                    if (hours > 0) resultParts.add(part.replace("%hours%", hours.toString()))
                }
                part.contains("%minutes%") -> {
                    if (minutes > 0) resultParts.add(part.replace("%minutes%", minutes.toString()))
                }
                part.contains("%seconds%") -> {
                    if (seconds > 0 || resultParts.isEmpty()) {
                        resultParts.add(part.replace("%seconds%", seconds.toString()))
                    }
                }
                else -> {
                    resultParts.add(part)
                }
            }
        }
        return resultParts.joinToString(" ")
    }

    /**
     * Formats remaining time in seconds to the single largest unit only (%time_single%).
     */
    fun formatTimeSingle(remainingSeconds: Long): String {
        val value: Long
        val unitKey: String
        when {
            remainingSeconds >= 86400 -> {
                value = remainingSeconds / 86400
                unitKey = if (value == 1L) "day" else "days"
            }
            remainingSeconds >= 3600 -> {
                value = remainingSeconds / 3600
                unitKey = if (value == 1L) "hour" else "hours"
            }
            remainingSeconds >= 60 -> {
                value = remainingSeconds / 60
                unitKey = if (value == 1L) "minute" else "minutes"
            }
            else -> {
                value = remainingSeconds
                unitKey = if (value == 1L) "second" else "seconds"
            }
        }
        val unit = timeUnits[unitKey] ?: unitKey
        return singleFormat
            .replace("%value%", value.toString())
            .replace("%unit%", unit)
    }

    /**
     * Normalizes a command string: lowercase, strip leading '/'.
     * Example: "/Home" → "home", "spawn" → "spawn"
     */
    private fun normalizeCommand(cmd: String): String {
        return cmd.trim().removePrefix("/").lowercase()
    }
}
