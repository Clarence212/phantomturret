# PhantomTurret 1.0.1

A standalone plugin for **Paper 1.21.11 and Java 21**.

Version 1.0.1 adds a full **27-slot ammunition chest holding up to 1,728 regular arrows**. This repository contains source only. The 1.0.1 source has not been built or gameplay-tested yet.

## Install

1. Stop your server.
2. Build the source, then put `phantom-turret-1.0.1.jar` in the server's `plugins` folder. Replace any older PhantomTurret JAR and keep the existing data folder.
3. Start the server. Configuration and saved turret data appear in `plugins/PhantomTurret`.

No other plugin is required. AuthMe and SuperVanish are optional integrations. Standard Bukkit permissions work with LuckPerms. Vanilla items and inventory menus support Geyser/Floodgate interactions.

## Build and use a turret

1. Place **one iron block**.
2. Place a **copper lightning rod upright on top**.
3. Use the base or lightning rod with an **empty main hand** to open the turret HUD.
4. Click the **lightning-rod icon** to open the ammo chest.
5. Put ordinary arrows into its slots.

The turret automatically fires at visible phantoms within **24 blocks**. Keep its line of sight clear. Each shot uses one arrow. Arrows track the target, and the turret continues firing until the phantom dies, leaves range, or ammunition runs out.

- Capacity: **27 slots, up to 1,728 regular arrows**. All slots are available for arrows; the ammo chest has no filler icons or buttons.
- Default interval: 30 ticks, or 1.5 seconds.
- Default damage: 6 health points.
- The first player to open the ammo chest or load the turret owns its ammunition controls. Operators can administer other players' turrets without taking ownership.
- Deposit and withdraw arrows with normal inventory clicks, shift-clicks, hotbar swaps, and dragging. Only regular arrows without custom metadata are accepted.
- The turret fires directly from the shared ammo inventory, including while it is open. Slot contents are saved on closing, chunk unloading, and normal shutdown.
- Using arrows directly on the base or rod also reloads the same storage. The HUD's arrow button withdraws up to 64 arrows.
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
  homing: true
```

Turret locations, ownership, and ammunition are stored in `plugins/PhantomTurret/data.yml`.

The old `ammo-capacity` option is superseded by the full 27-slot chest. Existing saved ammunition counters are converted into slots automatically. Excess legacy ammunition is retained and fills available slots as arrows are used or withdrawn.

## Source and build

Use Java 21 and Maven:

```text
mvn -DskipTests package
```

Output: `target/phantom-turret-1.0.1.jar`. Paper provides the API at runtime; no libraries are bundled. JARs, class files, and `target/` are excluded from Git.

## Source status

The 1.0.1 ammo-chest update is prepared in source only. It has not been compiled or gameplay-tested. The earlier 1.0.0 JAR does not contain the new chest storage.
