# PhantomTurret 1.1.0

A standalone Bukkit plugin targeting **Minecraft Java 1.20 (1.20.0) through 26.3** on **CraftBukkit, Spigot, Paper, and compatible forks such as Purpur**.

Version 1.1.0 removes the Paper requirement while keeping the shared **27-slot ammunition chest**, multiple projectile types, and HUD access with any item in hand. The plugin uses Java 17 bytecode and the original Spigot 1.20 API as its baseline. This repository contains source only.

## Server compatibility

- Target range: **1.20 through 26.3**, the latest stable Java release checked on 2026-10-18. [Minecraft 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3), [Spigot 26.3 release](https://www.spigotmc.org/threads/spigot-bungeecord-26-3.724398/).
- Use the Java version required by your server. The plugin itself requires Java 17 or newer.
- CraftBukkit, Spigot, Paper, and Purpur use the standard Bukkit scheduler and APIs targeted by this build. Folia's region scheduler is not supported.
- This is a server plugin, not a Fabric/Forge/NeoForge mod or a vanilla-server extension.
- Ammunition is available only when that item exists in the running server version. Newer projectiles are detected at runtime without loading their classes on older servers.
- Compatibility builds do not replace live gameplay testing across each server/version combination. Future releases and snapshots are not guaranteed.

## Install

1. Stop your server.
2. Build the source, then put `phantom-turret-1.1.0.jar` in the server's `plugins` folder. Replace any older PhantomTurret JAR, keep only one copy, and keep the existing data folder.
3. Start the server. Configuration and saved turret data appear in `plugins/PhantomTurret`.

No other plugin is required. AuthMe and SuperVanish are optional integrations. Standard Bukkit permissions work with LuckPerms. Vanilla items and inventory menus support Geyser/Floodgate interactions.

## Build and use a turret

1. Place **one iron block**.
2. Place a **copper lightning rod upright on top**.
3. **Right-click the base or lightning rod** to open the turret HUD. This works with any item in your main hand, including ammunition.
4. Click the **lightning-rod icon** to open the ammo chest.
5. Put supported projectile items into its slots. Different types can share the chest.

The turret automatically fires at visible phantoms within **24 blocks**. Keep its line of sight clear. Each shot uses one item from the first occupied slot, scanning left to right and then top to bottom. Projectiles track the target, and the turret continues firing until the phantom dies, leaves range, or ammunition runs out.

- Capacity: **27 slots**, using each item's normal stack limit. Up to **1,728 arrows** or **432 snowballs/eggs**. The ammo chest has no filler icons or buttons.
- Default interval: 30 ticks, or 1.5 seconds.
- Default damage: 6 health points.
- The first player to open the ammo chest or load the turret owns its ammunition controls. Operators can administer other players' turrets without taking ownership.
- Deposit and withdraw ammunition with normal inventory clicks, shift-clicks, hotbar swaps, and dragging. Item names, potion data, rocket designs, and other metadata are preserved in storage and returns.
- The turret fires directly from the shared ammo inventory, including while it is open. Slot contents are saved on closing, chunk unloading, and normal shutdown.
- **Sneak-right-click** the base or rod while holding supported ammunition to quick-load the same storage. Normal right-click always opens the HUD, without placing or using the held item. The HUD's arrow button withdraws the first loaded stack, retaining its item type and metadata.
- Turret projectiles only damage phantoms. Fired arrows cannot be picked up. A cancelled or failed launch returns the unused item to the chest, or drops it if storage is no longer available.
- Breaking either part returns unused ammunition. Registered structures cannot be pushed by pistons.
- Operates in loaded chunks, including while its owner is offline.
- Adds no chat, action-bar messages, custom particles, or custom sounds. Projectiles are silent; their normal Minecraft visuals, such as tipped-arrow particles and rocket trails, may still appear. The menu displays ammunition and controls.

## Supported ammunition

| Item | Turret behavior |
| --- | --- |
| Arrow | Standard arrow projectile. |
| Spectral arrow | Applies the native glowing effect on a successful hit. |
| Tipped arrow | Uses the loaded arrow's potion contents and custom effects. |
| Firework rocket | Flies toward the phantom and deals direct damage; no explosion or area damage. |
| Snowball | Deals direct damage to the phantom. |
| Egg, blue egg, brown egg | Egg-shaped projectiles deal direct damage and never hatch chickens. Colored eggs require a server version that includes them. |
| Wind charge | Deals direct damage; no wind burst, nearby knockback, or block activation. Available on servers that include wind charges. |

Every type uses the configured `turrets.damage` value (6 health points by default), in addition to the native effects of spectral/tipped arrows when applicable. Minecraft's mob immunities still apply to potion effects. Unsupported items cannot be deposited. Shot lifetime is limited to five seconds.

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
  # Applies to all supported projectile types; the existing setting name is retained.
  arrow-speed: 1.6
  homing: true
```

Turret locations, ownership, and ammunition are stored in `plugins/PhantomTurret/data.yml`.

The old `ammo-capacity` option is superseded by the full 27-slot chest. Existing saved ammunition counters are converted into slots automatically. Excess legacy ammunition is retained and fills available slots as arrows are used or withdrawn.

Existing 1.0.1 ammo inventories load directly, with their slot order preserved. No data reset is needed.

Version 1.1.0 also retains the 1.0.2 data layout. Support for older server versions does not convert newer world or serialized item data for downgrades.

## Source and build

Use Java 17 or newer and Maven:

```text
mvn -Dmaven.test.skip=true package
```

Output: `target/phantom-turret-1.1.0.jar`. The server provides the Bukkit API at runtime; no libraries are bundled. JARs, class files, and `target/` are excluded from Git.

The default dependency is `org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT`. `spigot-api.version` can be overridden for a separate compatibility build, but release JARs are built against the oldest API.

## Source status

Version 1.1.0 compiled and packaged successfully against Spigot **1.20, 1.21.11, and 26.3** on 2026-10-18, using Java 17 bytecode. The release JAR is built against the oldest API, 1.20. Test compilation and execution were skipped; no live-server gameplay matrix has been run for this release. JARs remain excluded from this repository.

The older 1.0.0 JAR requires an empty hand to open the HUD. Updating the source does not update an installed server plugin: replace the old JAR with a built 1.1.0 JAR and restart the server to use the compatibility update and all current features.
