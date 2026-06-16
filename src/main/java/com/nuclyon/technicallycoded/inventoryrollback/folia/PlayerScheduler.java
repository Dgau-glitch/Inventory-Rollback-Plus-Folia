package com.nuclyon.technicallycoded.inventoryrollback.folia;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Player-owned scheduler helpers for GUI, inventory, messages, and other live player state.
 */
public final class PlayerScheduler {

    private PlayerScheduler() {
    }

    public static void run(@NotNull Player player, @NotNull Runnable task) {
        SchedulerUtils.runEntity(player, task, () -> { });
    }

    public static void runLater(@NotNull Player player, @NotNull Runnable task, long delayTicks) {
        SchedulerUtils.runEntityLater(player, task, () -> { }, delayTicks);
    }

    public static <T> CompletableFuture<T> call(@NotNull Player player, @NotNull Callable<T> task) {
        return SchedulerUtils.callEntityMethod(player, task);
    }

    public static void openInventory(@NotNull Player player, @NotNull Inventory inventory) {
        run(player, () -> player.openInventory(inventory));
    }

    public static void closeInventory(@NotNull Player player) {
        run(player, player::closeInventory);
    }

    public static void sendMessage(@NotNull Player player, @NotNull String message) {
        run(player, () -> player.sendMessage(message));
    }
}
