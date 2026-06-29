package com.sharedcooldowns.cooldown

/**
 * Represents an active cooldown entry for a specific player and group.
 *
 * @property expiresAt Timestamp (System.currentTimeMillis) when this cooldown expires.
 * @property groupId The CooldownGroup id this entry belongs to.
 */
data class CooldownEntry(
    val expiresAt: Long,
    val groupId: String
)
