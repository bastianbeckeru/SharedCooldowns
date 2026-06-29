package com.sharedcooldowns

import com.sharedcooldowns.command.CooldownCommand
import com.sharedcooldowns.config.ConfigManager
import com.sharedcooldowns.cooldown.CooldownRegistry
import com.sharedcooldowns.listener.CmiTeleportListener
import com.sharedcooldowns.listener.CommandListener
import com.sharedcooldowns.listener.TeleportListener
import org.bukkit.plugin.java.JavaPlugin

/**
 * SharedCooldowns — PaperMC plugin for managing command cooldowns.
 *
 * Features:
 * - Individual and shared (group) command cooldowns
 * - Conditional cooldowns that only activate when actions complete (e.g. teleports)
 * - CMI integration for accurate teleport detection
 * - Configurable messages with color code support
 * - Admin commands for reload and version info
 */
class SharedCooldowns : JavaPlugin() {

    lateinit var configManager: ConfigManager
        private set

    lateinit var cooldownRegistry: CooldownRegistry
        private set

    override fun onEnable() {
        // Initialize core systems
        cooldownRegistry = CooldownRegistry()
        configManager = ConfigManager(this)

        // Register event listeners
        val pluginManager = server.pluginManager
        pluginManager.registerEvents(CommandListener(this), this)
        pluginManager.registerEvents(TeleportListener(this), this)

        // Register CMI-specific listener if CMI is present
        if (server.pluginManager.getPlugin("CMI") != null) {
            try {
                pluginManager.registerEvents(CmiTeleportListener(this), this)
                logger.info("CMI detected — CMI teleport listener registered.")
            } catch (e: Exception) {
                logger.warning("CMI detected but could not register CMI listener: ${e.message}")
                logger.warning("Falling back to generic Paper listener.")
            }
        } else {
            logger.info("CMI not detected — using generic teleport listener.")
        }

        // Register admin command
        val scCommand = getCommand("sharedcooldowns")
        val commandHandler = CooldownCommand(this)
        scCommand?.setExecutor(commandHandler)
        scCommand?.tabCompleter = commandHandler

        logger.info("SharedCooldowns v${description.version} enabled successfully.")
    }

    override fun onDisable() {
        cooldownRegistry.clearAll()
        logger.info("SharedCooldowns disabled.")
    }
}
