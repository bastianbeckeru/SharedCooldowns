package com.sharedcooldowns.cooldown;

/**
 * Represents an active cooldown entry for a specific player and group.
 *
 * @param expiresAt Timestamp (System.currentTimeMillis) when this cooldown expires.
 * @param groupId   The CooldownGroup id this entry belongs to.
 */
public record CooldownEntry(long expiresAt, String groupId) {}
