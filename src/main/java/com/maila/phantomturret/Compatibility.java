package com.maila.phantomturret;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.RegionAccessor;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

final class Compatibility {
    private static final Method SPAWN = spawnMethod();
    private static final Method ARROW_ITEM_STACK = method(AbstractArrow.class, "setItemStack", ItemStack.class);
    private static final Method ARROW_ITEM = method(AbstractArrow.class, "setItem", ItemStack.class);
    private static final Method POTION_GET = method(PotionMeta.class, "getBasePotionType");
    private static final Method POTION_SET = method(Arrow.class, "setBasePotionType", PotionType.class);
    private static final Method ACCELERATION = method(Fireball.class, "setAcceleration", Vector.class);

    private Compatibility() {}

    private static Method method(Class<?> type, String name, Class<?>... parameters) {
        try { return type.getMethod(name, parameters); }
        catch (NoSuchMethodException ignored) { return null; }
    }

    private static Method spawnMethod() {
        Method legacy = null;
        for (Method candidate : RegionAccessor.class.getMethods()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            if (!candidate.getName().equals("spawn") || parameters.length != 3
                || parameters[0] != Location.class || parameters[1] != Class.class) continue;
            if (parameters[2] == Consumer.class) return candidate;
            if (parameters[2].getName().equals("org.bukkit.util.Consumer")) legacy = candidate;
        }
        if (legacy == null) throw new IllegalStateException("No compatible Bukkit projectile spawn method is available.");
        return legacy;
    }

    private static Object invoke(Method method, Object receiver, Object... arguments) {
        try { return method.invoke(receiver, arguments); }
        catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }

    static Projectile spawn(Location location, Class<? extends Projectile> type, Consumer<Projectile> configure) {
        Class<?> consumerType = SPAWN.getParameterTypes()[2];
        Object callback = consumerType.isInstance(configure) ? configure
            : Proxy.newProxyInstance(consumerType.getClassLoader(), new Class<?>[] {consumerType}, (proxy, method, arguments) -> {
                return switch (method.getName()) {
                    case "accept" -> { configure.accept((Projectile) arguments[0]); yield null; }
                    case "toString" -> "PhantomTurret projectile initializer";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
        return (Projectile) invoke(SPAWN, location.getWorld(), location, type, callback);
    }

    static Class<? extends Projectile> projectile(String name) {
        try { return Class.forName("org.bukkit.entity." + name).asSubclass(Projectile.class); }
        catch (ClassNotFoundException | LinkageError ignored) { return null; }
    }

    static void arrowItem(AbstractArrow arrow, ItemStack item) {
        Method setter = ARROW_ITEM_STACK != null ? ARROW_ITEM_STACK : ARROW_ITEM;
        if (setter != null) invoke(setter, arrow, item);
        if (arrow instanceof Arrow tipped && item.getItemMeta() instanceof PotionMeta potion) {
            if (POTION_GET != null && POTION_SET != null) invoke(POTION_SET, tipped, invoke(POTION_GET, potion));
            else tipped.setBasePotionData(potion.getBasePotionData());
            tipped.clearCustomEffects();
            for (var effect : potion.getCustomEffects()) tipped.addCustomEffect(effect, true);
            if (potion.hasColor()) tipped.setColor(potion.getColor());
        }
    }

    static void stopAcceleration(Fireball projectile) {
        if (ACCELERATION != null) invoke(ACCELERATION, projectile, new Vector());
    }
}
