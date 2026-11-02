<p align="center">
  <img src="assets/icon512.png" alt="SharedCooldowns icon" width="128">
</p>

# SharedCooldowns

> Group commands together and enforce a shared cooldown across all of them.

A lightweight Minecraft plugin that lets you define groups of commands sharing a single cooldown — with conditional triggering, custom actions, and full formatting control.

---

## Features

- Group multiple commands under a single shared cooldown
- Conditional cooldowns — the cooldown starts only when the player actually teleports
- Cooldowns persist across restarts and `/scd reload`
- Custom actions on cooldown (actionbar, title, sound, message)
- Fully configurable time formats and messages
- Lightweight and performant

---

## Installation

1. Download the latest release from [Releases](../../releases)
2. Place the `.jar` in your server's `plugins/` folder
3. Restart the server
4. Edit `plugins/SharedCooldowns/config.yml` to your liking

**Requirements:** Paper 26.3+, Java 25+

---

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/scd check` | `sharedcooldowns.check` (everyone) | Shows your active cooldowns |
| `/scd check <player>` | `sharedcooldowns.admin` | Shows an online player's active cooldowns |
| `/scd reset <player>` | `sharedcooldowns.admin` | Clears all of an online player's cooldowns |
| `/scd reload` | `sharedcooldowns.admin` | Reloads the configuration |
| `/scd version` | `sharedcooldowns.admin` | Shows the plugin version |

`sharedcooldowns.bypass` skips all cooldowns.

**Aliases:** `/sharedcooldowns`, `/scd`

---

## Configuration

```yaml
pending-timeout: 120   # Seconds a conditional command waits for its teleport
teleport-causes:       # Causes that count; ender pearls, portals, etc. are ignored
  - COMMAND
  - PLUGIN

time:
  compact: "&b%days%&7d &b%hours%&7h &b%minutes%&7m &b%seconds%&7s"
  single: "&b%value% %unit%"
  units:
    day: "day"
    days: "days"
    hour: "hour"
    hours: "hours"
    minute: "minute"
    minutes: "minutes"
    second: "second"
    seconds: "seconds"

cooldowns:
  teleports:
    commands:
      - /spawn
      - /home
      - /tpa
    cooldown: 900        # Duration in seconds
    conditional: true    # Start the cooldown when the player teleports
    actions:
      - "[sound] block.note_block.pling 0.5 0.5"
      - "[actionbar] &eYou must wait %time_single% &ebefore teleporting again"
```

### Conditional cooldowns

A conditional command waits up to `pending-timeout` seconds for a teleport. The first teleport starts the cooldown. Any other command still waiting, such as a `/tpa` accepted later, has its teleport blocked while the cooldown runs.

- Set `pending-timeout` above your slowest teleport (with CMI: `/tpa` accept time + warmup).
- A failed command, such as `/home` with a wrong name, keeps waiting until the timeout. A teleport from another command with a counted cause can consume it, so it may start the cooldown or be blocked once.
- If another plugin cancels the command, no cooldown starts. This includes CMI custom aliases that run several commands: list the commands they run instead.

All messages of the `/scd` command are editable under `messages:` in `config.yml`.

### Actions

| Tag | Format | Description |
|---|---|---|
| `[message]` | `[message] <text>` | Sends a chat message (default) |
| `[actionbar]` | `[actionbar] <text>` | Sends an action bar message |
| `[title]` | `[title] <title>;<subtitle>` | Sends a title |
| `[sound]` | `[sound] <sound> <volume> <pitch>` | Plays a sound |

### Placeholders

| Placeholder | Example | Description |
|---|---|---|
| `%time_compact%` | `9d 5h 15m 30s` | Remaining time, all units |
| `%time_single%` | `9 days` | Remaining time, largest unit only |
| `%player%` | `Steve` | Player's username |
| `%command%` | `/spawn` | The command executed |

---

## Building

Run `./gradlew build`. The jar is written to `build/libs/`. Requires JDK 25.

---

## License

This project is licensed under the [MIT License](LICENSE).