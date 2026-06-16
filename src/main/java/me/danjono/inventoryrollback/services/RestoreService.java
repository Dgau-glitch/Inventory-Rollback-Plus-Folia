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
import me.danjono.inventoryrollback.inventory.SaveInventory;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates restore transactions so storage IO and player-owned mutations run in distinct Folia stages.
 */
public class RestoreService {

    private static final ConcurrentHashMap<UUID, PendingRestore> PENDING_RESTORES = new ConcurrentHashMap<>();
    private static final String PENDING_RESTORE_FOLDER = "pending-restores";

    private final InventoryRollbackPlus main;

    public RestoreService(@NotNull InventoryRollbackPlus main) {
        this.main = main;
    }

    public void restoreMainInventory(@NotNull Player staff, @NotNull OfflinePlayer offlinePlayer, @NotNull LogType logType, @NotNull Long timestamp) {
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = offlinePlayer.getUniqueId();
        String targetName = getDisplayName(offlinePlayer, targetUuid);

        Player target = offlinePlayer.isOnline() ? (Player) offlinePlayer : null;
        SchedulerUtils.runTaskAsynchronously(() -> {
            PlayerData data = loadBackupData(targetUuid, logType, timestamp);
            ItemStack[] inventory = data.getMainInventory();
            ItemStack[] armour = data.getArmour();

            if (target == null) {
                PendingRestore pending = pendingFor(targetUuid);
                pending.mainInventory = copyItemArray(inventory);
                pending.armour = copyItemArray(armour);
                savePendingRestore(targetUuid, pending);
                PlayerScheduler.sendMessage(staff, MessageData.getPluginPrefix() + MessageData.getMainInventoryRestored(targetName));
                return;
            }

            PlayerScheduler.call(target, () -> {
                applyMainInventory(target, inventory, armour);
                if (SoundData.isInventoryRestoreEnabled()) {
                    target.playSound(target.getLocation(), SoundData.getInventoryRestored(), 1, 1);
                }
                target.sendMessage(MessageData.getPluginPrefix() + MessageData.getMainInventoryRestoredPlayer(staffName));
                return null;
            }).whenComplete((ignored, ex) -> notifyStaff(staff, staffUuid, targetUuid, MessageData.getMainInventoryRestored(targetName), ex));
        });
    }

    public void restoreEnderChest(@NotNull Player staff, @NotNull OfflinePlayer offlinePlayer, @NotNull LogType logType, @NotNull Long timestamp) {
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = offlinePlayer.getUniqueId();
        String targetName = getDisplayName(offlinePlayer, targetUuid);

        Player target = offlinePlayer.isOnline() ? (Player) offlinePlayer : null;
        SchedulerUtils.runTaskAsynchronously(() -> {
            PlayerData data = loadBackupData(targetUuid, logType, timestamp);
            ItemStack[] enderChest = data.getEnderChest() != null ? data.getEnderChest() : new ItemStack[0];

            if (target == null) {
                PendingRestore pending = pendingFor(targetUuid);
                pending.enderChest = copyItemArray(enderChest);
                savePendingRestore(targetUuid, pending);
                PlayerScheduler.sendMessage(staff, MessageData.getPluginPrefix() + MessageData.getEnderChestRestored(targetName));
                return;
            }

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
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = offlinePlayer.getUniqueId();
        String targetName = getDisplayName(offlinePlayer, targetUuid);

        Player target = offlinePlayer.isOnline() ? (Player) offlinePlayer : null;
        if (target == null) {
            SchedulerUtils.runTaskAsynchronously(() -> {
                PendingRestore pending = pendingFor(targetUuid);
                pending.health = health;
                savePendingRestore(targetUuid, pending);
                PlayerScheduler.sendMessage(staff, MessageData.getPluginPrefix() + MessageData.getHealthRestored(targetName));
            });
            return;
        }

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
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = offlinePlayer.getUniqueId();
        String targetName = getDisplayName(offlinePlayer, targetUuid);

        Player target = offlinePlayer.isOnline() ? (Player) offlinePlayer : null;
        if (target == null) {
            SchedulerUtils.runTaskAsynchronously(() -> {
                PendingRestore pending = pendingFor(targetUuid);
                pending.hunger = hunger;
                pending.saturation = saturation;
                savePendingRestore(targetUuid, pending);
                PlayerScheduler.sendMessage(staff, MessageData.getPluginPrefix() + MessageData.getHungerRestored(targetName));
            });
            return;
        }

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
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();
        UUID targetUuid = offlinePlayer.getUniqueId();
        String targetName = getDisplayName(offlinePlayer, targetUuid);
        int level = (int) RestoreInventory.getLevel(xp);

        Player target = offlinePlayer.isOnline() ? (Player) offlinePlayer : null;
        if (target == null) {
            SchedulerUtils.runTaskAsynchronously(() -> {
                PendingRestore pending = pendingFor(targetUuid);
                pending.xp = xp;
                savePendingRestore(targetUuid, pending);
                PlayerScheduler.sendMessage(staff, MessageData.getPluginPrefix() + MessageData.getExperienceRestored(targetName, level));
            });
            return;
        }

        PlayerScheduler.call(target, () -> {
            RestoreInventory.setTotalExperience(target, xp);
            if (SoundData.isExperienceRestoredEnabled()) {
                target.playSound(target.getLocation(), SoundData.getExperienceSound(), 1, 1);
            }
            target.sendMessage(MessageData.getPluginPrefix() + MessageData.getExperienceRestoredPlayer(staffName, level));
            return null;
        }).whenComplete((ignored, ex) -> notifyStaff(staff, staffUuid, targetUuid, MessageData.getExperienceRestored(targetName, level), ex));
    }

    public void applyPendingRestore(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        PendingRestore cached = PENDING_RESTORES.remove(uuid);
        if (cached != null) {
            applyPendingRestore(player, uuid, cached);
            return;
        }

        SchedulerUtils.runTaskAsynchronously(() -> {
            PendingRestore loaded = loadPendingRestore(uuid);
            if (loaded == null || loaded.isEmpty()) return;
            PENDING_RESTORES.remove(uuid);
            applyPendingRestore(player, uuid, loaded);
        });
    }

    private void applyPendingRestore(@NotNull Player player, @NotNull UUID uuid, @NotNull PendingRestore pending) {
        PlayerScheduler.call(player, () -> {
            if (pending.mainInventory != null) {
                applyMainInventory(player, pending.mainInventory, pending.armour);
            }
            if (pending.enderChest != null) {
                player.getEnderChest().setContents(pending.enderChest);
            }
            if (pending.health != null) {
                player.setHealth(Math.min(player.getMaxHealth(), pending.health));
            }
            if (pending.hunger != null) {
                player.setFoodLevel(pending.hunger);
            }
            if (pending.saturation != null) {
                player.setSaturation(pending.saturation);
            }
            if (pending.xp != null) {
                RestoreInventory.setTotalExperience(player, pending.xp);
            }
            return null;
        }).whenComplete((ignored, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                PENDING_RESTORES.put(uuid, pending);
                return;
            }
            SchedulerUtils.runTaskAsynchronously(() -> deletePendingRestore(uuid));
        });
    }

    private void applyMainInventory(@NotNull Player target, ItemStack[] inventory, ItemStack[] armour) {
        target.getInventory().setContents(inventory != null ? inventory : new ItemStack[0]);
        if (main.getVersion().lessOrEqThan(BukkitVersion.v1_8_R3)) {
            target.getInventory().setArmorContents(armour != null ? armour : new ItemStack[0]);
        }
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

    private PlayerData loadBackupData(@NotNull UUID uuid, @NotNull LogType logType, @NotNull Long timestamp) {
        PlayerData data = new PlayerData(uuid, logType, timestamp);
        data.loadAllBackupData();
        return data;
    }

    private PendingRestore pendingFor(@NotNull UUID uuid) {
        PendingRestore pending = PENDING_RESTORES.get(uuid);
        if (pending == null) {
            pending = loadPendingRestore(uuid);
            if (pending == null) pending = new PendingRestore();
            PENDING_RESTORES.put(uuid, pending);
        }
        return pending;
    }

    private PendingRestore loadPendingRestore(@NotNull UUID uuid) {
        File file = pendingFile(uuid);
        if (!file.exists()) return null;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        PendingRestore pending = new PendingRestore();
        if (yaml.isSet("mainInventory")) pending.mainInventory = RestoreInventory.getInventoryItems(main.getVersion().name(), yaml.getString("mainInventory"));
        if (yaml.isSet("armour")) pending.armour = RestoreInventory.getInventoryItems(main.getVersion().name(), yaml.getString("armour"));
        if (yaml.isSet("enderChest")) pending.enderChest = RestoreInventory.getInventoryItems(main.getVersion().name(), yaml.getString("enderChest"));
        if (yaml.isSet("health")) pending.health = yaml.getDouble("health");
        if (yaml.isSet("hunger")) pending.hunger = yaml.getInt("hunger");
        if (yaml.isSet("saturation")) pending.saturation = (float) yaml.getDouble("saturation");
        if (yaml.isSet("xp")) pending.xp = (float) yaml.getDouble("xp");
        return pending;
    }

    private void savePendingRestore(@NotNull UUID uuid, @NotNull PendingRestore pending) {
        File file = pendingFile(uuid);
        File folder = file.getParentFile();
        if (!folder.exists() && !folder.mkdirs()) {
            main.getLogger().warning("Could not create pending restore folder: " + folder.getAbsolutePath());
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        if (pending.mainInventory != null) yaml.set("mainInventory", SaveInventory.toBase64(pending.mainInventory));
        if (pending.armour != null) yaml.set("armour", SaveInventory.toBase64(pending.armour));
        if (pending.enderChest != null) yaml.set("enderChest", SaveInventory.toBase64(pending.enderChest));
        if (pending.health != null) yaml.set("health", pending.health);
        if (pending.hunger != null) yaml.set("hunger", pending.hunger);
        if (pending.saturation != null) yaml.set("saturation", pending.saturation);
        if (pending.xp != null) yaml.set("xp", pending.xp);

        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deletePendingRestore(@NotNull UUID uuid) {
        File file = pendingFile(uuid);
        if (file.exists() && !file.delete()) {
            main.getLogger().warning("Could not delete pending restore file: " + file.getAbsolutePath());
        }
    }

    private File pendingFile(@NotNull UUID uuid) {
        return new File(new File(main.getDataFolder(), PENDING_RESTORE_FOLDER), uuid + ".yml");
    }

    private String getDisplayName(@NotNull OfflinePlayer offlinePlayer, @NotNull UUID uuid) {
        String name = offlinePlayer.getName();
        return name != null ? name : uuid.toString();
    }

    private ItemStack[] copyItemArray(ItemStack[] contents) {
        if (contents == null) return null;
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) copy[i] = contents[i].clone();
        }
        return copy;
    }

    private static class PendingRestore {
        private ItemStack[] mainInventory;
        private ItemStack[] armour;
        private ItemStack[] enderChest;
        private Double health;
        private Integer hunger;
        private Float saturation;
        private Float xp;

        private boolean isEmpty() {
            return mainInventory == null && armour == null && enderChest == null
                    && health == null && hunger == null && saturation == null && xp == null;
        }
    }
}
