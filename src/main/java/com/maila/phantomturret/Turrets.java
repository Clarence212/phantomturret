package com.maila.phantomturret;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.LightningRod;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

final class Turrets implements Listener {
    private final PhantomTurretPlugin plugin;
    private final Map<String, Long> nextShot = new HashMap<>();
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
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Player p = e.getPlayer(); Block b = base(e.getClickedBlock());
        if (!plugin.allowed(p) || !structure(b)) return;
        String id = Store.id(b); if (!owned(p, id)) return;
        // Opening the controls takes priority over using or placing the held item.
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack held = p.getInventory().getItemInMainHand();
        if (p.isSneaking() && AmmoTypes.supported(held)) {
            plugin.ammunition.register(p, b);
            int take = plugin.ammunition.add(b, held);
            if (take <= 0) return;
            plugin.store.save();
            held.setAmount(held.getAmount() - take);
        } else open(p, b);
    }
    private void open(Player p, Block b) {
        if (!plugin.allowed(p) || !structure(b)) return;
        String id = Store.id(b); if (!owned(p, id)) return;
        TurretMenu.Holder h = new TurretMenu.Holder(); h.inventory = Bukkit.createInventory(h, 27, "Phantom Turret");
        int ammo = plugin.ammunition.count(b);
        h.inventory.setItem(11, Items.named(Material.LIGHTNING_ROD, "Phantom Turret", "Ammunition: " + ammo,
            "Range: " + plugin.turretRadius() + " blocks", "Click to open 27 ammunition slots"));
        h.actions.put(11, () -> plugin.ammunition.open(p, b));
        h.inventory.setItem(15, Items.named(Material.ARROW, "Withdraw ammunition", "Click to take the first loaded stack"));
        h.actions.put(15, () -> {
            if (!canManage(p, b)) return;
            ItemStack stack = plugin.ammunition.firstStack(b);
            if (stack == null) return;
            ItemStack[] before = TurretMenu.cloneContents(p.getInventory().getStorageContents());
            if (!p.getInventory().addItem(stack.clone()).isEmpty()) { p.getInventory().setStorageContents(before); return; }
            plugin.ammunition.takeFirst(b, stack.getAmount()); plugin.store.save(); open(p, b);
        });
        p.openInventory(h.inventory);
    }
    void tick() {
        plugin.projectiles.tick();
        long tick = plugin.currentTick();
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
            double r = plugin.turretRadius();
            Phantom target = loc.getWorld().getNearbyEntities(muzzle, r, r, r).stream()
                .filter(Phantom.class::isInstance).map(Phantom.class::cast)
                .filter(p -> p.isValid() && p.getLocation().distanceSquared(muzzle) <= r * r && visible(muzzle, p))
                .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(muzzle))).orElse(null);
            if (target == null) continue;
            Vector direction = target.getBoundingBox().getCenter().subtract(muzzle.toVector());
            if (direction.lengthSquared() < .001) continue;
            // Commit the actual ammunition before spawning; preserve its metadata if launch is denied.
            ItemStack round = plugin.ammunition.takeFirst(b, 1);
            if (round == null) continue;
            plugin.store.save();
            if (!plugin.projectiles.fire(muzzle, target, round)) {
                // Launch events may run other plugins, so recheck the structure and available storage.
                if (!structure(b) || !plugin.store.data.contains(path) || plugin.ammunition.add(b, round) != 1)
                    b.getWorld().dropItemNaturally(b.getLocation().add(.5, 1, .5), round);
                plugin.store.save();
            }
            nextShot.put(id, tick + plugin.integer("turrets.cooldown-ticks", 30, 5, 1200));
        }
    }
    private boolean visible(Location muzzle, Phantom target) {
        Vector v = target.getBoundingBox().getCenter().subtract(muzzle.toVector());
        double d = v.length(); return d > .01 && muzzle.getWorld().rayTraceBlocks(muzzle, v.normalize(), d, org.bukkit.FluidCollisionMode.NEVER, true) == null;
    }
    private void dismantle(Block any) {
        Block b = base(any); String path = "turrets." + Store.id(b);
        if (!plugin.store.data.contains(path)) return;
        var ammo = plugin.ammunition.contents(b); plugin.ammunition.forget(b);
        plugin.store.set(path, null); plugin.store.save(); nextShot.remove(Store.id(b));
        for (ItemStack item : ammo) b.getWorld().dropItemNaturally(b.getLocation().add(.5, 1, .5), item);
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
}
