package com.maila.phantomturret;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class Hooks {
    private final PhantomTurretPlugin plugin;
    private final Set<String> warned = new HashSet<>();
    Hooks(PhantomTurretPlugin plugin) { this.plugin = plugin; }
    private Plugin installed(String name) {
        Plugin owner = Bukkit.getPluginManager().getPlugin(name);
        return owner != null && owner.isEnabled() ? owner : null;
    }
    private void warn(String hook, Exception cause) {
        if (warned.add(hook)) plugin.getLogger().warning(hook + " integration unavailable: " + cause.getClass().getSimpleName());
    }
    boolean authenticated(Player player) {
        Plugin owner = installed("AuthMe"); if (owner == null) return true;
        try {
            Class<?> type = Class.forName("fr.xephi.authme.api.v3.AuthMeApi", true, owner.getClass().getClassLoader());
            Object api = type.getMethod("getInstance").invoke(null);
            if ((boolean) type.getMethod("isAuthenticated", Player.class).invoke(api, player)) return true;
            try { return (boolean) type.getMethod("isUnrestricted", Player.class).invoke(api, player); }
            catch (NoSuchMethodException ignored) { return false; }
        } catch (ReflectiveOperationException | RuntimeException e) { warn("AuthMe", e); return false; }
    }
    boolean vanished(Player player) {
        for (var value : player.getMetadata("vanished")) if (value.asBoolean()) return true;
        Plugin owner = installed("SuperVanish"); if (owner == null) return false;
        try {
            Class<?> type = Class.forName("de.myzelyam.api.vanish.VanishAPI", true, owner.getClass().getClassLoader());
            return (boolean) type.getMethod("isInvisible", Player.class).invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException e) { warn("SuperVanish", e); return false; }
    }
}
