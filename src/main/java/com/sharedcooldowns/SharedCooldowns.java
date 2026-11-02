package com.sharedcooldowns;

import com.sharedcooldowns.command.CooldownCommand;
import com.sharedcooldowns.config.ConfigManager;
import com.sharedcooldowns.cooldown.CooldownRegistry;
import com.sharedcooldowns.cooldown.PdcCooldownStore;
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
 * - Cooldowns persist across restarts
 * - Configurable messages with color code support
 * - Admin commands for reload and version info
 */
public class SharedCooldowns extends JavaPlugin {

    private ConfigManager configManager;
    private CooldownRegistry cooldownRegistry;
    private PdcCooldownStore cooldownStore;

    @Override
    public void onEnable() {
        // Initialize core systems
        cooldownStore = new PdcCooldownStore(this);
        cooldownRegistry = new CooldownRegistry(cooldownStore, System::currentTimeMillis);
        configManager = new ConfigManager(this);

        // Register event listeners
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CommandListener(this), this);
        pluginManager.registerEvents(new TeleportListener(this), this);

        // Register admin command
        PluginCommand scCommand = getCommand("sharedcooldowns");
        if (scCommand != null) {
            CooldownCommand commandHandler = new CooldownCommand(this);
            scCommand.setExecutor(commandHandler);
            scCommand.setTabCompleter(commandHandler);
        }

        getLogger().info("SharedCooldowns v" + getPluginMeta().getVersion() + " enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (cooldownRegistry != null) {
            cooldownRegistry.clearAllPending();
        }
        getLogger().info("SharedCooldowns disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PdcCooldownStore getCooldownStore() {
        return cooldownStore;
    }

    public CooldownRegistry getCooldownRegistry() {
        return cooldownRegistry;
    }
}
