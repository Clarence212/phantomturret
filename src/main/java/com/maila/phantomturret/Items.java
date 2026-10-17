package com.maila.phantomturret;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

final class Items {
    private Items() {}
    static boolean plain(ItemStack item) { return item != null && !item.hasItemMeta(); }
    static boolean empty(ItemStack item) { return item == null || item.getType().isAir() || item.getAmount() <= 0; }
    static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material); var meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.Arrays.stream(lore).map(line -> Component.text(line).decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta); return item;
    }
}
