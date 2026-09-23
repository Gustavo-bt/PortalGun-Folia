package pl.by.fentisdev.portalgun.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class SchedulerUtil {

    private static Boolean folia = null;

    public static boolean isFolia() {
        if (folia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                folia = true;
            } catch (ClassNotFoundException e) {
                folia = false;
            }
        }
        return folia;
    }

    public static void runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), Math.max(1, delayTicks));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runTaskLaterForPlayer(Plugin plugin, Player player, Runnable task, long delayTicks) {
        if (isFolia()) {
            player.getScheduler().runDelayed(plugin, t -> task.run(), null, Math.max(1, delayTicks));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static Object runRepeatingGlobal(Plugin plugin, Runnable task, long delay, long period) {
        if (isFolia()) {
            return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), Math.max(1, delay), Math.max(1, period));
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    public static void cancelTask(Object task) {
        if (task == null) return;
        try {
            task.getClass().getMethod("cancel").invoke(task);
        } catch (Exception ignored) {}
        if (task instanceof BukkitTask) {
            try { ((BukkitTask) task).cancel(); } catch (Exception ignored) {}
        }
    }

    public static void runAtLocation(Plugin plugin, Location loc, Runnable task) {
        if (isFolia()) {
            Bukkit.getRegionScheduler().run(plugin, loc, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void teleportAsync(Entity entity, Location loc) {
        entity.teleportAsync(loc);
    }
}