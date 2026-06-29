package com.sharedcooldowns.cooldown

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory registry of active cooldowns and pending conditional actions.
 *
 * Thread-safe via ConcurrentHashMap. No schedulers — uses lazy cleanup
 * on every access to purge expired entries.
 */
class CooldownRegistry {

    /**
     * Active cooldowns: playerUUID → (groupId → expiresAtMillis).
     */
    private val activeCooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()

    /**
     * Pending conditional actions: playerUUID → PendingAction.
     * When a conditional command (e.g. /home) is executed, we store it here
     * and only apply the cooldown when the action (teleport) completes.
     */
    private val pendingActions = ConcurrentHashMap<UUID, PendingAction>()

    /**
     * Returns the remaining cooldown time in seconds for a player and group.
     * Returns 0 if no cooldown is active or it has expired.
     */
    fun getRemainingSeconds(playerUUID: UUID, groupId: String): Long {
        val playerCooldowns = activeCooldowns[playerUUID] ?: return 0
        val expiresAt = playerCooldowns[groupId] ?: return 0
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000

        if (remaining <= 0) {
            // Lazy cleanup: remove expired entry
            playerCooldowns.remove(groupId)
            if (playerCooldowns.isEmpty()) {
                activeCooldowns.remove(playerUUID)
            }
            return 0
        }

        return remaining
    }

    /**
     * Checks if a player has an active cooldown for the given group.
     */
    fun hasCooldown(playerUUID: UUID, groupId: String): Boolean {
        return getRemainingSeconds(playerUUID, groupId) > 0
    }

    /**
     * Applies a cooldown to a player for a specific group.
     *
     * @param playerUUID The player's UUID.
     * @param groupId The cooldown group identifier.
     * @param durationSeconds Cooldown duration in seconds.
     */
    fun applyCooldown(playerUUID: UUID, groupId: String, durationSeconds: Int) {
        val playerCooldowns = activeCooldowns.getOrPut(playerUUID) { ConcurrentHashMap() }
        playerCooldowns[groupId] = System.currentTimeMillis() + (durationSeconds * 1000L)
    }

    /**
     * Marks that a conditional command was executed and is awaiting confirmation
     * (e.g. a teleport needs to complete before the cooldown is applied).
     *
     * @param playerUUID The player who executed the command.
     * @param group The cooldown group the command belongs to.
     */
    fun markPending(playerUUID: UUID, group: CooldownGroup) {
        pendingActions[playerUUID] = PendingAction(
            group = group,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Confirms a pending action (e.g. teleport completed) and applies the cooldown.
     *
     * @param playerUUID The player whose pending action completed.
     * @return true if a pending action was found and cooldown was applied.
     */
    fun confirmPending(playerUUID: UUID): Boolean {
        val pending = pendingActions.remove(playerUUID) ?: return false

        // Check if the pending action has expired (30s timeout)
        if (System.currentTimeMillis() - pending.createdAt > PENDING_TIMEOUT_MS) {
            return false
        }

        applyCooldown(playerUUID, pending.group.id, pending.group.cooldownSeconds)
        return true
    }

    /**
     * Cancels a pending action without applying the cooldown.
     *
     * @param playerUUID The player whose pending action should be cancelled.
     */
    fun cancelPending(playerUUID: UUID) {
        pendingActions.remove(playerUUID)
    }

    /**
     * Checks whether a player has a pending conditional action.
     */
    fun hasPending(playerUUID: UUID): Boolean {
        val pending = pendingActions[playerUUID] ?: return false

        // Auto-expire stale pending actions
        if (System.currentTimeMillis() - pending.createdAt > PENDING_TIMEOUT_MS) {
            pendingActions.remove(playerUUID)
            return false
        }

        return true
    }

    /**
     * Clears all cooldowns and pending actions. Used on plugin reload.
     */
    fun clearAll() {
        activeCooldowns.clear()
        pendingActions.clear()
    }

    /**
     * Removes all data for a specific player. Useful on player quit.
     */
    fun clearPlayer(playerUUID: UUID) {
        activeCooldowns.remove(playerUUID)
        pendingActions.remove(playerUUID)
    }

    /**
     * Represents a command execution awaiting confirmation before cooldown applies.
     */
    private data class PendingAction(
        val group: CooldownGroup,
        val createdAt: Long
    )

    companion object {
        /** Pending actions expire after 30 seconds if not confirmed. */
        private const val PENDING_TIMEOUT_MS = 30_000L
    }
}
