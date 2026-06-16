package com.nuclyon.technicallycoded.inventoryrollback.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Scheduler facade for Folia's ownership-aware schedulers.
 * <p>
 * Callers must choose the scheduler that owns the state they are about to touch:
 * entity-owned state, location/region-owned state, global-region state, or pure async work.
 */
public interface FoliaSchedulerService {

    @Nullable
    default ScheduledTask entity(@NotNull Entity entity, @NotNull Runnable task) {
        return entity(entity, task, null);
    }

    @Nullable
    ScheduledTask entity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired);

    @Nullable
    default ScheduledTask entityLater(@NotNull Entity entity, @NotNull Runnable task, long delayTicks) {
        return entityLater(entity, task, null, delayTicks);
    }

    @Nullable
    ScheduledTask entityLater(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks);

    @Nullable
    default ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return entityTimer(entity, task, null, initialDelayTicks, periodTicks);
    }

    @Nullable
    ScheduledTask entityTimer(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks);

    @NotNull
    <T> CompletableFuture<T> entityFuture(@NotNull Entity entity, @NotNull Callable<T> task);

    @NotNull
    ScheduledTask region(@NotNull Location location, @NotNull Runnable task);

    @NotNull
    ScheduledTask regionLater(@NotNull Location location, @NotNull Runnable task, long delayTicks);

    @NotNull
    ScheduledTask regionTimer(@NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks);

    @NotNull
    <T> CompletableFuture<T> regionFuture(@NotNull Location location, @NotNull Callable<T> task);

    @NotNull
    ScheduledTask global(@NotNull Runnable task);

    @NotNull
    ScheduledTask globalLater(@NotNull Runnable task, long delayTicks);

    @NotNull
    ScheduledTask globalTimer(@NotNull Runnable task, long initialDelayTicks, long periodTicks);

    @NotNull
    <T> CompletableFuture<T> globalFuture(@NotNull Callable<T> task);

    @NotNull
    ScheduledTask async(@NotNull Runnable task);

    @NotNull
    ScheduledTask asyncLater(@NotNull Runnable task, long delayTicks);

    @NotNull
    ScheduledTask asyncTimer(@NotNull Runnable task, long initialDelayTicks, long periodTicks);

    void cancelPluginTasks();

    @NotNull
    <T> CompletableFuture<T> asyncFuture(@NotNull Callable<T> task);
}
