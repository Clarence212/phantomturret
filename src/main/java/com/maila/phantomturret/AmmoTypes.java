package com.maila.phantomturret;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.WindCharge;
import org.bukkit.inventory.ItemStack;

/** Supported ammunition and its native projectile. Item metadata stays on the item. */
final class AmmoTypes {
    private AmmoTypes() {}

    static Class<? extends Projectile> projectile(Material material) {
        return switch (material) {
            case ARROW, TIPPED_ARROW -> Arrow.class;
            case SPECTRAL_ARROW -> SpectralArrow.class;
            case FIREWORK_ROCKET -> Firework.class;
            case SNOWBALL -> Snowball.class;
            case EGG, BLUE_EGG, BROWN_EGG -> Egg.class;
            case WIND_CHARGE -> WindCharge.class;
            default -> null;
        };
    }

    static int stackSize(ItemStack item) { return Math.min(64, item.getMaxStackSize()); }

    static boolean supported(ItemStack item) {
        return !Items.empty(item) && projectile(item.getType()) != null && item.getAmount() <= stackSize(item);
    }

    static boolean supportedOrEmpty(ItemStack item) { return Items.empty(item) || supported(item); }
}
