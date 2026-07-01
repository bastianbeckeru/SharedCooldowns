package com.sharedcooldowns.command;

import com.sharedcooldowns.SharedCooldowns;
import com.sharedcooldowns.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the /sharedcooldowns admin command.
 *
 * Subcommands:
 * - /sharedcooldowns reload  — Reloads config.yml
 * - /sharedcooldowns version — Shows plugin version
 */
public class CooldownCommand implements CommandExecutor, TabCompleter {

    private final SharedCooldowns plugin;

    public CooldownCommand(SharedCooldowns plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sharedcooldowns.admin")) {
            sender.sendMessage(ColorUtil.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "version", "ver" -> handleVersion(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sharedcooldowns.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> subcommands = List.of("reload", "version");
            String prefix = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String sub : subcommands) {
                if (sub.startsWith(prefix)) {
                    matches.add(sub);
                }
            }
            return matches;
        }

        return List.of();
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        plugin.getCooldownRegistry().clearAll();
        sender.sendMessage(ColorUtil.colorize("&8[&b&lSharedCooldowns&8]&r &fConfiguration reloaded successfully."));
    }

    private void handleVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String apiVersion = plugin.getDescription().getAPIVersion();
        if (apiVersion == null) {
            apiVersion = "N/A";
        }
        sender.sendMessage(ColorUtil.colorize("&8[&b&lSharedCooldowns&8]&r &fVersion: &e" + version + " &7(API: " + apiVersion + ")"));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&8[&b&lSharedCooldowns&8]&r &fUsage:"));
        sender.sendMessage(ColorUtil.colorize("  &e/sharedcooldowns reload &8- &7Reloads the configuration"));
        sender.sendMessage(ColorUtil.colorize("  &e/sharedcooldowns version &8- &7Shows the plugin version"));
    }
}
