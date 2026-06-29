# SharedCooldowns

> Group commands together and enforce a shared cooldown across all of them.

A lightweight Minecraft plugin that lets you define groups of commands sharing a single cooldown — with conditional triggering, custom actions, and full formatting control.

---

## Features

- Group multiple commands under a single shared cooldown
- Conditional cooldowns — only applied if the action completes successfully
- Custom actions on cooldown (actionbar, title, sound, message)
- Fully configurable time formats and messages
- Lightweight and performant

---

## Installation

1. Download the latest release from [Releases](../../releases)
2. Place the `.jar` in your server's `plugins/` folder
3. Restart the server
4. Edit `plugins/SharedCooldowns/config.yml` to your liking

**Requirements:** Paper/Spigot 26.2+, Java 26+

---

## Commands & Permissions

| Permission | Description |
|---|---|
| `sharedcooldowns.admin` | Allows you to use `/scd reload` and `/scd version`  |
| `sharedcooldowns.bypass` | Allows you to bypass any cooldown |

| Command | Description  |
|---|---|
| `/scd reload` | Reload the configuration | 
| `/scd version` | Check the plugin version | 

**Aliases:** `/sharedcooldowns`, `/scd`

---

## Configuration

```yaml
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
    conditional: true    # Only apply cooldown if the action completes
    actions:
      - "[sound] block.note_block.pling 0.5 0.5"
      - "[actionbar] &eYou must wait %time_single% &ebefore teleporting again"
```

### Actions

| Tag | Format | Description |
|---|---|---|
| `[message]` | `[message] <text>` | Sends a chat message (default) |
| `[actionbar]` | `[actionbar] <text>` | Sends an action bar message |
| `[title]` | `[title] <title>\|<subtitle>` | Sends a title |
| `[sound]` | `[sound] <sound> <volume> <pitch>` | Plays a sound |

### Placeholders

| Placeholder | Example | Description |
|---|---|---|
| `%time_compact%` | `9d 5h 15m 30s` | Remaining time, all units |
| `%time_single%` | `9 days` | Remaining time, largest unit only |
| `%player%` | `Steve` | Player's username |
| `%command%` | `/spawn` | The command executed |

---

## License

This project is licensed under the [MIT License](LICENSE).