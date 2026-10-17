# PhantomTurret 1.0.2

A standalone plugin for **Paper 1.21.11 and Java 21**.

Version 1.0.2 adds **multiple projectile types** to the shared 27-slot ammunition chest. This repository contains source only. The 1.0.2 source has not been built or gameplay-tested yet.

## Install

1. Stop your server.
2. Build the source, then put `phantom-turret-1.0.2.jar` in the server's `plugins` folder. Replace any older PhantomTurret JAR and keep the existing data folder.
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
| Egg, blue egg, brown egg | Deals direct damage; never hatches chickens. |
| Wind charge | Deals direct damage; no wind burst, nearby knockback, or block activation. |

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

## Source and build

Use Java 21 and Maven:

```text
mvn -DskipTests package
```

Output: `target/phantom-turret-1.0.2.jar`. Paper provides the API at runtime; no libraries are bundled. JARs, class files, and `target/` are excluded from Git.

## Source status

The 1.0.2 projectile update is prepared in source only. It has not been compiled or gameplay-tested. Earlier JARs do not contain these changes.
