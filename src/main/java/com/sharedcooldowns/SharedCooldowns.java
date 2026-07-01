package com.sharedcooldowns;

import com.sharedcooldowns.command.CooldownCommand;
import com.sharedcooldowns.config.ConfigManager;
import com.sharedcooldowns.cooldown.CooldownRegistry;
import com.sharedcooldowns.listener.CmiTeleportListener;
import com.sharedcooldowns.listener.CommandListener;
import com.sharedcooldowns.listener.TeleportListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

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
public class SharedCooldowns extends JavaPlugin {

    private ConfigManager configManager;
    private CooldownRegistry cooldownRegistry;

    @Override
    public void onEnable() {
        // Initialize core systems
        cooldownRegistry = new CooldownRegistry();
        configManager = new ConfigManager(this);

        // Register event listeners
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CommandListener(this), this);
        pluginManager.registerEvents(new TeleportListener(this), this);

        // Register CMI-specific listener if CMI is present
        if (pluginManager.getPlugin("CMI") != null) {
            try {
                pluginManager.registerEvents(new CmiTeleportListener(this), this);
                getLogger().info("CMI detected — CMI teleport listener registered.");
            } catch (Exception e) {
                getLogger().warning("CMI detected but could not register CMI listener: " + e.getMessage());
                getLogger().warning("Falling back to generic Paper listener.");
            }
        } else {
            getLogger().info("CMI not detected — using generic teleport listener.");
        }

        // Register admin command
        PluginCommand scCommand = getCommand("sharedcooldowns");
        if (scCommand != null) {
            CooldownCommand commandHandler = new CooldownCommand(this);
            scCommand.setExecutor(commandHandler);
            scCommand.setTabCompleter(commandHandler);
        }

        getLogger().info("SharedCooldowns v" + getDescription().getVersion() + " enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (cooldownRegistry != null) {
            cooldownRegistry.clearAll();
        }
        getLogger().info("SharedCooldowns disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CooldownRegistry getCooldownRegistry() {
        return cooldownRegistry;
    }
}
