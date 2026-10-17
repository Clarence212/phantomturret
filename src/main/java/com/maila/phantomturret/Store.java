package com.maila.phantomturret;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

final class Store {
    final YamlConfiguration data;
    private final PhantomTurretPlugin plugin; private final File file; private boolean dirty;
    Store(PhantomTurretPlugin plugin) {
        this.plugin = plugin; file = new File(plugin.getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }
    void set(String path, Object value) { data.set(path, value); dirty = true; }
    void saveIfDirty() { if (dirty) save(); }
    void save() {
        try {
            Files.createDirectories(file.toPath().getParent());
            var temp = file.toPath().resolveSibling("data.yml.tmp"); Files.writeString(temp, data.saveToString());
            try { Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        } catch (IOException e) { plugin.getLogger().severe("Unable to save PhantomTurret data: " + e.getMessage()); }
    }
    static String id(Block b) { return b.getWorld().getUID() + "_" + b.getX() + "_" + b.getY() + "_" + b.getZ(); }
    static String encode(Location l) { return l.getWorld().getUID() + "," + l.getX() + "," + l.getY() + "," + l.getZ(); }
    static Location decode(String text) {
        if (text == null) return null;
        try { String[] a = text.split(","); World w = Bukkit.getWorld(UUID.fromString(a[0]));
            return w == null ? null : new Location(w, Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]));
        } catch (RuntimeException e) { return null; }
    }
    static boolean loaded(Location l) { return l != null && l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4); }
}
