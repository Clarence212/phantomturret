package com.maila.phantomturret;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhantomTurretPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private static final double DEFAULT_RADIUS = 24;
    private static final double MIN_RADIUS = 1;
    private static final double MAX_RADIUS = 64;
    Store store;
    Hooks hooks;
    TurretMenu menu;
    Turrets turrets;
    TurretAmmo ammunition;
    TurretShots projectiles;
    private long currentTick;

    @Override public void onEnable() {
        saveDefaultConfig();
        store = new Store(this); hooks = new Hooks(this); menu = new TurretMenu(this); turrets = new Turrets(this);
        ammunition = new TurretAmmo(this);
        projectiles = new TurretShots(this);
        Bukkit.getPluginManager().registerEvents(menu, this);
        Bukkit.getPluginManager().registerEvents(turrets, this);
        Bukkit.getPluginManager().registerEvents(ammunition, this);
        Bukkit.getPluginManager().registerEvents(projectiles, this);
        projectiles.removeLoadedOrphans();
        getCommand("phantomturret").setExecutor(this);
        getCommand("phantomturret").setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer(this, () -> { currentTick++; turrets.tick(); }, 1, 1);
        Bukkit.getScheduler().runTaskTimer(this, store::saveIfDirty, 100, 100);
        getLogger().info("PhantomTurret " + getDescription().getVersion() + " enabled on " + Bukkit.getName() + " " + Bukkit.getBukkitVersion() + ".");
    }

    @Override public void onDisable() {
        if (ammunition != null) ammunition.close();
        if (menu != null) menu.close();
        if (projectiles != null) projectiles.close();
        if (store != null) store.save();
    }

    boolean enabled() { return getConfig().getBoolean("enabled", true); }
    long currentTick() { return currentTick; }
    double turretRadius() { return decimal("turrets.radius", DEFAULT_RADIUS, MIN_RADIUS, MAX_RADIUS); }

    boolean allowed(Player p) {
        return enabled() && p.isOnline() && !p.isDead() && p.getGameMode() != GameMode.SPECTATOR
            && p.hasPermission("phantomturret.use")
            && !getConfig().getStringList("disabled-worlds").contains(p.getWorld().getName())
            && hooks.authenticated(p) && !hooks.vanished(p);
    }

    NamespacedKey key(String name) { return new NamespacedKey(this, name); }
    void later(Runnable work) { Bukkit.getScheduler().runTask(this, work); }
    int integer(String path, int fallback, int min, int max) { return Math.max(min, Math.min(max, getConfig().getInt(path, fallback))); }
    double decimal(String path, double fallback, double min, double max) {
        double value = getConfig().getDouble(path, fallback);
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p && (!hooks.authenticated(p) || hooks.vanished(p))) return true;
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload") || !sender.hasPermission("phantomturret.admin")) return true;
        reloadConfig();
        sender.sendMessage("PhantomTurret configuration reloaded.");
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && sender.hasPermission("phantomturret.admin")
            && "reload".startsWith(args[0].toLowerCase(java.util.Locale.ROOT)) ? List.of("reload") : List.of();
    }
}
