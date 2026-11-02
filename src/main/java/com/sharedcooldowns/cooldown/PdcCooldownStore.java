package com.sharedcooldowns.cooldown;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores expiry timestamps in each player's PersistentDataContainer, which the
 * server saves with the player's data. Commands are only run by online players,
 * so this needs no database, no extra files and no background tasks.
 */
public class PdcCooldownStore implements CooldownStore {

    private static final String KEY_PREFIX = "cooldown_";

    private final Plugin plugin;
    private final Map<String, NamespacedKey> keys = new HashMap<>();

    public PdcCooldownStore(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public long getExpiresAt(UUID playerId, String groupId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return 0;
        }
        Long expiresAt = player.getPersistentDataContainer().get(key(groupId), PersistentDataType.LONG);
        return expiresAt != null ? expiresAt : 0;
    }

    @Override
    public void setExpiresAt(UUID playerId, String groupId, long expiresAt) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.getPersistentDataContainer().set(key(groupId), PersistentDataType.LONG, expiresAt);
        }
    }

    @Override
    public void remove(UUID playerId, String groupId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.getPersistentDataContainer().remove(key(groupId));
        }
    }

    @Override
    public void removeAll(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            PersistentDataContainer data = player.getPersistentDataContainer();
            ownKeys(data).forEach(data::remove);
        }
    }

    /** Removes expired entries, including those of groups no longer in the config. */
    public void purgeExpired(Player player, long now) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        for (NamespacedKey key : ownKeys(data)) {
            Long expiresAt = data.get(key, PersistentDataType.LONG);
            if (expiresAt == null || expiresAt <= now) {
                data.remove(key);
            }
        }
    }

    private List<NamespacedKey> ownKeys(PersistentDataContainer data) {
        String namespace = plugin.getName().toLowerCase(Locale.ROOT);
        List<NamespacedKey> own = new ArrayList<>();
        for (NamespacedKey key : data.getKeys()) {
            if (key.getNamespace().equals(namespace) && key.getKey().startsWith(KEY_PREFIX)) {
                own.add(key);
            }
        }
        return own;
    }

    private NamespacedKey key(String groupId) {
        return keys.computeIfAbsent(groupId, id -> new NamespacedKey(plugin,
                KEY_PREFIX + id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_")));
    }
}
