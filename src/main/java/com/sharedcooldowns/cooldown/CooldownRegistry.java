package com.sharedcooldowns.cooldown;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of active cooldowns and pending conditional actions.
 *
 * Thread-safe via ConcurrentHashMap. No schedulers — uses lazy cleanup
 * on every access to purge expired entries.
 */
public class CooldownRegistry {

    /** Pending actions expire after 30 seconds if not confirmed. */
    private static final long PENDING_TIMEOUT_MS = 30_000L;

    /**
     * Active cooldowns: playerUUID → (groupId → expiresAtMillis).
     */
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> activeCooldowns = new ConcurrentHashMap<>();

    /**
     * Pending conditional actions: playerUUID → PendingAction.
     * When a conditional command (e.g. /home) is executed, we store it here
     * and only apply the cooldown when the action (teleport) completes.
     */
    private final ConcurrentHashMap<UUID, PendingAction> pendingActions = new ConcurrentHashMap<>();

    /**
     * Returns the remaining cooldown time in seconds for a player and group.
     * Returns 0 if no cooldown is active or it has expired.
     */
    public long getRemainingSeconds(UUID playerUUID, String groupId) {
        ConcurrentHashMap<String, Long> playerCooldowns = activeCooldowns.get(playerUUID);
        if (playerCooldowns == null) {
            return 0;
        }

        Long expiresAt = playerCooldowns.get(groupId);
        if (expiresAt == null) {
            return 0;
        }

        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;

        if (remaining <= 0) {
            // Lazy cleanup: remove expired entry
            playerCooldowns.remove(groupId);
            if (playerCooldowns.isEmpty()) {
                activeCooldowns.remove(playerUUID, playerCooldowns);
            }
            return 0;
        }

        return remaining;
    }

    /**
     * Checks if a player has an active cooldown for the given group.
     */
    public boolean hasCooldown(UUID playerUUID, String groupId) {
        return getRemainingSeconds(playerUUID, groupId) > 0;
    }

    /**
     * Applies a cooldown to a player for a specific group.
     *
     * @param playerUUID      The player's UUID.
     * @param groupId         The cooldown group identifier.
     * @param durationSeconds Cooldown duration in seconds.
     */
    public void applyCooldown(UUID playerUUID, String groupId, int durationSeconds) {
        ConcurrentHashMap<String, Long> playerCooldowns = activeCooldowns.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        playerCooldowns.put(groupId, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    /**
     * Marks that a conditional command was executed and is awaiting confirmation
     * (e.g. a teleport needs to complete before the cooldown is applied).
     *
     * @param playerUUID The player who executed the command.
     * @param group      The cooldown group the command belongs to.
     */
    public void markPending(UUID playerUUID, CooldownGroup group) {
        pendingActions.put(playerUUID, new PendingAction(group, System.currentTimeMillis()));
    }

    /**
     * Confirms a pending action (e.g. teleport completed) and applies the cooldown.
     *
     * @param playerUUID The player whose pending action completed.
     * @return true if a pending action was found and cooldown was applied.
     */
    public boolean confirmPending(UUID playerUUID) {
        PendingAction pending = pendingActions.remove(playerUUID);
        if (pending == null) {
            return false;
        }

        // Check if the pending action has expired (30s timeout)
        if (System.currentTimeMillis() - pending.createdAt() > PENDING_TIMEOUT_MS) {
            return false;
        }

        applyCooldown(playerUUID, pending.group().id(), pending.group().cooldownSeconds());
        return true;
    }

    /**
     * Cancels a pending action without applying the cooldown.
     *
     * @param playerUUID The player whose pending action should be cancelled.
     */
    public void cancelPending(UUID playerUUID) {
        pendingActions.remove(playerUUID);
    }

    /**
     * Checks whether a player has a pending conditional action.
     */
    public boolean hasPending(UUID playerUUID) {
        PendingAction pending = pendingActions.get(playerUUID);
        if (pending == null) {
            return false;
        }

        // Auto-expire stale pending actions
        if (System.currentTimeMillis() - pending.createdAt() > PENDING_TIMEOUT_MS) {
            pendingActions.remove(playerUUID, pending);
            return false;
        }

        return true;
    }

    /**
     * Clears all cooldowns and pending actions. Used on plugin reload.
     */
    public void clearAll() {
        activeCooldowns.clear();
        pendingActions.clear();
    }

    /**
     * Removes all data for a specific player. Useful on player quit.
     */
    public void clearPlayer(UUID playerUUID) {
        activeCooldowns.remove(playerUUID);
        pendingActions.remove(playerUUID);
    }

    /**
     * Represents a command execution awaiting confirmation before cooldown applies.
     */
    private record PendingAction(CooldownGroup group, long createdAt) {}
}
