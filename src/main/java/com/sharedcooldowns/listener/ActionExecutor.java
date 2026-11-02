package com.sharedcooldowns.listener;

import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.config.ConfigManager;
import com.sharedcooldowns.util.ColorUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Pattern;

/** Runs a group's configured actions for a player who is on cooldown. */
final class ActionExecutor {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final String ACTIONBAR = "[actionbar]";
    private static final String TITLE = "[title]";
    private static final String SOUND = "[sound]";
    private static final String MESSAGE = "[message]";

    private final SharedCooldowns plugin;

    ActionExecutor(SharedCooldowns plugin) {
        this.plugin = plugin;
    }

    void execute(Player player, List<String> actions, long remaining, String command) {
        ConfigManager config = plugin.getConfigManager();
        for (String action : actions) {
            String replaced = action
                .replace("%time_compact%", config.formatTimeCompact(remaining))
                .replace("%time_single%", config.formatTimeSingle(remaining))
                .replace("%player%", player.getName())
                .replace("%command%", "/" + command);

            if (hasTag(replaced, ACTIONBAR)) {
                player.sendActionBar(ColorUtil.colorize(content(replaced, ACTIONBAR)));
            } else if (hasTag(replaced, TITLE)) {
                String[] parts = content(replaced, TITLE).split(";", 2);
                player.showTitle(Title.title(
                    ColorUtil.colorize(parts[0].trim()),
                    ColorUtil.colorize(parts.length > 1 ? parts[1].trim() : "")
                ));
            } else if (hasTag(replaced, SOUND)) {
                playSound(player, content(replaced, SOUND));
            } else {
                String text = hasTag(replaced, MESSAGE) ? content(replaced, MESSAGE) : replaced;
                player.sendMessage(ColorUtil.colorize(config.getPrefix() + text));
            }
        }
    }

    private static boolean hasTag(String action, String tag) {
        return action.regionMatches(true, 0, tag, 0, tag.length());
    }

    private static String content(String action, String tag) {
        return action.substring(tag.length()).trim();
    }

    private void playSound(Player player, String content) {
        String[] params = WHITESPACE_PATTERN.split(content);
        if (params[0].isEmpty()) {
            return;
        }
        float volume = params.length > 1 ? parseFloat(params[1]) : 1.0f;
        float pitch = params.length > 2 ? parseFloat(params[2]) : 1.0f;
        try {
            player.playSound(player.getLocation(), params[0], volume, pitch);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Could not play sound '" + params[0] + "': " + e.getMessage());
        }
    }

    private static float parseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }
}
