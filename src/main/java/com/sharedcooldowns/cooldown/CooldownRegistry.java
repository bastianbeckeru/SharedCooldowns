package com.sharedcooldowns.cooldown;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Tracks active cooldowns and conditional commands waiting for their teleport.
 *
 * Every conditional command queues its own pending entry, oldest first. The first
 * teleport starts the cooldown; a later teleport from another queued command (for
 * example a /tpa accepted afterwards) arrives during the cooldown and is blocked.
 *
 * Not thread-safe: use from the main server thread only.
 */
public class CooldownRegistry {

    /** Bounds memory if a player spams commands that never teleport. */
    static final int MAX_PENDING_PER_PLAYER = 16;

    private final CooldownStore store;
    private final LongSupplier clock;
    private final Map<UUID, Deque<Pending>> pending = new HashMap<>();

    public CooldownRegistry(CooldownStore store, LongSupplier clock) {
        this.store = store;
        this.clock = clock;
    }

    /**
     * Returns the remaining seconds, rounded up so a cooldown never ends early.
     * Capped at the group's duration, so lowering it in the config takes effect
     * on cooldowns that are already running.
     */
    public long getRemainingSeconds(UUID playerId, CooldownGroup group) {
        long expiresAt = store.getExpiresAt(playerId, group.id());
        if (expiresAt == 0) {
            return 0;
        }
        long remainingMillis = Math.min(expiresAt - clock.getAsLong(), group.cooldownSeconds() * 1000L);
        if (remainingMillis <= 0) {
            store.remove(playerId, group.id());
            return 0;
        }
        return (remainingMillis + 999) / 1000;
    }

    public void applyCooldown(UUID playerId, CooldownGroup group) {
        store.setExpiresAt(playerId, group.id(), clock.getAsLong() + group.cooldownSeconds() * 1000L);
    }

    /** Queues a conditional command that waits up to {@code timeoutSeconds} for its teleport. */
    public void markPending(UUID playerId, CooldownGroup group, String command, int timeoutSeconds) {
        Deque<Pending> queue = pending.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        if (queue.size() >= MAX_PENDING_PER_PLAYER) {
            queue.pollFirst();
        }
        queue.addLast(new Pending(group, command, clock.getAsLong() + timeoutSeconds * 1000L));
    }

    /**
     * Returns the pending command if this teleport must be blocked because its
     * group is already on cooldown, consuming it. Returns null if it may proceed.
     */
    public Pending takeBlockedTeleport(UUID playerId) {
        Pending oldest = oldestLive(playerId);
        if (oldest == null || getRemainingSeconds(playerId, oldest.group()) == 0) {
            return null;
        }
        removeOldest(playerId);
        return oldest;
    }

    /** Consumes the oldest pending command after a teleport and starts its cooldown. */
    public boolean confirmTeleport(UUID playerId) {
        Pending oldest = oldestLive(playerId);
        if (oldest == null) {
            return false;
        }
        removeOldest(playerId);
        applyCooldown(playerId, oldest.group());
        return true;
    }

    /**
     * Clears all of a player's cooldowns and pending commands, so a teleport still
     * on its way cannot restart the cooldown.
     *
     * @return how many of the given groups had an active cooldown
     */
    public int resetAll(UUID playerId, Collection<CooldownGroup> groups) {
        int active = 0;
        for (CooldownGroup group : groups) {
            if (getRemainingSeconds(playerId, group) > 0) {
                active++;
            }
        }
        store.removeAll(playerId);
        clearPending(playerId);
        return active;
    }

    public void clearPending(UUID playerId) {
        pending.remove(playerId);
    }

    public void clearAllPending() {
        pending.clear();
    }

    private Pending oldestLive(UUID playerId) {
        Deque<Pending> queue = pending.get(playerId);
        if (queue == null) {
            return null;
        }
        long now = clock.getAsLong();
        queue.removeIf(entry -> entry.expiresAt() <= now);
        if (queue.isEmpty()) {
            pending.remove(playerId);
            return null;
        }
        return queue.peekFirst();
    }

    private void removeOldest(UUID playerId) {
        Deque<Pending> queue = pending.get(playerId);
        queue.pollFirst();
        if (queue.isEmpty()) {
            pending.remove(playerId);
        }
    }

    /** A conditional command waiting for its teleport. */
    public record Pending(CooldownGroup group, String command, long expiresAt) {}
}
