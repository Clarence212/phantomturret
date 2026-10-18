package com.maila.phantomturret;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.inventory.ItemStack;

/** Supported ammunition and its native projectile. Item metadata stays on the item. */
final class AmmoTypes {
    private static final Class<? extends Projectile> WIND_CHARGE = Compatibility.projectile("WindCharge");
    private AmmoTypes() {}

    static Class<? extends Projectile> projectile(Material material) {
        return switch (material.name()) {
            case "ARROW", "TIPPED_ARROW" -> Arrow.class;
            case "SPECTRAL_ARROW" -> SpectralArrow.class;
            case "FIREWORK_ROCKET" -> Firework.class;
            case "SNOWBALL", "EGG", "BLUE_EGG", "BROWN_EGG" -> Snowball.class;
            case "WIND_CHARGE" -> WIND_CHARGE;
            default -> null;
        };
    }

    static int stackSize(ItemStack item) { return Math.min(64, item.getMaxStackSize()); }

    static boolean supported(ItemStack item) {
        return !Items.empty(item) && projectile(item.getType()) != null && item.getAmount() <= stackSize(item);
    }

    static boolean supportedOrEmpty(ItemStack item) { return Items.empty(item) || supported(item); }
}
