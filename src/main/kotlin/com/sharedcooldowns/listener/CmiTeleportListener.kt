package com.sharedcooldowns.listener

import com.Zrips.CMI.events.CMIAsyncPlayerTeleportEvent
import com.sharedcooldowns.SharedCooldowns
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/**
 * CMI-specific teleport listener. Only registered when CMI is present on the server.
 *
 * Listens to [CMIAsyncPlayerTeleportEvent] which fires when CMI processes a teleport.
 * This provides more accurate detection than the generic [PlayerTeleportEvent]
 * for CMI-managed commands like /home, /spawn, /tpa, /warp, etc.
 */
class CmiTeleportListener(private val plugin: SharedCooldowns) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCmiTeleport(event: CMIAsyncPlayerTeleportEvent) {
        val player = event.getPlayer() ?: return
        val registry = plugin.cooldownRegistry

        // Only process if this player has a pending conditional cooldown
        if (!registry.hasPending(player.uniqueId)) return

        // CMI teleport event fired and not cancelled — confirm the cooldown
        val confirmed = registry.confirmPending(player.uniqueId)
        if (confirmed) {
            plugin.logger.fine("Cooldown confirmed for ${player.name} via CMI teleport event.")
        }
    }
}
