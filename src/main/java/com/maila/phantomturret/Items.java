package com.maila.phantomturret;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

final class Items {
    private Items() {}
    static boolean plain(ItemStack item) { return item != null && !item.hasItemMeta(); }
    static boolean empty(ItemStack item) { return item == null || item.getType().isAir() || item.getAmount() <= 0; }
    static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material); var meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + name);
        meta.setLore(java.util.Arrays.stream(lore).map(line -> ChatColor.RESET + line).toList());
        item.setItemMeta(meta); return item;
    }
}
