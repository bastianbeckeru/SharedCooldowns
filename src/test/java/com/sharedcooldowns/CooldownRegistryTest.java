package com.sharedcooldowns.cooldown;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CooldownRegistryTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final int TIMEOUT = 120;
    private static final CooldownGroup TELEPORTS =
        new CooldownGroup("teleports", Set.of("home", "tpa"), 600, List.of(), true);

    private final Map<String, Long> saved = new HashMap<>();
    private final CooldownStore store = new CooldownStore() {
        @Override public long getExpiresAt(UUID id, String group) { return saved.getOrDefault(group, 0L); }
        @Override public void setExpiresAt(UUID id, String group, long at) { saved.put(group, at); }
        @Override public void remove(UUID id, String group) { saved.remove(group); }
        @Override public void removeAll(UUID id) { saved.clear(); }
    };
    private long now = 1_000_000L;
    private final CooldownRegistry registry = new CooldownRegistry(store, () -> now);

    private void seconds(long amount) {
        now += amount * 1000;
    }

    @Test
    void remainingRoundsUpSoCooldownNeverEndsEarly() {
        registry.applyCooldown(PLAYER, TELEPORTS);
        now += 599_100;
        assertEquals(1, registry.getRemainingSeconds(PLAYER, TELEPORTS));
        now += 900;
        assertEquals(0, registry.getRemainingSeconds(PLAYER, TELEPORTS));
        assertTrue(saved.isEmpty());
    }

    @Test
    void loweringTheConfiguredDurationCapsRunningCooldowns() {
        registry.applyCooldown(PLAYER, TELEPORTS);
        CooldownGroup shorter = new CooldownGroup("teleports", Set.of("home"), 60, List.of(), true);
        assertEquals(60, registry.getRemainingSeconds(PLAYER, shorter));
    }

    @Test
    void cooldownSurvivesANewRegistryWithTheSameStore() {
        registry.applyCooldown(PLAYER, TELEPORTS);
        CooldownRegistry afterRestart = new CooldownRegistry(store, () -> now);
        assertEquals(600, afterRestart.getRemainingSeconds(PLAYER, TELEPORTS));
    }

    @Test
    void clearingPendingKeepsRunningCooldowns() {
        registry.applyCooldown(PLAYER, TELEPORTS);
        registry.clearAllPending();
        assertEquals(600, registry.getRemainingSeconds(PLAYER, TELEPORTS));
    }

    @Test
    void teleportWithoutPendingCommandDoesNothing() {
        assertFalse(registry.confirmTeleport(PLAYER));
        assertEquals(0, registry.getRemainingSeconds(PLAYER, TELEPORTS));
    }

    @Test
    void teleportStartsCooldownOfPendingCommand() {
        registry.markPending(PLAYER, TELEPORTS, "home", TIMEOUT);
        assertNull(registry.takeBlockedTeleport(PLAYER));
        assertTrue(registry.confirmTeleport(PLAYER));
        assertEquals(600, registry.getRemainingSeconds(PLAYER, TELEPORTS));
    }

    @Test
    void tpaAcceptedLateStillStartsCooldown() {
        registry.markPending(PLAYER, TELEPORTS, "tpa", TIMEOUT);
        seconds(63); // CMI: 60 s to accept + 3 s warmup
        assertTrue(registry.confirmTeleport(PLAYER));
        assertEquals(600, registry.getRemainingSeconds(PLAYER, TELEPORTS));
    }

    @Test
    void pendingCommandExpiresAfterTimeout() {
        registry.markPending(PLAYER, TELEPORTS, "home", TIMEOUT);
        seconds(TIMEOUT);
        assertFalse(registry.confirmTeleport(PLAYER));
        assertEquals(0, registry.getRemainingSeconds(PLAYER, TELEPORTS));
    }

    @Test
    void secondQueuedTeleportIsBlockedDuringCooldown() {
        registry.markPending(PLAYER, TELEPORTS, "tpa", TIMEOUT);
        registry.markPending(PLAYER, TELEPORTS, "home", TIMEOUT);

        assertNull(registry.takeBlockedTeleport(PLAYER));
        assertTrue(registry.confirmTeleport(PLAYER));

        seconds(10);
        CooldownRegistry.Pending blocked = registry.takeBlockedTeleport(PLAYER);
        assertNotNull(blocked);
        assertEquals("home", blocked.command());
        assertNull(registry.takeBlockedTeleport(PLAYER));
    }

    @Test
    void resetClearsCooldownsAndPendingCommandsAndCountsActiveOnes() {
        registry.applyCooldown(PLAYER, TELEPORTS);
        registry.markPending(PLAYER, TELEPORTS, "tpa", TIMEOUT);

        assertEquals(1, registry.resetAll(PLAYER, List.of(TELEPORTS)));

        assertEquals(0, registry.getRemainingSeconds(PLAYER, TELEPORTS));
        assertTrue(saved.isEmpty());
        assertFalse(registry.confirmTeleport(PLAYER));
        assertEquals(0, registry.resetAll(PLAYER, List.of(TELEPORTS)));
    }

    @Test
    void quittingForgetsPendingCommands() {
        registry.markPending(PLAYER, TELEPORTS, "home", TIMEOUT);
        registry.clearPending(PLAYER);
        assertFalse(registry.confirmTeleport(PLAYER));
    }

    @Test
    void pendingQueueIsBounded() {
        for (int i = 0; i < CooldownRegistry.MAX_PENDING_PER_PLAYER + 5; i++) {
            registry.markPending(PLAYER, TELEPORTS, "home", TIMEOUT);
        }
        int confirmed = 0;
        while (registry.confirmTeleport(PLAYER)) {
            confirmed++;
        }
        assertEquals(CooldownRegistry.MAX_PENDING_PER_PLAYER, confirmed);
    }
}
