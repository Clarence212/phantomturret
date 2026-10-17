# PhantomTurret 1.0.0

A standalone plugin for **Paper 1.21.11 and Java 21**.

## Install

1. Stop your server.
2. Put `phantom-turret-1.0.0.jar` in the server's `plugins` folder.
3. Start the server. Configuration and saved turret data appear in `plugins/PhantomTurret`.

No other plugin is required. AuthMe and SuperVanish are optional integrations. Standard Bukkit permissions work with LuckPerms. Vanilla items and inventory menus support Geyser/Floodgate interactions.

### Running alongside Xtend

If you keep Xtend installed, set `features.turrets: false` in `plugins/Xtend/config.yml`, then run `/xtend reload` or restart the server. PhantomTurret waits while Xtend's turret feature is enabled, so two plugins cannot operate the same structure at once.

If existing Xtend turrets contain arrows, withdraw them through Xtend before disabling that feature. Load them again through PhantomTurret afterwards. This plugin uses its own saved data and does not import Xtend ownership or ammunition.

## Build and use a turret

1. Place **one iron block**.
2. Place a **copper lightning rod upright on top**.
3. Hold **ordinary arrows** and right-click the iron block to load it.

The turret automatically fires at visible phantoms within **24 blocks**. Keep its line of sight clear. Each shot uses one arrow. Arrows track the target, and the turret continues firing until the phantom dies, leaves range, or ammunition runs out.

- Capacity: 256 arrows.
- Default interval: 30 ticks, or 1.5 seconds.
- Default damage: 6 health points.
- The first player to load it owns its ammunition controls. Operators can administer other players' turrets.
- Use the base with an empty hand to open the ammunition menu. Click its arrow icon to withdraw up to 64 arrows.
- Turret arrows cannot damage players or other mobs, and cannot be picked up.
- Breaking either part returns unused ammunition. Registered structures cannot be pushed by pistons.
- Operates in loaded chunks, including while its owner is offline.
- Adds no chat, action-bar messages, particles, or custom sounds. The menu displays ammunition and controls.

## Commands and permissions

| Command | Action |
| --- | --- |
| `/phantomturret` or `/pturret` | Open the turret you are looking at within 5 blocks. |
| `/phantomturret reload` | Reload configuration; requires `phantomturret.admin`. |

`phantomturret.use` defaults to everyone. `phantomturret.admin` defaults to operators.

Commands intentionally produce no chat responses. Player actions require AuthMe authentication when AuthMe is installed, and are blocked while vanished. Cancelled interactions are respected.

## Configuration

Edit `plugins/PhantomTurret/config.yml`, then reload:

```yaml
enabled: true
disabled-worlds: []
turrets:
  radius: 24
  cooldown-ticks: 30
  damage: 6.0
  arrow-speed: 1.6
  ammo-capacity: 256
  homing: true
```

Turret locations, ownership, and ammunition are stored in `plugins/PhantomTurret/data.yml`.

## Source and build

Use Java 21 and Maven:

```text
mvn -DskipTests package
```

Output: `target/phantom-turret-1.0.0.jar`. Paper provides the API at runtime; no libraries are bundled.

This release extracts Xtend's turret feature into its own plugin and namespace. The standalone release was compiled against Paper 1.21.11; separate gameplay tests were not run for this extraction.
