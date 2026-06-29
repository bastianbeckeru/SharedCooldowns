package com.sharedcooldowns.command

import com.sharedcooldowns.SharedCooldowns
import com.sharedcooldowns.util.ColorUtil.colorize
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * Handles the /sharedcooldowns admin command.
 *
 * Subcommands:
 * - /sharedcooldowns reload  — Reloads config.yml
 * - /sharedcooldowns version — Shows plugin version
 */
class CooldownCommand(private val plugin: SharedCooldowns) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("sharedcooldowns.admin")) {
            sender.sendMessage("&cYou don't have permission to use this command.".colorize())
            return true
        }

        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "version", "ver" -> handleVersion(sender)
            else -> sendUsage(sender)
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("sharedcooldowns.admin")) return emptyList()

        if (args.size == 1) {
            val subcommands = listOf("reload", "version")
            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }

        return emptyList()
    }

    private fun handleReload(sender: CommandSender) {
        plugin.configManager.reload()
        plugin.cooldownRegistry.clearAll()
        sender.sendMessage("&8[&b&lSharedCooldowns&8]&r &fConfiguration reloaded successfully.".colorize())
    }

    private fun handleVersion(sender: CommandSender) {
        val version = plugin.description.version
        val apiVersion = plugin.description.apiVersion ?: "N/A"
        sender.sendMessage("&8[&b&lSharedCooldowns&8]&r &fVersion: &e$version &7(API: $apiVersion)".colorize())
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("&8[&b&lSharedCooldowns&8]&r &fUsage:".colorize())
        sender.sendMessage("  &e/sharedcooldowns reload &8- &7Reloads the configuration".colorize())
        sender.sendMessage("  &e/sharedcooldowns version &8- &7Shows the plugin version".colorize())
    }
}
