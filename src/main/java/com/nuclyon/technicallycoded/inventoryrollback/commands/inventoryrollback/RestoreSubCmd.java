package com.nuclyon.technicallycoded.inventoryrollback.commands.inventoryrollback;

import com.nuclyon.technicallycoded.inventoryrollback.InventoryRollbackPlus;
import com.nuclyon.technicallycoded.inventoryrollback.commands.IRPCommand;
import com.nuclyon.technicallycoded.inventoryrollback.folia.PlayerScheduler;
import com.nuclyon.technicallycoded.inventoryrollback.folia.SchedulerUtils;
import me.danjono.inventoryrollback.InventoryRollback;
import me.danjono.inventoryrollback.config.ConfigData;
import me.danjono.inventoryrollback.config.MessageData;
import me.danjono.inventoryrollback.gui.menu.MainMenu;
import me.danjono.inventoryrollback.gui.menu.PlayerMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RestoreSubCmd extends IRPCommand {

    public RestoreSubCmd(InventoryRollbackPlus mainIn) {
        super(mainIn);
    }

    @Override
    public void onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("inventoryrollbackplus.viewbackups")) {
                if (!ConfigData.isEnabled()) {
                    sender.sendMessage(MessageData.getPluginPrefix() + MessageData.getPluginDisabled());
                    return;
                }
                Player staff = (Player) sender;
                openBackupMenu(sender, staff, args);
            } else {
                sender.sendMessage(MessageData.getPluginPrefix() + MessageData.getNoPermission());
            }
        } else {
            sender.sendMessage(MessageData.getPluginPrefix() + MessageData.getPlayerOnlyError());
        }
    }

    private void openBackupMenu(CommandSender sender, Player staff, String[] args) {
        if (args.length <= 0 || args.length == 1) {
            try {
                openMainMenu(staff);
            } catch (NullPointerException ignored) {}
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageData.getPluginPrefix() + MessageData.getError());
            return;
        }

        String playerInput = args[1];
        UUID uuid = parseUuid(playerInput);
        if (uuid != null) {
            try {
                openPlayerMenu(staff, Bukkit.getOfflinePlayer(uuid));
            } catch (NullPointerException ignored) {}
            return;
        }

        OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayerIfCached(playerInput);
        if (cachedPlayer != null) {
            openPlayerMenu(staff, cachedPlayer);
            return;
        }

        SchedulerUtils.runTaskAsynchronously(() -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer rollbackPlayer = Bukkit.getOfflinePlayer(playerInput);
            PlayerScheduler.run(staff, () -> openPlayerMenu(staff, rollbackPlayer));
        });
    }

    private UUID parseUuid(String input) {
        String uuidStr = input;
        if (uuidStr.length() != 36 && uuidStr.length() != 32) return null;

        if (uuidStr.length() == 32) {
            uuidStr = uuidStr.substring(0, 8) + "-"
                    + uuidStr.substring(8, 12) + "-"
                    + uuidStr.substring(12, 16) + "-"
                    + uuidStr.substring(16, 20) + "-"
                    + uuidStr.substring(20);
        }

        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void openMainMenu(Player staff) {
        MainMenu menu = new MainMenu(staff, 1);

        PlayerScheduler.openInventory(staff, menu.getInventory());
        PlayerScheduler.run(staff, menu::getMainMenu);
    }

    private void openPlayerMenu(Player staff, OfflinePlayer offlinePlayer) {
        PlayerMenu menu = new PlayerMenu(staff, offlinePlayer);

        PlayerScheduler.openInventory(staff, menu.getInventory());
        menu.populatePlayerMenuAsync();
    }

}
