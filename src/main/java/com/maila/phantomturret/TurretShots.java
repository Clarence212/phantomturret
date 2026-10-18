package com.maila.phantomturret;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/** Native projectile flight, with impacts restricted to phantoms. */
final class TurretShots implements Listener {
    private final PhantomTurretPlugin plugin;
    private final Map<UUID, Shot> shots = new HashMap<>();
    private final Set<UUID> directDamage = new HashSet<>();
    private record Shot(UUID target, Location origin, long expiry) {}

    TurretShots(PhantomTurretPlugin plugin) { this.plugin = plugin; }

    private boolean tagged(Entity entity) {
        return entity.getPersistentDataContainer().has(plugin.key("turret_projectile"), PersistentDataType.BYTE)
            || entity.getPersistentDataContainer().has(plugin.key("turret_arrow"), PersistentDataType.BYTE);
    }

    private double speed() { return plugin.decimal("turrets.arrow-speed", 1.6, .5, 3); }
    private double damage() { return plugin.decimal("turrets.damage", 6, .1, 100); }

    boolean fire(Location muzzle, Phantom target, ItemStack ammunition) {
        if (!AmmoTypes.supported(ammunition)) return false;
        Vector direction = target.getBoundingBox().getCenter().subtract(muzzle.toVector());
        if (direction.lengthSquared() < .001) return false;
        Vector velocity = direction.normalize().multiply(speed());
        Projectile[] created = new Projectile[1];
        try {
            Projectile projectile = Compatibility.spawn(muzzle, AmmoTypes.projectile(ammunition.getType()), entity -> {
                created[0] = entity;
                // Configure and tag before ProjectileLaunchEvent so other plugins can inspect the shot.
                entity.getPersistentDataContainer().set(plugin.key("turret_projectile"), PersistentDataType.BYTE, (byte) 1);
                entity.setSilent(true);
                entity.setGravity(!plugin.getConfig().getBoolean("turrets.homing", true));
                ItemStack item = ammunition.clone(); item.setAmount(1);
                if (entity instanceof AbstractArrow arrow) {
                    Compatibility.arrowItem(arrow, item);
                    arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    arrow.setCritical(false); arrow.setPierceLevel(0); arrow.setDamage(damage());
                } else if (entity instanceof Firework firework) {
                    firework.setFireworkMeta((FireworkMeta) item.getItemMeta()); firework.setShotAtAngle(true);
                    firework.setLife(0); firework.setMaxLife(200);
                } else if (entity instanceof ThrowableProjectile throwable) {
                    throwable.setItem(item);
                } else if (entity instanceof Fireball wind) {
                    Compatibility.stopAcceleration(wind); wind.setIsIncendiary(false); wind.setYield(0);
                }
                entity.setVelocity(velocity);
            });
            if (!projectile.isValid()) { projectile.remove(); return false; }
            shots.put(projectile.getUniqueId(), new Shot(target.getUniqueId(), muzzle.clone(), plugin.currentTick() + 100L));
            return true;
        } catch (RuntimeException exception) {
            if (created[0] != null) created[0].remove();
            plugin.getLogger().warning("Could not launch turret ammunition " + ammunition.getType() + ": " + exception.getMessage());
            return false;
        }
    }

    private void remove(Projectile projectile) {
        shots.remove(projectile.getUniqueId()); projectile.remove();
    }

    void tick() {
        long tick = plugin.currentTick();
        for (UUID id : new ArrayList<>(shots.keySet())) {
            Shot shot = shots.get(id);
            if (!(Bukkit.getEntity(id) instanceof Projectile projectile)) { shots.remove(id); continue; }
            if (!projectile.isValid() || tick >= shot.expiry()
                || projectile instanceof AbstractArrow arrow && arrow.isInBlock()
                || !(Bukkit.getEntity(shot.target()) instanceof Phantom phantom)
                || !phantom.isValid() || !phantom.getWorld().equals(projectile.getWorld()) || !plugin.enabled()) {
                remove(projectile); continue;
            }
            double radius = plugin.decimal("turrets.radius", 24, 1, 64);
            if (phantom.getLocation().distanceSquared(shot.origin()) > radius * radius) { remove(projectile); continue; }
            if (plugin.getConfig().getBoolean("turrets.homing", true)) {
                Vector toward = phantom.getBoundingBox().getCenter().subtract(projectile.getLocation().toVector());
                if (toward.lengthSquared() > .001) projectile.setVelocity(toward.normalize().multiply(speed()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void damage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile) || !tagged(projectile)) return;
        UUID id = projectile.getUniqueId();
        if (!plugin.enabled() || !(event.getEntity() instanceof Phantom)
            || !(shots.containsKey(id) || directDamage.contains(id))) {
            event.setCancelled(true); remove(projectile);
        } else if (!(projectile instanceof AbstractArrow) && !directDamage.contains(id)) {
            // Rockets and wind charges use only our single-target damage, never native area damage.
            event.setCancelled(true);
        } else event.setDamage(damage());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void hit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!tagged(projectile)) return;
        boolean allowed = !event.isCancelled() && plugin.enabled() && shots.containsKey(projectile.getUniqueId())
            && event.getHitEntity() instanceof Phantom phantom && phantom.isValid();
        if (projectile instanceof AbstractArrow) {
            if (!allowed) { event.setCancelled(true); remove(projectile); }
            else plugin.later(() -> remove(projectile)); // Allow native tipped/spectral effects on this phantom.
            return;
        }
        // Cancel native egg hatching and wind/rocket impact behavior, including block activation.
        event.setCancelled(true);
        try {
            if (allowed) {
                directDamage.add(projectile.getUniqueId());
                ((Phantom) event.getHitEntity()).damage(damage(), projectile);
            }
        } finally {
            directDamage.remove(projectile.getUniqueId()); remove(projectile);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void firework(FireworkExplodeEvent event) {
        if (tagged(event.getEntity())) { event.setCancelled(true); remove(event.getEntity()); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void prime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof Projectile projectile && tagged(projectile)) {
            event.setCancelled(true); remove(projectile);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void explode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Projectile projectile && tagged(projectile)) {
            event.setCancelled(true); event.blockList().clear(); remove(projectile);
        }
    }

    @EventHandler
    public void chunkLoaded(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities())
            if (entity instanceof Projectile projectile && tagged(projectile) && !shots.containsKey(entity.getUniqueId()))
                projectile.remove();
    }

    void removeLoadedOrphans() {
        // Some spawn chunks are already loaded before plugins enable after a restart.
        for (var world : Bukkit.getWorlds())
            for (Projectile projectile : world.getEntitiesByClass(Projectile.class))
                if (tagged(projectile) && !shots.containsKey(projectile.getUniqueId())) projectile.remove();
    }

    void close() {
        for (UUID id : new ArrayList<>(shots.keySet()))
            if (Bukkit.getEntity(id) instanceof Projectile projectile) projectile.remove();
        shots.clear(); directDamage.clear();
    }
}
