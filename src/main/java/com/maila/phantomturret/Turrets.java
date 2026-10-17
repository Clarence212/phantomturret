package com.maila.phantomturret;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.LightningRod;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

final class Turrets implements Listener {
    private final PhantomTurretPlugin plugin;
    private final Map<String, Long> nextShot = new HashMap<>();
    private final Map<UUID, Shot> shots = new HashMap<>();
    private record Shot(UUID target, Location origin, long expiry) {}
    Turrets(PhantomTurretPlugin plugin) { this.plugin = plugin; }
    private Block base(Block clicked) {
        return clicked.getBlockData() instanceof LightningRod ? clicked.getRelative(BlockFace.DOWN) : clicked;
    }
    private boolean structure(Block b) {
        return b.getType() == Material.IRON_BLOCK && b.getRelative(BlockFace.UP).getBlockData() instanceof LightningRod rod
            && rod.getFacing() == BlockFace.UP;
    }
    private boolean owned(Player p, String id) {
        String owner = plugin.store.data.getString("turrets." + id + ".owner");
        return owner == null || owner.equals(p.getUniqueId().toString()) || p.hasPermission("phantomturret.admin");
    }
    boolean canManage(Player p, Block b) {
        return plugin.allowed(p) && p.getWorld().equals(b.getWorld()) && Store.loaded(b.getLocation())
            && p.getLocation().distanceSquared(b.getLocation()) <= 36 && structure(b) && owned(p, Store.id(b));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interact(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !e.getAction().isRightClick() || e.getClickedBlock() == null) return;
        Player p = e.getPlayer(); Block b = base(e.getClickedBlock());
        if (!plugin.allowed(p) || !structure(b)) return;
        String id = Store.id(b); if (!owned(p, id)) return;
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held.getType() == Material.ARROW && Items.plain(held)) {
            e.setCancelled(true);
            plugin.ammunition.register(p, b);
            int take = plugin.ammunition.add(b, held.getAmount());
            if (take <= 0) return;
            plugin.store.save();
            held.setAmount(held.getAmount() - take);
        } else if (Items.empty(held)) { e.setCancelled(true); open(p, b); }
    }
    void openTargeted(Player p) { Block b = p.getTargetBlockExact(5); if (b != null) open(p, base(b)); }
    private void open(Player p, Block b) {
        if (!plugin.allowed(p) || !structure(b)) return;
        String id = Store.id(b); if (!owned(p, id)) return;
        TurretMenu.Holder h = new TurretMenu.Holder(); h.inventory = Bukkit.createInventory(h, 27, Component.text("Phantom Turret"));
        int ammo = plugin.ammunition.count(b);
        h.inventory.setItem(11, Items.named(Material.LIGHTNING_ROD, "Phantom Turret", "Ammunition: " + ammo,
            "Range: " + plugin.decimal("turrets.radius", 24, 1, 64) + " blocks", "Click to open 27 ammunition slots"));
        h.actions.put(11, () -> plugin.ammunition.open(p, b));
        h.inventory.setItem(15, Items.named(Material.ARROW, "Withdraw ammunition", "Click to take up to 64 arrows"));
        h.actions.put(15, () -> {
            if (!canManage(p, b)) return;
            int current = plugin.ammunition.count(b), n = Math.min(current, 64);
            if (n == 0) return;
            ItemStack[] before = TurretMenu.cloneContents(p.getInventory().getStorageContents());
            if (!p.getInventory().addItem(new ItemStack(Material.ARROW, n)).isEmpty()) { p.getInventory().setStorageContents(before); return; }
            plugin.ammunition.take(b, n); plugin.store.save(); open(p, b);
        });
        p.openInventory(h.inventory);
    }
    void tick() {
        long tick = Bukkit.getCurrentTick();
        for (UUID id : new java.util.ArrayList<>(shots.keySet())) {
            Shot shot = shots.get(id);
            if (!(Bukkit.getEntity(id) instanceof Arrow arrow)) { shots.remove(id); continue; }
            if (tick >= shot.expiry() || arrow.isInBlock() || !(Bukkit.getEntity(shot.target()) instanceof Phantom phantom)
                || !phantom.isValid() || !phantom.getWorld().equals(arrow.getWorld()) || !plugin.enabled()) {
                arrow.remove(); shots.remove(id); continue;
            }
            double r = plugin.decimal("turrets.radius", 24, 1, 64);
            if (phantom.getLocation().distanceSquared(shot.origin()) > r * r) { arrow.remove(); shots.remove(id); continue; }
            if (plugin.getConfig().getBoolean("turrets.homing", true)) {
                Vector toward = phantom.getBoundingBox().getCenter().subtract(arrow.getLocation().toVector());
                if (toward.lengthSquared() > .001) arrow.setVelocity(toward.normalize().multiply(plugin.decimal("turrets.arrow-speed", 1.6, .5, 3)));
            }
        }
        if (!plugin.enabled() || tick % 5 != 0) return;
        var section = plugin.store.data.getConfigurationSection("turrets"); if (section == null) return;
        for (String id : section.getKeys(false)) {
            String path = "turrets." + id;
            Location loc = Store.decode(plugin.store.data.getString(path + ".location"));
            if (!Store.loaded(loc) || plugin.getConfig().getStringList("disabled-worlds").contains(loc.getWorld().getName())) continue;
            Block b = loc.getBlock();
            if (!structure(b)) { dismantle(b); continue; }
            int ammo = plugin.ammunition.count(b);
            if (ammo <= 0 || nextShot.getOrDefault(id, 0L) > tick) continue;
            Location muzzle = loc.clone().add(.5, 2.1, .5);
            double r = plugin.decimal("turrets.radius", 24, 1, 64);
            Phantom target = loc.getWorld().getNearbyEntities(loc.clone().add(.5, 1, .5), r, r, r).stream()
                .filter(Phantom.class::isInstance).map(Phantom.class::cast)
                .filter(p -> p.isValid() && p.getLocation().distanceSquared(muzzle) <= r * r && visible(muzzle, p))
                .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(muzzle))).orElse(null);
            if (target == null) continue;
            Vector direction = target.getBoundingBox().getCenter().subtract(muzzle.toVector());
            if (direction.lengthSquared() < .001) continue;
            // Commit ammunition before spawning, so a restart cannot duplicate a fired arrow.
            if (plugin.ammunition.take(b, 1) != 1) continue;
            plugin.store.save();
            Arrow arrow = loc.getWorld().spawnArrow(muzzle, direction.normalize(), (float) plugin.decimal("turrets.arrow-speed", 1.6, .5, 3), 0);
            arrow.getPersistentDataContainer().set(plugin.key("turret_arrow"), PersistentDataType.BYTE, (byte) 1);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED); arrow.setCritical(false);
            arrow.setGravity(!plugin.getConfig().getBoolean("turrets.homing", true));
            arrow.setDamage(plugin.decimal("turrets.damage", 6, .1, 100));
            shots.put(arrow.getUniqueId(), new Shot(target.getUniqueId(), muzzle, tick + 100));
            nextShot.put(id, tick + plugin.integer("turrets.cooldown-ticks", 30, 5, 1200));
        }
    }
    private boolean visible(Location muzzle, Phantom target) {
        Vector v = target.getBoundingBox().getCenter().subtract(muzzle.toVector());
        double d = v.length(); return d > .01 && muzzle.getWorld().rayTraceBlocks(muzzle, v.normalize(), d, org.bukkit.FluidCollisionMode.NEVER, true) == null;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Arrow a && a.getPersistentDataContainer().has(plugin.key("turret_arrow"))) {
            if (!(e.getEntity() instanceof Phantom)) { e.setCancelled(true); a.remove(); }
            else e.setDamage(plugin.decimal("turrets.damage", 6, .1, 100));
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void hit(ProjectileHitEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(plugin.key("turret_arrow"))) {
            UUID id = e.getEntity().getUniqueId(); plugin.later(() -> { e.getEntity().remove(); shots.remove(id); });
        }
    }
    private void dismantle(Block any) {
        Block b = base(any); String path = "turrets." + Store.id(b);
        if (!plugin.store.data.contains(path)) return;
        int ammo = plugin.ammunition.count(b); plugin.ammunition.forget(b);
        plugin.store.set(path, null); plugin.store.save(); nextShot.remove(Store.id(b));
        for (int n = ammo; n > 0; n -= 64) b.getWorld().dropItemNaturally(b.getLocation().add(.5, 1, .5), new ItemStack(Material.ARROW, Math.min(64, n)));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void broken(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.IRON_BLOCK && !(b.getBlockData() instanceof LightningRod)) return;
        Block oldBase = base(b);
        plugin.later(() -> { if (!e.isCancelled() && !structure(oldBase)) dismantle(oldBase); });
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void explode(EntityExplodeEvent e) { e.blockList().forEach(this::dismantle); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void explode(BlockExplodeEvent e) { e.blockList().forEach(this::dismantle); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> plugin.store.data.contains("turrets." + Store.id(base(b))))) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> plugin.store.data.contains("turrets." + Store.id(base(b))))) e.setCancelled(true);
    }
    void close() { for (UUID id : shots.keySet()) if (Bukkit.getEntity(id) instanceof Arrow a) a.remove(); shots.clear(); }
    @EventHandler public void chunkLoaded(ChunkLoadEvent e) {
        for (var entity : e.getChunk().getEntities())
            if (entity instanceof Arrow && entity.getPersistentDataContainer().has(plugin.key("turret_arrow"))
                && !shots.containsKey(entity.getUniqueId())) entity.remove();
    }
}
