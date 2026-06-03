package com.nuclyon.technicallycoded.inventoryrollback.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Direct Folia implementation of {@link FoliaSchedulerService}.
 */
public class DirectFoliaSchedulerService implements FoliaSchedulerService {

    private static final long TICK_MILLIS = 50L;

    private final Plugin plugin;

    public DirectFoliaSchedulerService(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @Nullable
    public ScheduledTask entity(@NotNull Entity entity, @NotNull Runnable task) {
        return entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }

    @Override
    @Nullable
    public ScheduledTask entityLater(@NotNull Entity entity, @NotNull Runnable task, long delayTicks) {
        return entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
    }

    @Override
    @Nullable
    public ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), null, initialDelayTicks, periodTicks);
    }

    @Override
    @NotNull
    public <T> CompletableFuture<T> entityFuture(@NotNull Entity entity, @NotNull Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ScheduledTask scheduledTask = entity.getScheduler().run(
                plugin,
                ignored -> complete(future, task),
                () -> future.completeExceptionally(new IllegalStateException("Entity scheduler retired before task execution."))
        );
        if (scheduledTask == null) {
            future.completeExceptionally(new IllegalStateException("Entity scheduler is retired."));
        }
        return future;
    }

    @Override
    @NotNull
    public ScheduledTask region(@NotNull Location location, @NotNull Runnable task) {
        return plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
    }

    @Override
    @NotNull
    public ScheduledTask regionLater(@NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return plugin.getServer().getRegionScheduler().runDelayed(plugin, location, scheduledTask -> task.run(), delayTicks);
    }

    @Override
    @NotNull
    public ScheduledTask regionTimer(@NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, scheduledTask -> task.run(), initialDelayTicks, periodTicks);
    }

    @Override
    @NotNull
    public <T> CompletableFuture<T> regionFuture(@NotNull Location location, @NotNull Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getServer().getRegionScheduler().execute(plugin, location, () -> complete(future, task));
        return future;
    }

    @Override
    @NotNull
    public ScheduledTask global(@NotNull Runnable task) {
        return plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    }

    @Override
    @NotNull
    public ScheduledTask globalLater(@NotNull Runnable task, long delayTicks) {
        return plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
    }

    @Override
    @NotNull
    public ScheduledTask globalTimer(@NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), initialDelayTicks, periodTicks);
    }

    @Override
    @NotNull
    public <T> CompletableFuture<T> globalFuture(@NotNull Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> complete(future, task));
        return future;
    }

    @Override
    @NotNull
    public ScheduledTask async(@NotNull Runnable task) {
        return plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    @NotNull
    public ScheduledTask asyncLater(@NotNull Runnable task, long delayTicks) {
        return plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), ticksToMillis(delayTicks), TimeUnit.MILLISECONDS);
    }

    @Override
    @NotNull
    public ScheduledTask asyncTimer(@NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return plugin.getServer().getAsyncScheduler().runAtFixedRate(
                plugin,
                scheduledTask -> task.run(),
                ticksToMillis(initialDelayTicks),
                ticksToMillis(periodTicks),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    @NotNull
    public <T> CompletableFuture<T> asyncFuture(@NotNull Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> complete(future, task));
        return future;
    }

    private static long ticksToMillis(long ticks) {
        return Math.max(0L, ticks) * TICK_MILLIS;
    }

    private static <T> void complete(@NotNull CompletableFuture<T> future, @NotNull Callable<T> task) {
        try {
            future.complete(task.call());
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }
}
