package com.sharedcooldowns.listener;

import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.cooldown.CooldownRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Resolves conditional commands from the real teleport, which every plugin goes
 * through. Only the configured causes count, so ender pearls, chorus fruit and
 * portals neither start a cooldown nor get blocked.
 */
public class TeleportListener implements Listener {

    private final SharedCooldowns plugin;
    private final ActionExecutor actions;

    public TeleportListener(SharedCooldowns plugin) {
        this.plugin = plugin;
        this.actions = new ActionExecutor(plugin);
    }

    /** Blocks a teleport left over from a queued command once the cooldown is running. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleportCheck(PlayerTeleportEvent event) {
        if (!countsAsTeleport(event)) {
            return;
        }
        Player player = event.getPlayer();
        CooldownRegistry registry = plugin.getCooldownRegistry();
        CooldownRegistry.Pending blocked = registry.takeBlockedTeleport(player.getUniqueId());
        if (blocked != null) {
            event.setCancelled(true);
            actions.execute(player, blocked.group().actions(),
                    registry.getRemainingSeconds(player.getUniqueId(), blocked.group()), blocked.command());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportConfirm(PlayerTeleportEvent event) {
        if (countsAsTeleport(event)) {
            plugin.getCooldownRegistry().confirmTeleport(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getCooldownStore().purgeExpired(event.getPlayer(), System.currentTimeMillis());
    }

    private boolean countsAsTeleport(PlayerTeleportEvent event) {
        return plugin.getConfigManager().getTeleportCauses().contains(event.getCause());
    }
}
