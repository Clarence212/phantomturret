package com.maila.phantomturret;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** One shared, persistent 27 slot ammunition inventory for each turret. */
final class TurretAmmo implements Listener {
    static final int SLOTS = 27;
    private final PhantomTurretPlugin plugin;
    private final Map<String, Holder> loaded = new HashMap<>();

    static final class Holder implements InventoryHolder {
        final String id;
        final Location location;
        Inventory inventory;
        int legacyOverflow;
        Holder(Block block) { id = Store.id(block); location = block.getLocation(); }
        @Override public Inventory getInventory() { return inventory; }
    }

    TurretAmmo(PhantomTurretPlugin plugin) { this.plugin = plugin; }
    private static String path(Holder holder) { return "turrets." + holder.id; }

    private Holder get(Block block) {
        Holder holder = loaded.computeIfAbsent(Store.id(block), id -> {
            Holder created = new Holder(block);
            created.inventory = Bukkit.createInventory(created, SLOTS, "Phantom Turret Ammo");
            var stored = plugin.store.data.getList(path(created) + ".ammo-inventory");
            if (stored != null) {
                for (int slot = 0; slot < Math.min(stored.size(), SLOTS); slot++)
                    if (stored.get(slot) instanceof ItemStack item && AmmoTypes.supported(item)) created.inventory.setItem(slot, item.clone());
                created.legacyOverflow = Math.max(0, plugin.store.data.getInt(path(created) + ".legacy-overflow"));
                fillLegacyOverflow(created);
            } else {
                int legacy = Math.max(0, plugin.store.data.getInt(path(created) + ".ammo"));
                created.legacyOverflow = legacy - put(created, new ItemStack(Material.ARROW), legacy);
            }
            return created;
        });
        // Convert the old counter once; later saves use the actual slot contents.
        if (plugin.store.data.contains(path(holder) + ".ammo")) write(holder);
        return holder;
    }

    private static int physicalCount(Holder holder) {
        int count = 0;
        for (ItemStack item : holder.inventory.getContents()) if (AmmoTypes.supported(item)) count += item.getAmount();
        return count;
    }

    private static int put(Holder holder, ItemStack template, int amount) {
        int remaining = Math.max(0, amount);
        // Merge identical items first, then use empty slots. Never merge different potion/rocket data.
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < SLOTS && remaining > 0; slot++) {
                ItemStack item = holder.inventory.getItem(slot);
                boolean empty = Items.empty(item);
                if (pass == 0 ? empty || !item.isSimilar(template) : !empty) continue;
                int current = empty ? 0 : item.getAmount();
                int add = Math.min(remaining, AmmoTypes.stackSize(template) - current);
                if (add <= 0) continue;
                ItemStack stack = template.clone(); stack.setAmount(current + add);
                holder.inventory.setItem(slot, stack); remaining -= add;
            }
        }
        return Math.max(0, amount) - remaining;
    }

    private static void fillLegacyOverflow(Holder holder) {
        if (holder.legacyOverflow > 0)
            holder.legacyOverflow -= put(holder, new ItemStack(Material.ARROW), holder.legacyOverflow);
    }

    private void write(Holder holder) {
        if (loaded.get(holder.id) != holder || !plugin.store.data.contains(path(holder) + ".location")) return;
        fillLegacyOverflow(holder);
        plugin.store.set(path(holder) + ".ammo-inventory", Arrays.asList(TurretMenu.cloneContents(holder.inventory.getContents())));
        plugin.store.set(path(holder) + ".legacy-overflow", holder.legacyOverflow > 0 ? holder.legacyOverflow : null);
        plugin.store.set(path(holder) + ".ammo", null);
    }

    void register(Player player, Block block) {
        String root = "turrets." + Store.id(block);
        plugin.store.set(root + ".location", Store.encode(block.getLocation()));
        if (plugin.store.data.getString(root + ".owner") == null)
            plugin.store.set(root + ".owner", player.getUniqueId().toString());
        write(get(block));
    }

    int count(Block block) {
        Holder holder = get(block);
        return (int) Math.min(Integer.MAX_VALUE, physicalCount(holder) + (long) holder.legacyOverflow);
    }

    int add(Block block, ItemStack item) {
        if (!AmmoTypes.supported(item)) return 0;
        Holder holder = get(block); fillLegacyOverflow(holder);
        int added = put(holder, item, item.getAmount()); write(holder); return added;
    }

    ItemStack firstStack(Block block) {
        Holder holder = get(block); fillLegacyOverflow(holder);
        for (ItemStack item : holder.inventory.getContents())
            if (AmmoTypes.supported(item)) return item.clone();
        return null;
    }

    ItemStack takeFirst(Block block, int maximum) {
        Holder holder = get(block); fillLegacyOverflow(holder);
        if (maximum <= 0) return null;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack item = holder.inventory.getItem(slot);
            if (!AmmoTypes.supported(item)) continue;
            int take = Math.min(maximum, item.getAmount());
            ItemStack taken = item.clone(); taken.setAmount(take);
            ItemStack rest = item.clone(); rest.setAmount(item.getAmount() - take);
            holder.inventory.setItem(slot, rest.getAmount() == 0 ? null : rest);
            fillLegacyOverflow(holder); write(holder); return taken;
        }
        return null;
    }

    List<ItemStack> contents(Block block) {
        Holder holder = get(block);
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : holder.inventory.getContents())
            if (AmmoTypes.supported(item)) items.add(item.clone());
        for (int remaining = holder.legacyOverflow; remaining > 0; remaining -= 64)
            items.add(new ItemStack(Material.ARROW, Math.min(64, remaining)));
        return items;
    }

    void open(Player player, Block block) {
        if (!plugin.turrets.canManage(player, block)) return;
        Holder holder = get(block);
        var view = player.openInventory(holder.inventory);
        if (view == null || !player.getOpenInventory().getTopInventory().equals(holder.inventory)) return;
        if (!plugin.turrets.canManage(player, block)) { player.closeInventory(); return; }
        register(player, block); plugin.store.save();
    }

    private boolean canEdit(Player player, Holder holder) {
        return loaded.get(holder.id) == holder && Store.loaded(holder.location)
            && plugin.turrets.canManage(player, holder.location.getBlock());
    }

    private void closeLater(Player player, Holder holder) {
        plugin.later(() -> {
            if (player.getOpenInventory().getTopInventory().equals(holder.inventory)) player.closeInventory();
        });
    }

    private void saveLater(Holder holder) {
        plugin.later(() -> {
            if (loaded.get(holder.id) == holder) { write(holder); plugin.store.save(); }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)
            || !(event.getWhoClicked() instanceof Player player)) return;
        if (!canEdit(player, holder)) { event.setCancelled(true); closeLater(player, holder); return; }
        boolean top = event.getRawSlot() >= 0 && event.getRawSlot() < SLOTS;
        boolean accepted = true;
        if (top) {
            accepted = switch (event.getAction()) {
                case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> AmmoTypes.supportedOrEmpty(event.getCursor());
                case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                    ItemStack incoming = event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND
                        ? player.getInventory().getItemInOffHand()
                        : event.getHotbarButton() >= 0 && event.getHotbarButton() < 9
                            ? player.getInventory().getItem(event.getHotbarButton()) : null;
                    yield AmmoTypes.supportedOrEmpty(incoming);
                }
                case NOTHING, PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, DROP_ALL_SLOT,
                     DROP_ONE_SLOT, MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR -> true;
                default -> false;
            };
        } else if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            accepted = AmmoTypes.supported(event.getCurrentItem());
        }
        if (!accepted) { event.setCancelled(true); return; }
        // Bukkit applies accepted clicks after the event, save their final slot contents next tick.
        saveLater(holder);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)
            || !(event.getWhoClicked() instanceof Player player)) return;
        if (!canEdit(player, holder)) { event.setCancelled(true); closeLater(player, holder); return; }
        for (var slot : event.getNewItems().entrySet()) {
            if (slot.getKey() >= 0 && slot.getKey() < SLOTS && !AmmoTypes.supported(slot.getValue())) {
                event.setCancelled(true); return;
            }
        }
        saveLater(holder);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void closed(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Holder holder && loaded.get(holder.id) == holder) {
            write(holder); plugin.store.save();
        }
    }

    private void retire(Holder holder) {
        loaded.remove(holder.id, holder);
        for (var viewer : new ArrayList<>(holder.inventory.getViewers())) viewer.closeInventory();
    }

    void forget(Block block) {
        Holder holder = loaded.get(Store.id(block));
        if (holder != null) { retire(holder); holder.inventory.clear(); holder.legacyOverflow = 0; }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void unloaded(ChunkUnloadEvent event) {
        for (Holder holder : new ArrayList<>(loaded.values())) {
            Location location = holder.location;
            if (location.getWorld().equals(event.getWorld()) && (location.getBlockX() >> 4) == event.getChunk().getX()
                && (location.getBlockZ() >> 4) == event.getChunk().getZ()) {
                write(holder); retire(holder);
            }
        }
        plugin.store.saveIfDirty();
    }

    void close() {
        for (Holder holder : new ArrayList<>(loaded.values())) { write(holder); retire(holder); }
    }
}
