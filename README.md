# PhantomTurret 1.1.2

A standalone Bukkit plugin targeting **Minecraft Java 1.20 (1.20.0) through 26.3** on **CraftBukkit, Spigot, Paper, and compatible forks such as Purpur**.

Version 1.1.2 simplifies commands so they’re only used to reload the configuration. After reloading, the command sender will receive a confirmation message. The turret range can now be configured directly in config.yml, and the same range setting is used for target detection, projectile tracking, and the HUD. The default range is still 24 blocks, and all other default settings remain unchanged.

 Multiple projectile types, and opening the HUD by right clicking with any item in hand. It uses Java 17 bytecode and the original Spigot 1.20 API as its baseline. Please note that this repository contains source code only.

## Server compatibility

- Target range: **1.20 through 26.3**, the latest stable Java release checked on 2026-10-18. [Minecraft 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3), [Spigot 26.3 release](https://www.spigotmc.org/threads/spigot-bungeecord-26-3.724398/).
- Use the Java version required by your server. The plugin itself requires Java 17 or newer.
- CraftBukkit, Spigot, Paper, and Purpur use the standard Bukkit scheduler and APIs targeted by this build. Folia's region scheduler is not supported.
- This is a server plugin, not a Fabric/Forge/NeoForge mod or a vanilla server extension.
- Ammunition is available only when that item exists in the running server version. Newer projectiles are detected at runtime without loading their classes on older servers.
- Compatibility builds do not replace live gameplay testing across each server/version combination. Future releases and snapshots are not guaranteed.

## Usage

1. Place **one iron block**.
2. Place a **copper lightning rod upright on top**.
3. **Right click the base or lightning rod** to open the turret HUD. This works with any item in your main hand, including ammunition.
4. Click the **lightning rod icon** to open the ammo chest.
5. Put supported projectile items into its slots. Different types can share the chest.

The turret automatically targets and shoots visible phantoms within 24 blocks by default. If you’re an admin, you can change the range by editing turrets.radius in config.yml and running /pturret reload to apply the changes. Just make sure the turret has a clear line of sight to its target. Each shot uses one item from the first occupied slot, checking from left to right and then top to bottom. The projectiles will track the target, and the turret will keep firing until the phantom dies, moves out of range, or the ammunition runs out.

- Default interval: **30 ticks, or 1.5 seconds.**
- Default damage: **6 health points.**
- The first player to open the ammo chest or load the turret becomes the owner of its ammunition controls. Operators can manage other players' turrets without taking ownership.
- Deposit and withdraw ammunition using normal inventory clicks, shift clicks, hotbar swaps, and dragging. Item names, potion data, rocket designs, and other metadata are preserved when storing and returning items.
- The turret fires directly from the shared ammo inventory, even while it's open. Slot contents are saved when the chest closes, the chunk unloads, or the server shuts down normally.
- **Sneak right click** the base or rod while holding supported ammunition to quickly load it into the same storage. Normal right clicking always opens the HUD without placing or using the held item. The HUD's arrow button withdraws the first loaded stack while preserving its item type and metadata.
- Turret projectiles only damage phantoms, and fired arrows cannot be picked up. If a launch is cancelled or fails, the unused item is returned to the chest or dropped if the storage is no longer available.
- Breaking either part returns unused ammunition. Registered turret structures cannot be pushed by pistons.
- Turrets operate in loaded chunks, even while their owner is offline.
- Turret interactions do not produce chat messages, action bar messages, custom particles, or custom sounds. Projectiles are silent, although normal Minecraft visuals like tipped arrow particles and rocket trails may still appear. The menu displays ammunition and controls. The reload command sends a confirmation only to its sender.

## Supported ammunition

| Item | Turret behavior |
| --- | --- |
| Arrow | Standard arrow projectile. |
| Spectral arrow | Applies the native glowing effect on a successful hit. |
| Tipped arrow | Uses the loaded arrow's potion contents and custom effects. |
| Firework rocket | Flies toward the phantom and deals direct damage; no explosion or area damage. |
| Snowball | Deals direct damage to the phantom. |
| Egg, blue egg, brown egg | Egg shaped projectiles deal direct damage and never hatch chickens. Colored eggs require a server version that includes them. |
| Wind charge | Deals direct damage; no wind burst, nearby knockback, or block activation. Available on servers that include wind charges. |

Every type uses the configured `turrets.damage` value (6 health points by default), in addition to the native effects of spectral/tipped arrows when applicable. Minecraft's mob immunities still apply to potion effects. Unsupported items cannot be deposited. Shot lifetime is limited to five seconds.

## Commands and permissions

| Command | Action |
| --- | --- |
| `/phantomturret reload` or `/pturret reload` | Reload configuration and confirm to the sender; requires `phantomturret.admin`. |

`phantomturret.use` defaults to everyone. `phantomturret.admin` defaults to operators.

After a permitted reload finishes, the player or console that executed it receives `PhantomTurret configuration reloaded.` This is the only command action. Open the HUD by right clicking the turret.

## Configuration

Edit `plugins/PhantomTurret/config.yml`, then run `/pturret reload`:

```yaml
enabled: true
disabled-worlds: []
turrets:
  radius: 24
  cooldown-ticks: 30
  damage: 6.0
  #Applies to all supported projectile types
  arrow-speed: 1.6
  homing: true
```

`turrets.radius` is a spherical range in blocks, measured from the firing position. It accepts values from **1 to 64**, including decimals, and defaults to **24**. The existing setting name is retained, so an already customized radius continues to work. Invalid or non finite config values fall back to 24; numeric values outside the supported interval are clamped to 1 or 64.

For example, change `radius: 24` to `radius: 48`, save the YAML file, then run `/pturret reload` to use a 48 block range. Set it back to `24` to restore the default. This setting applies to every turret; it does not change an individual turret separately. Reopen an already open HUD to see the new range.

Turret locations, ownership, and ammunition are stored in `plugins/PhantomTurret/data.yml`.

The old `ammo-capacity` option is superseded by the full 27 slot chest. Existing saved ammunition counters are converted into slots automatically. Excess legacy ammunition is retained and fills available slots as arrows are used or withdrawn.

Existing 1.0.1 ammo inventories load directly, with their slot order preserved. No data reset is needed.

Versions 1.1.0 through 1.1.2 also retain the 1.0.2 data layout. Support for older server versions does not convert newer world or serialized item data for downgrades.

## Source and build

Use Java 17 or newer and Maven:

```text
mvn -Dmaven.test.skip=true package
```

Output: `target/phantom-turret-1.1.2.jar`. The server provides the Bukkit API at runtime; no libraries are bundled. JARs, class files, and `target/` are excluded from Git.

The default dependency is `org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT`. `spigot-api.version` can be overridden for a separate compatibility build, but release JARs are built against the oldest API.

## Source status

Version 1.1.2 compiled and packaged successfully against the Spigot **1.20** baseline on 2026-10-19 with Java 17 bytecode. Test compilation and execution were skipped. Live server gameplay testing has not been performed for this release.

Version 1.1.0 compiled and packaged successfully against Spigot **1.20, 1.21.11, and 26.3** on 2026-10-18, using Java 17 bytecode. The release JAR is built against the oldest API, 1.20. Test compilation and execution were skipped; no live server gameplay matrix has been run for this release. JARs remain excluded from this repository.

The older 1.0.0 JAR requires an empty hand to open the HUD. Updating the source does not update an installed server plugin: replace the old JAR with a built 1.1.2 JAR and restart the server to use the current command behavior, compatibility update, and all current features.
