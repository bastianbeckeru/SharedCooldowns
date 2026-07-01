package com.sharedcooldowns.listener;

import com.Zrips.CMI.events.CMIAsyncPlayerTeleportEvent;
import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.cooldown.CooldownRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.logging.Level;

/**
 * CMI-specific teleport listener. Only registered when CMI is present on the server.
 *
 * Listens to {@link CMIAsyncPlayerTeleportEvent} which fires when CMI processes a teleport.
 * This provides more accurate detection than the generic {@link org.bukkit.event.player.PlayerTeleportEvent}
 * for CMI-managed commands like /home, /spawn, /tpa, /warp, etc.
 */
public class CmiTeleportListener implements Listener {

    private final SharedCooldowns plugin;

    public CmiTeleportListener(SharedCooldowns plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCmiTeleport(CMIAsyncPlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        CooldownRegistry registry = plugin.getCooldownRegistry();

        // Only process if this player has a pending conditional cooldown
        if (!registry.hasPending(player.getUniqueId())) {
            return;
        }

        // CMI teleport event fired and not cancelled — confirm the cooldown
        boolean confirmed = registry.confirmPending(player.getUniqueId());
        if (confirmed) {
            plugin.getLogger().log(Level.FINE, "Cooldown confirmed for " + player.getName() + " via CMI teleport event.");
        }
    }
}
