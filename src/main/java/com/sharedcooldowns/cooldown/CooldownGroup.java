package com.sharedcooldowns.cooldown;

import java.util.List;
import java.util.Set;

/**
 * Represents a cooldown group configuration — either a shared group of
 * multiple commands or a single individual command.
 *
 * @param id              Unique identifier for this group (section key or command name).
 * @param commands        Set of normalized command names (lowercase, without leading '/').
 * @param cooldownSeconds Duration of the cooldown in seconds.
 * @param actions         List of action templates executed when cooldown is active.
 * @param conditional     If true, cooldown only applies after the action (e.g. teleport) completes.
 */
public record CooldownGroup(
    String id,
    Set<String> commands,
    int cooldownSeconds,
    List<String> actions,
    boolean conditional
) {}
