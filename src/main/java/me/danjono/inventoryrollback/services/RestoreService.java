package me.danjono.inventoryrollback.services;

import com.nuclyon.technicallycoded.inventoryrollback.InventoryRollbackPlus;
import com.nuclyon.technicallycoded.inventoryrollback.folia.PlayerScheduler;
import com.nuclyon.technicallycoded.inventoryrollback.folia.SchedulerUtils;
import com.tcoded.lightlibs.bukkitversion.BukkitVersion;
import me.danjono.inventoryrollback.config.MessageData;
import me.danjono.inventoryrollback.config.SoundData;
import me.danjono.inventoryrollback.data.LogType;
import me.danjono.inventoryrollback.data.PlayerData;
import me.danjono.inventoryrollback.inventory.RestoreInventory;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Coordinates restore transactions so storage IO and player-owned mutations run in distinct Folia stages.
 */
public class RestoreService {

    private final InventoryRollbackPlus main;

    public RestoreService(@NotNull InventoryRollbackPlus main) {
        this.main = main;
    }

    public void restoreMainInventory(@NotNull Player staff, @NotNull OfflinePlayer offlinePlayer, @NotNull LogType logType, @NotNull Long timestamp) {
        Player target = getOnlineTarget(staff, offlinePlayer, MessageData.getMainInventoryNotOnline(offlinePlayer.getName()));
        if (target == null) return;

        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = target.getUniqueId();
        String targetName = offlinePlayer.getName();

        SchedulerUtils.runTaskAsynchronously(() -> {
            PlayerData data = loadBackupData(offlinePlayer, logType, timestamp);
            ItemStack[] inventory = data.getMainInventory();
            ItemStack[] armour = data.getArmour();

            PlayerScheduler.call(target, () -> {
                target.getInventory().setContents(inventory);
                if (main.getVersion().lessOrEqThan(BukkitVersion.v1_8_R3)) {
                    target.getInventory().setArmorContents(armour);
                }
                if (SoundData.isInventoryRestoreEnabled()) {
                    target.playSound(target.getLocation(), SoundData.getInventoryRestored(), 1, 1);
                }
                target.sendMessage(MessageData.getPluginPrefix() + MessageData.getMainInventoryRestoredPlayer(staffName));
                return null;
            }).whenComplete((ignored, ex) -> notifyStaff(staff, staffUuid, targetUuid, MessageData.getMainInventoryRestored(targetName), ex));
        });
    }

    public void restoreEnderChest(@NotNull Player staff, @NotNull OfflinePlayer offlinePlayer, @NotNull LogType logType, @NotNull Long timestamp) {
        Player target = getOnlineTarget(staff, offlinePlayer, MessageData.getEnderChestNotOnline(offlinePlayer.getName()));
        if (target == null) return;

        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = target.getUniqueId();
        String targetName = offlinePlayer.getName();

        SchedulerUtils.runTaskAsynchronously(() -> {
            PlayerData data = loadBackupData(offlinePlayer, logType, timestamp);
            ItemStack[] enderChest = data.getEnderChest() != null ? data.getEnderChest() : new ItemStack[0];

            PlayerScheduler.call(target, () -> {
                target.getEnderChest().setContents(enderChest);
                if (SoundData.isInventoryRestoreEnabled()) {
                    target.playSound(target.getLocation(), SoundData.getInventoryRestored(), 1, 1);
                }
                target.sendMessage(MessageData.getPluginPrefix() + MessageData.getEnderChestRestoredPlayer(staffName));
                return null;
            }).whenComplete((ignored, ex) -> notifyStaff(staff, staffUuid, targetUuid, MessageData.getEnderChestRestored(targetName), ex));
        });
    }

    public void restoreHealth(@NotNull Player staff, @NotNull OfflinePlayer offlinePlayer, double health) {
        Player target = getOnlineTarget(staff, offlinePlayer, MessageData.getHealthNotOnline(offlinePlayer.getName()));
        if (target == null) return;

        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = target.getUniqueId();
        String targetName = offlinePlayer.getName();

        PlayerScheduler.call(target, () -> {
            target.setHealth(health);
            if (SoundData.isFoodRestoredEnabled()) {
                target.playSound(target.getLocation(), SoundData.getFoodRestored(), 1, 1);
            }
            target.sendMessage(MessageData.getPluginPrefix() + MessageData.getHealthRestoredPlayer(staffName));
            return null;
        }).whenComplete((ignored, ex) -> notifyStaff(staff, staffUuid, targetUuid, MessageData.getHealthRestored(targetName), ex));
    }

    public void restoreHunger(@NotNull Player staff, @NotNull OfflinePlayer offlinePlayer, int hunger, float saturation) {
        Player target = getOnlineTarget(staff, offlinePlayer, MessageData.getHungerNotOnline(offlinePlayer.getName()));
        if (target == null) return;

        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = target.getUniqueId();
        String targetName = offlinePlayer.getName();

        PlayerScheduler.call(target, () -> {
            target.setFoodLevel(hunger);
            target.setSaturation(saturation);
            if (SoundData.isHungerRestoredEnabled()) {
                target.playSound(target.getLocation(), SoundData.getHungerRestored(), 1, 1);
            }
            target.sendMessage(MessageData.getPluginPrefix() + MessageData.getHungerRestoredPlayer(staffName));
            return null;
        }).whenComplete((ignored, ex) -> notifyStaff(staff, staffUuid, targetUuid, MessageData.getHungerRestored(targetName), ex));
    }

    public void restoreExperience(@NotNull Player staff, @NotNull OfflinePlayer offlinePlayer, float xp) {
        Player target = getOnlineTarget(staff, offlinePlayer, MessageData.getExperienceNotOnlinePlayer(offlinePlayer.getName()));
        if (target == null) return;

        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = target.getUniqueId();
        String targetName = offlinePlayer.getName();
        int level = (int) RestoreInventory.getLevel(xp);

        PlayerScheduler.call(target, () -> {
            RestoreInventory.setTotalExperience(target, xp);
            if (SoundData.isExperienceRestoredEnabled()) {
                target.playSound(target.getLocation(), SoundData.getExperienceSound(), 1, 1);
            }
            target.sendMessage(MessageData.getPluginPrefix() + MessageData.getExperienceRestoredPlayer(staffName, level));
            return null;
        }).whenComplete((ignored, ex) -> notifyStaff(staff, staffUuid, targetUuid, MessageData.getExperienceRestored(targetName, level), ex));
    }

    private void notifyStaff(@NotNull Player staff, @NotNull UUID staffUuid, @NotNull UUID targetUuid, @NotNull String successMessage, Throwable ex) {
        if (ex != null) {
            ex.printStackTrace();
            PlayerScheduler.sendMessage(staff, MessageData.getPluginPrefix() + MessageData.getError());
            return;
        }
        if (!staffUuid.equals(targetUuid)) {
            PlayerScheduler.sendMessage(staff, MessageData.getPluginPrefix() + successMessage);
        }
    }

    private PlayerData loadBackupData(@NotNull OfflinePlayer offlinePlayer, @NotNull LogType logType, @NotNull Long timestamp) {
        PlayerData data = new PlayerData(offlinePlayer, logType, timestamp);
        data.loadAllBackupData();
        return data;
    }

    private Player getOnlineTarget(@NotNull Player staff, @NotNull OfflinePlayer offlinePlayer, @NotNull String notOnlineMessage) {
        if (!offlinePlayer.isOnline()) {
            PlayerScheduler.sendMessage(staff, MessageData.getPluginPrefix() + notOnlineMessage);
            return null;
        }
        return (Player) offlinePlayer;
    }
}
