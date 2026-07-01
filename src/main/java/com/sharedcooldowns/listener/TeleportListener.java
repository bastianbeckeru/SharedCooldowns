package com.sharedcooldowns.listener;

import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.cooldown.CooldownRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Level;

/**
 * Listens for teleport events to confirm conditional cooldowns.
 *
 * Dual strategy:
 * 1. CMI events (if CMI is present) — handled by {@link CmiTeleportListener}.
 * 2. Paper's {@link PlayerTeleportEvent} at MONITOR priority as fallback.
 *
 * MONITOR priority ensures all other plugins have already decided whether
 * to cancel the event. We only confirm the cooldown if the teleport
 * was NOT cancelled.
 */
public class TeleportListener implements Listener {

    private final SharedCooldowns plugin;

    public TeleportListener(SharedCooldowns plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        CooldownRegistry registry = plugin.getCooldownRegistry();

        // Only process if this player has a pending conditional cooldown
        if (!registry.hasPending(player.getUniqueId())) {
            return;
        }

        // The teleport was not cancelled (ignoreCancelled = true ensures this),
        // so confirm the pending action and apply the cooldown.
        boolean confirmed = registry.confirmPending(player.getUniqueId());
        if (confirmed) {
            plugin.getLogger().log(Level.FINE, "Cooldown confirmed for " + player.getName() + " via generic teleport event.");
        }
    }
}
