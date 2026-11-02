package com.sharedcooldowns.command;

import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.config.ConfigManager;
import com.sharedcooldowns.cooldown.CooldownGroup;
import com.sharedcooldowns.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Handles the /sharedcooldowns command.
 *
 * Subcommands:
 * - check            — Shows your own active cooldowns (sharedcooldowns.check)
 * - check <player>   — Shows another player's active cooldowns (sharedcooldowns.admin)
 * - reset <player>   — Clears all of a player's cooldowns (sharedcooldowns.admin)
 * - reload           — Reloads config.yml (sharedcooldowns.admin)
 * - version          — Shows the plugin version (sharedcooldowns.admin)
 */
public class CooldownCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN = "sharedcooldowns.admin";
    private static final String PERM_CHECK = "sharedcooldowns.check";

    private final SharedCooldowns plugin;

    public CooldownCommand(SharedCooldowns plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String target = args.length > 1 ? args[1] : null;
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "check" -> handleCheck(sender, target);
            case "reset" -> handleReset(sender, target);
            case "reload" -> handleReload(sender);
            case "version", "ver" -> handleVersion(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean admin = sender.hasPermission(PERM_ADMIN);
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (admin || sender.hasPermission(PERM_CHECK)) {
                subcommands.add("check");
            }
            if (admin) {
                subcommands.addAll(List.of("reset", "reload", "version"));
            }
            return filter(subcommands, args[0]);
        }
        if (args.length == 2 && admin && List.of("check", "reset").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return filter(names, args[1]);
        }
        return List.of();
    }

    private void handleCheck(CommandSender sender, String targetName) {
        ConfigManager config = plugin.getConfigManager();
        if (targetName == null) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage(ColorUtil.colorize(config.getMessage("usage-check-others")));
            } else if (sender.hasPermission(PERM_CHECK) || sender.hasPermission(PERM_ADMIN)) {
                showCooldowns(sender, self);
            } else {
                sendPrefixed(sender, config.getMessage("no-permission"));
            }
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        boolean isSelf = target != null && target.equals(sender);
        if (!sender.hasPermission(PERM_ADMIN) && !(isSelf && sender.hasPermission(PERM_CHECK))) {
            sendPrefixed(sender, config.getMessage("no-permission"));
        } else if (target == null) {
            sendPrefixed(sender, config.getMessage("player-not-found").replace("%target%", targetName));
        } else {
            showCooldowns(sender, target);
        }
    }

    private void showCooldowns(CommandSender sender, Player target) {
        ConfigManager config = plugin.getConfigManager();
        List<String> lines = new ArrayList<>();
        for (CooldownGroup group : config.getGroups()) {
            long remaining = plugin.getCooldownRegistry().getRemainingSeconds(target.getUniqueId(), group);
            if (remaining > 0) {
                lines.add(config.getMessage("check-entry")
                    .replace("%group%", group.id())
                    .replace("%commands%", group.commands().stream().map(c -> "/" + c).collect(Collectors.joining(", ")))
                    .replace("%time_compact%", config.formatTimeCompact(remaining))
                    .replace("%time_single%", config.formatTimeSingle(remaining)));
            }
        }
        String key = lines.isEmpty() ? "check-empty" : "check-header";
        if (target.equals(sender)) {
            key += "-self";
        }
        sendPrefixed(sender, config.getMessage(key).replace("%target%", target.getName()));
        lines.forEach(line -> sender.sendMessage(ColorUtil.colorize(line)));
    }

    private void handleReset(CommandSender sender, String targetName) {
        ConfigManager config = plugin.getConfigManager();
        if (!sender.hasPermission(PERM_ADMIN)) {
            sendPrefixed(sender, config.getMessage("no-permission"));
            return;
        }
        if (targetName == null) {
            sender.sendMessage(ColorUtil.colorize(config.getMessage("usage-reset")));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sendPrefixed(sender, config.getMessage("player-not-found").replace("%target%", targetName));
            return;
        }
        int count = plugin.getCooldownRegistry().resetAll(target.getUniqueId(), config.getGroups());
        sendPrefixed(sender, config.getMessage("reset-done")
            .replace("%target%", target.getName())
            .replace("%count%", String.valueOf(count)));
    }

    private void handleReload(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();
        if (!sender.hasPermission(PERM_ADMIN)) {
            sendPrefixed(sender, config.getMessage("no-permission"));
            return;
        }
        config.reload();
        plugin.getCooldownRegistry().clearAllPending();
        sendPrefixed(sender, config.getMessage("reload-done"));
    }

    private void handleVersion(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sendPrefixed(sender, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        String version = plugin.getPluginMeta().getVersion();
        String apiVersion = plugin.getPluginMeta().getAPIVersion();
        sendPrefixed(sender, "&fVersion: &e" + version + " &7(API: " + (apiVersion != null ? apiVersion : "N/A") + ")");
    }

    private void sendUsage(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();
        boolean admin = sender.hasPermission(PERM_ADMIN);
        List<String> keys = new ArrayList<>();
        if (sender instanceof Player && (admin || sender.hasPermission(PERM_CHECK))) {
            keys.add("usage-check");
        }
        if (admin) {
            keys.addAll(List.of("usage-check-others", "usage-reset", "usage-reload", "usage-version"));
        }
        if (keys.isEmpty()) {
            sendPrefixed(sender, config.getMessage("no-permission"));
            return;
        }
        sendPrefixed(sender, config.getMessage("usage-header"));
        keys.forEach(key -> sender.sendMessage(ColorUtil.colorize(config.getMessage(key))));
    }

    private void sendPrefixed(CommandSender sender, String message) {
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getPrefix() + message));
    }

    private static List<String> filter(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
