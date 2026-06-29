package com.sharedcooldowns.listener

import com.sharedcooldowns.SharedCooldowns
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent

/**
 * Listens for teleport events to confirm conditional cooldowns.
 *
 * Dual strategy:
 * 1. CMI events (if CMI is present) — handled by [CmiTeleportListener].
 * 2. Paper's [PlayerTeleportEvent] at MONITOR priority as fallback.
 *
 * MONITOR priority ensures all other plugins have already decided whether
 * to cancel the event. We only confirm the cooldown if the teleport
 * was NOT cancelled.
 */
class TeleportListener(private val plugin: SharedCooldowns) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        val registry = plugin.cooldownRegistry

        // Only process if this player has a pending conditional cooldown
        if (!registry.hasPending(player.uniqueId)) return

        // The teleport was not cancelled (ignoreCancelled = true ensures this),
        // so confirm the pending action and apply the cooldown.
        val confirmed = registry.confirmPending(player.uniqueId)
        if (confirmed) {
            plugin.logger.fine("Cooldown confirmed for ${player.name} via generic teleport event.")
        }
    }
}
