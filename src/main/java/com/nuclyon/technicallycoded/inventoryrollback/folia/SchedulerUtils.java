package com.nuclyon.technicallycoded.inventoryrollback.folia;

import com.nuclyon.technicallycoded.inventoryrollback.InventoryRollbackPlus;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Compatibility adapter for legacy call sites.
 * <p>
 * New code should depend on {@link FoliaSchedulerService} and choose entity, region,
 * global, or async ownership explicitly instead of routing work through this class.
 */
public abstract class SchedulerUtils {

    private static FoliaSchedulerService service() {
        return new DirectFoliaSchedulerService(InventoryRollbackPlus.getInstance());
    }

    /**
     * Schedules a task on the owning region for the location, or on the global region when location is null.
     *
     * @param loc The location whose region should own the task, or null for the global region.
     * @param task The task to run.
     * @param delay The delay in ticks before the task runs.
     */
    public static void runTaskLater(@Nullable Location loc, @NotNull Runnable task, long delay) {
        if (loc == null) {
            service().globalLater(task, delay);
            return;
        }
        service().regionLater(loc, task, delay);
    }

    /**
     * Legacy async-delayed adapter. The location parameter is ignored because async work has no region owner.
     *
     * @param loc Ignored legacy parameter.
     * @param task The task to run.
     * @param delay The delay in ticks before the task runs.
     */
    @Deprecated
    public static void runTaskLaterAsynchronously(@Nullable Location loc, @NotNull Runnable task, long delay) {
        service().asyncLater(task, delay);
    }

    /**
     * Schedules a repeating task on the owning region for the location, or on the global region when location is null.
     *
     * @param loc The location whose region should own the task, or null for the global region.
     * @param runnable The runnable to run.
     * @param delay The delay in ticks before the task runs.
     * @param period The period in ticks between subsequent runs of the task.
     */
    public static void runTaskTimer(@Nullable Location loc, @NotNull FoliaRunnable runnable, long delay, long period) {
        if (loc == null) {
            runnable.setScheduledTask(service().globalTimer(runnable, delay, period));
            return;
        }
        runnable.setScheduledTask(service().regionTimer(loc, runnable, delay, period));
    }

    /**
     * Legacy async repeating adapter.
     *
     * @param runnable The runnable to run.
     * @param delay The delay in ticks before the task runs.
     * @param period The period in ticks between subsequent runs of the task.
     */
    @Deprecated
    public static void runTaskTimerAsynchronously(@NotNull FoliaRunnable runnable, long delay, long period) {
        runnable.setScheduledTask(service().asyncTimer(runnable, delay, period));
    }

    /**
     * Runs work asynchronously from the server tick process.
     *
     * @param task The task to run.
     */
    public static void runTaskAsynchronously(@NotNull Runnable task) {
        service().async(task);
    }

    /**
     * Runs a task on the owning region for the location, or on the global region when location is null.
     *
     * @param loc The location whose region should own the task, or null for the global region.
     * @param task The task to run.
     */
    public static void runTask(@Nullable Location loc, @NotNull Runnable task) {
        if (loc == null) {
            service().global(task);
            return;
        }
        service().region(loc, task);
    }

    /**
     * Calls a task on the owning region for the location, or on the global region when location is null.
     *
     * @param loc The location whose region should own the task, or null for the global region.
     * @param task The task to call.
     * @return A future containing the task result.
     */
    public static <T> CompletableFuture<T> callSyncMethod(@Nullable Location loc, @NotNull Callable<T> task) {
        if (loc == null) {
            return service().globalFuture(task);
        }
        return service().regionFuture(loc, task);
    }
}
