package me.danjono.inventoryrollback.gui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.nuclyon.technicallycoded.inventoryrollback.folia.PlayerScheduler;
import com.nuclyon.technicallycoded.inventoryrollback.folia.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.danjono.inventoryrollback.config.MessageData;
import me.danjono.inventoryrollback.data.LogType;
import me.danjono.inventoryrollback.data.PlayerData;
import me.danjono.inventoryrollback.gui.Buttons;
import me.danjono.inventoryrollback.gui.InventoryName;

public class RollbackListMenu {

    private int pageNumber;

    private Player staff;
    private UUID playerUUID;
    private LogType logType;
    
    private Buttons buttons;
    private Inventory inventory;

    public RollbackListMenu(Player staff, OfflinePlayer player, LogType logType, int pageNumberIn) {
        this.staff = staff;
        this.playerUUID = player.getUniqueId();
        this.logType = logType;
        this.pageNumber = pageNumberIn;
        this.buttons = new Buttons(playerUUID);
        
        createInventory();
    }
    
    public void createInventory() {
        inventory = Bukkit.createInventory(staff, InventoryName.ROLLBACK_LIST.getSize(), InventoryName.ROLLBACK_LIST.getName());
        
        List<String> lore = new ArrayList<>();  
        if (pageNumber == 1) {
            ItemStack mainMenu = buttons.backButton(MessageData.getMainMenuButton(), logType, 0, null);                     
            inventory.setItem(InventoryName.ROLLBACK_LIST.getSize() - 8, mainMenu);
        }       

        if (pageNumber > 1) {
            lore.add("Page " + (pageNumber - 1));
            ItemStack previousPage = buttons.backButton(MessageData.getPreviousPageButton(), logType, pageNumber - 1, lore);

            inventory.setItem(InventoryName.ROLLBACK_LIST.getSize() - 8, previousPage);
            lore.clear();
        }
    }
    
    public Inventory getInventory() {
        return this.inventory;
    }

    public void populateBackupsAsync() {
        SchedulerUtils.runTaskAsynchronously(() -> {
            RollbackListData data = loadBackups();
            PlayerScheduler.run(staff, () -> applyBackups(data));
        });
    }

    public void showBackups() {
        applyBackups(loadBackups());
    }

    private RollbackListData loadBackups() {
        PlayerData playerData = new PlayerData(playerUUID, logType, null);

        int backups = playerData.getAmountOfBackups();
        int spaceRequired = InventoryName.ROLLBACK_LIST.getSize() - 9;
        int pagesRequired = (int) Math.ceil(backups / (double) spaceRequired);

        int resolvedPage = pageNumber;
        if (resolvedPage > pagesRequired) {
            resolvedPage = pagesRequired;
        } else if (resolvedPage <= 0) {
            resolvedPage = 1;
        }

        int backupsAlreadyPassed = spaceRequired * (resolvedPage - 1);
        int backupsOnCurrentPage = Math.min(backups, Math.min(spaceRequired, backups - backupsAlreadyPassed));
        List<Long> timeStamps = playerData.getSelectedPageTimestamps(resolvedPage);

        List<RollbackButtonData> buttons = new ArrayList<>();
        for (int i = 0; i < backupsOnCurrentPage; i++) {
            try {
                Long timestamp = timeStamps.get(i);
                PlayerData rowData = new PlayerData(playerUUID, logType, timestamp);
                rowData.getRollbackMenuData();

                String displayName = MessageData.getDeathTime(PlayerData.getTime(timestamp));
                List<String> lore = new ArrayList<>();

                String deathReason = rowData.getDeathReason();
                if (deathReason != null)
                    lore.add(MessageData.getDeathReason(deathReason));

                String world = rowData.getWorld();
                double x = rowData.getX();
                double y = rowData.getY();
                double z = rowData.getZ();
                String location = world + "," + x + "," + y + "," + z;

                lore.add(MessageData.getDeathLocationWorld(world));
                lore.add(MessageData.getDeathLocationX(x));
                lore.add(MessageData.getDeathLocationY(y));
                lore.add(MessageData.getDeathLocationZ(z));

                buttons.add(new RollbackButtonData(location, timestamp, displayName, lore));
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        return new RollbackListData(resolvedPage, pagesRequired, buttons);
    }

    private void applyBackups(RollbackListData data) {
        pageNumber = data.pageNumber;
        int position = 0;
        for (RollbackButtonData buttonData : data.buttons) {
            ItemStack item = buttons.createInventoryButton(
                    new ItemStack(Material.CHEST),
                    logType,
                    buttonData.location,
                    buttonData.timestamp,
                    buttonData.displayName,
                    buttonData.lore
            );
            inventory.setItem(position, item);
            position++;
        }

        List<String> lore = new ArrayList<>();
        if (pageNumber < data.pagesRequired) {
            lore.add("Page " + (pageNumber + 1));
            ItemStack nextPage = buttons.nextButton(MessageData.getNextPageButton(), logType, pageNumber + 1, lore);

            inventory.setItem(position + 7, nextPage);
            lore.clear();
        }
    }

    private static class RollbackListData {
        private final int pageNumber;
        private final int pagesRequired;
        private final List<RollbackButtonData> buttons;

        private RollbackListData(int pageNumber, int pagesRequired, List<RollbackButtonData> buttons) {
            this.pageNumber = pageNumber;
            this.pagesRequired = pagesRequired;
            this.buttons = buttons;
        }
    }

    private static class RollbackButtonData {
        private final String location;
        private final Long timestamp;
        private final String displayName;
        private final List<String> lore;

        private RollbackButtonData(String location, Long timestamp, String displayName, List<String> lore) {
            this.location = location;
            this.timestamp = timestamp;
            this.displayName = displayName;
            this.lore = lore;
        }
    }

}
