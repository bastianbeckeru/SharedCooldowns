package com.sharedcooldowns.cooldown;

import java.util.UUID;

/** Durable storage for cooldown expiry timestamps (epoch milliseconds). */
public interface CooldownStore {

    /** Returns the stored expiry, or 0 if there is none. */
    long getExpiresAt(UUID playerId, String groupId);

    void setExpiresAt(UUID playerId, String groupId, long expiresAt);

    void remove(UUID playerId, String groupId);

    /** Removes every stored cooldown of the player, including groups no longer configured. */
    void removeAll(UUID playerId);
}
