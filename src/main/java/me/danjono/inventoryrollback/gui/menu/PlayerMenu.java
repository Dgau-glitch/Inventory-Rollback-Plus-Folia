package me.danjono.inventoryrollback.gui.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.nuclyon.technicallycoded.inventoryrollback.folia.PlayerScheduler;
import com.nuclyon.technicallycoded.inventoryrollback.folia.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.danjono.inventoryrollback.config.MessageData;
import me.danjono.inventoryrollback.data.LogType;
import me.danjono.inventoryrollback.data.PlayerData;
import me.danjono.inventoryrollback.gui.Buttons;
import me.danjono.inventoryrollback.gui.InventoryName;

public class PlayerMenu {

    private Player staff;
    private OfflinePlayer offlinePlayer;

    private Buttons buttons;
    private Inventory inventory;

    public PlayerMenu(Player staff, OfflinePlayer player) {
        this.staff = staff;
        this.offlinePlayer = player;
        this.buttons = new Buttons(player.getUniqueId());

        createInventory();
    }

    public void createInventory() {
        inventory = Bukkit.createInventory(staff, InventoryName.PLAYER_MENU.getSize(), InventoryName.PLAYER_MENU.getName());
        
        inventory.setItem(2, buttons.createDeathLogButton(LogType.DEATH, null));
        inventory.setItem(3, buttons.createJoinLogButton(LogType.JOIN, null));
        inventory.setItem(4, buttons.createQuitLogButton(LogType.QUIT, null));
        inventory.setItem(5, buttons.createWorldChangeLogButton(LogType.WORLD_CHANGE, null));
        inventory.setItem(6, buttons.createForceSaveLogButton(LogType.FORCE, null));
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void populatePlayerMenuAsync() {
        SchedulerUtils.runTaskAsynchronously(() -> {
            PlayerMenuData data = loadPlayerMenuData();
            PlayerScheduler.run(staff, () -> applyPlayerMenuData(data));
        });
    }

    public void getPlayerMenu() {
        applyPlayerMenuData(loadPlayerMenuData());
    }

    private PlayerMenuData loadPlayerMenuData() {
        List<String> lore = new ArrayList<>();

        if (offlinePlayer.isOnline()) {
            lore.add(ChatColor.GREEN + "Online now");
        } else if (!offlinePlayer.hasPlayedBefore()) {
            lore.add(ChatColor.RED + "Never played on this server");
        } else {
            lore.add(ChatColor.RED + "Offline");

            String dateTime = "Unknown";
            if (offlinePlayer.getLastPlayed() != 0)
                dateTime = PlayerData.getTime(offlinePlayer.getLastPlayed());
            lore.add(ChatColor.RED + "Last online: " + dateTime);
        }

        UUID uuid = offlinePlayer.getUniqueId();
        int deaths = new PlayerData(uuid, LogType.DEATH, null).getAmountOfBackups();
        int joins = new PlayerData(uuid, LogType.JOIN, null).getAmountOfBackups();
        int quits = new PlayerData(uuid, LogType.QUIT, null).getAmountOfBackups();
        int worldChanges = new PlayerData(uuid, LogType.WORLD_CHANGE, null).getAmountOfBackups();
        int forceSaves = new PlayerData(uuid, LogType.FORCE, null).getAmountOfBackups();

        return new PlayerMenuData(lore, deaths, joins, quits, worldChanges, forceSaves);
    }

    private void applyPlayerMenuData(PlayerMenuData data) {
        inventory.setItem(0, buttons.playerHead(data.lore, true));

        if (!data.hasBackups()) {
            staff.sendMessage(MessageData.getPluginPrefix() + MessageData.getNoBackupError(offlinePlayer.getName()));
        }

        String backupsAvailable = " backup(s) available";

        inventory.setItem(2, buttons.createDeathLogButton(LogType.DEATH, Arrays.asList(data.deaths + backupsAvailable)));
        inventory.setItem(3, buttons.createJoinLogButton(LogType.JOIN, Arrays.asList(data.joins + backupsAvailable)));
        inventory.setItem(4, buttons.createQuitLogButton(LogType.QUIT, Arrays.asList(data.quits + backupsAvailable)));
        inventory.setItem(5, buttons.createWorldChangeLogButton(LogType.WORLD_CHANGE, Arrays.asList(data.worldChanges + backupsAvailable)));
        inventory.setItem(6, buttons.createForceSaveLogButton(LogType.FORCE, Arrays.asList(data.forceSaves + backupsAvailable)));
    }

    private static class PlayerMenuData {
        private final List<String> lore;
        private final int deaths;
        private final int joins;
        private final int quits;
        private final int worldChanges;
        private final int forceSaves;

        private PlayerMenuData(List<String> lore, int deaths, int joins, int quits, int worldChanges, int forceSaves) {
            this.lore = lore;
            this.deaths = deaths;
            this.joins = joins;
            this.quits = quits;
            this.worldChanges = worldChanges;
            this.forceSaves = forceSaves;
        }

        private boolean hasBackups() {
            return deaths > 0 || joins > 0 || quits > 0 || worldChanges > 0 || forceSaves > 0;
        }
    }

}
