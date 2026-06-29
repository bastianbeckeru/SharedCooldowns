package com.sharedcooldowns.cooldown

/**
 * Represents a cooldown group configuration — either a shared group of
 * multiple commands or a single individual command.
 *
 * @property id Unique identifier for this group (section key or command name).
 * @property commands Set of normalized command names (lowercase, without leading '/').
 * @property cooldownSeconds Duration of the cooldown in seconds.
 * @property actions List of action templates executed when cooldown is active.
 * @property conditional If true, cooldown only applies after the action (e.g. teleport) completes.
 */
data class CooldownGroup(
    val id: String,
    val commands: Set<String>,
    val cooldownSeconds: Int,
    val actions: List<String>,
    val conditional: Boolean
)
