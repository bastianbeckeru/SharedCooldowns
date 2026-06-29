package com.sharedcooldowns.listener

import com.sharedcooldowns.SharedCooldowns
import com.sharedcooldowns.util.ColorUtil.colorize
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Intercepts player commands to enforce cooldowns.
 *
 * Uses HIGHEST priority to run after most other plugins have processed
 * the event, but before MONITOR. This ensures we don't block commands
 * that were already cancelled by other plugins.
 */
class CommandListener(private val plugin: SharedCooldowns) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player

        // Bypass permission check
        if (player.hasPermission("sharedcooldowns.bypass")) return

        // Extract base command from the message: "/home arg1 arg2" → "home"
        val message = event.message
        val baseCommand = extractBaseCommand(message) ?: return

        // Find the cooldown group for this command
        val configManager = plugin.configManager
        val group = configManager.commandToGroup[baseCommand] ?: return

        val registry = plugin.cooldownRegistry
        val playerUUID = player.uniqueId

        // Check if player has an active cooldown for this group
        val remaining = registry.getRemainingSeconds(playerUUID, group.id)
        if (remaining > 0) {
            // Block the command and execute cooldown actions
            event.isCancelled = true
            executeActions(player, group.actions, remaining, baseCommand)
            return
        }

        // No active cooldown — decide whether to apply immediately or defer
        if (group.conditional) {
            // Conditional: mark as pending, cooldown applied only when action completes
            registry.markPending(playerUUID, group)
        } else {
            // Non-conditional: apply cooldown immediately
            registry.applyCooldown(playerUUID, group.id, group.cooldownSeconds)
        }

    }

    /**
     * Clean up player data when they disconnect.
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.cooldownRegistry.cancelPending(event.player.uniqueId)
    }

    /**
     * Extracts the configured base command or subcommand match from a chat message.
     * Supports multi-word commands (like "cmi home"), CMI subcommands fallback, and namespaces.
     */
    private fun extractBaseCommand(message: String): String? {
        if (!message.startsWith("/")) return null

        val configManager = plugin.configManager

        // Helper to strip namespace from a command name (e.g. "cmi:home" -> "home")
        fun stripNamespace(cmd: String): String {
            return if (cmd.contains(":")) cmd.substringAfter(":") else cmd
        }

        // Helper to find a match from a list of command parts
        fun findMatch(cmdParts: List<String>): String? {
            // Check prefixes from longest to shortest
            for (i in cmdParts.size downTo 1) {
                val checkCmd = cmdParts.take(i).joinToString(" ")
                
                // Direct match
                if (configManager.commandToGroup.containsKey(checkCmd)) {
                    return checkCmd
                }
                
                // Namespace check on the first part of this subsegment
                val firstPart = cmdParts[0]
                if (firstPart.contains(":")) {
                    val cleanFirstPart = stripNamespace(firstPart)
                    val newParts = listOf(cleanFirstPart) + cmdParts.drop(1)
                    val checkCmdNoNamespace = newParts.take(i).joinToString(" ")
                    if (configManager.commandToGroup.containsKey(checkCmdNoNamespace)) {
                        return checkCmdNoNamespace
                    }
                }
            }
            return null
        }

        val cleanMessage = message.substring(1).trim().lowercase()
        if (cleanMessage.isEmpty()) return null

        val parts = cleanMessage.split("\\s+".toRegex())

        // 1. Try matching the full command parts directly (handles "/cmi home args" matching "cmi home")
        val directMatch = findMatch(parts)
        if (directMatch != null) {
            return directMatch
        }

        // 2. CMI Subcommand fallback: If player ran "/cmi home args" but config has only "home" (not "cmi home")
        if (parts[0] == "cmi" && parts.size >= 2) {
            val subParts = parts.drop(1)
            val cmiMatch = findMatch(subParts)
            if (cmiMatch != null) {
                return cmiMatch
            }
        }

        // 3. General namespace fallback for the first command part (e.g., "/cmi:home args" -> check "home args")
        if (parts[0].contains(":")) {
            val cleanFirstPart = stripNamespace(parts[0])
            val subParts = listOf(cleanFirstPart) + parts.drop(1)
            val nsMatch = findMatch(subParts)
            if (nsMatch != null) {
                return nsMatch
            }
        }

        return null
    }

    /**
     * Executes sequential actions for a player under cooldown.
     */
    private fun executeActions(
        player: Player,
        actions: List<String>,
        remaining: Long,
        baseCommand: String
    ) {
        val configManager = plugin.configManager
        val actionbarPrefix = "[actionbar]"
        val titlePrefix = "[title]"
        val soundPrefix = "[sound]"
        val messagePrefix = "[message]"

        actions.forEach { action ->
            val replaced = action
                .replace("%time_compact%", configManager.formatTimeCompact(remaining))
                .replace("%time_single%", configManager.formatTimeSingle(remaining))
                .replace("%player%", player.name)
                .replace("%command%", "/$baseCommand")

            when {
                replaced.startsWith(actionbarPrefix, ignoreCase = true) -> {
                    val content = replaced.substring(actionbarPrefix.length).trim()
                    player.sendActionBar(content.colorize())
                }
                replaced.startsWith(titlePrefix, ignoreCase = true) -> {
                    val content = replaced.substring(titlePrefix.length).trim()
                    val titleParts = content.split(";", limit = 2)
                    val titleText = titleParts.getOrNull(0)?.trim() ?: ""
                    val subtitleText = titleParts.getOrNull(1)?.trim() ?: ""
                    player.showTitle(
                        net.kyori.adventure.title.Title.title(
                            titleText.colorize(),
                            subtitleText.colorize()
                        )
                    )
                }
                replaced.startsWith(soundPrefix, ignoreCase = true) -> {
                    val content = replaced.substring(soundPrefix.length).trim()
                    val soundParams = content.split("\\s+".toRegex())
                    val soundKey = soundParams.getOrNull(0) ?: return@forEach
                    val volume = soundParams.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                    val pitch = soundParams.getOrNull(2)?.toFloatOrNull() ?: 1.0f
                    try {
                        player.playSound(player.location, soundKey, volume, pitch)
                    } catch (e: Exception) {
                        // Ignore sound play exceptions
                    }
                }
                else -> {
                    val content = if (replaced.startsWith(messagePrefix, ignoreCase = true)) {
                        replaced.substring(messagePrefix.length).trim()
                    } else {
                        replaced
                    }
                    player.sendMessage((configManager.prefix + content).colorize())
                }
            }
        }
    }
}
