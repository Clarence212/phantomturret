package com.maila.phantomturret;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

final class TurretMenu implements Listener {
    private final PhantomTurretPlugin plugin;
    TurretMenu(PhantomTurretPlugin plugin) { this.plugin = plugin; }
    static final class Holder implements InventoryHolder {
        Inventory inventory;
        final Map<Integer, Runnable> actions = new HashMap<>();
        @Override public Inventory getInventory() { return inventory; }
    }
    static ItemStack[] cloneContents(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) copy[index] = items[index] == null ? null : items[index].clone();
        return copy;
    }
    private static boolean mouseClick(ClickType click) {
        return switch (click) {
            case LEFT, SHIFT_LEFT, RIGHT, SHIFT_RIGHT, MIDDLE, DOUBLE_CLICK,
                WINDOW_BORDER_LEFT, WINDOW_BORDER_RIGHT -> true;
            default -> false;
        };
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        boolean previouslyCancelled = event.isCancelled(); event.setCancelled(true);
        if (previouslyCancelled || !(event.getWhoClicked() instanceof Player player) || !plugin.allowed(player)) return;
        Runnable action = holder.actions.get(event.getRawSlot());
        if (action != null && mouseClick(event.getClick())) plugin.later(() -> {
            if (plugin.allowed(player) && player.getOpenInventory().getTopInventory().equals(holder.inventory)) action.run();
        });
    }
    @EventHandler public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }
    void close() {
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Holder) player.closeInventory();
    }
}
