package com.nuclyon.technicallycoded.inventoryrollback.commands;

import com.nuclyon.technicallycoded.inventoryrollback.InventoryRollbackPlus;
import com.nuclyon.technicallycoded.inventoryrollback.commands.inventoryrollback.*;
import me.danjono.inventoryrollback.config.MessageData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_PREFIX = "inventoryrollbackplus.";

    private final HelpSubCmd helpCommand;
    private final Map<String, CommandMetadata> commandRegistry = new LinkedHashMap<>();

    public Commands(InventoryRollbackPlus mainIn) {
        this.helpCommand = new HelpSubCmd(mainIn);
        register(new CommandMetadata("restore", PERMISSION_PREFIX + "viewbackups", new RestoreSubCmd(mainIn), this::restoreCompletion));
        register(new CommandMetadata("forcebackup", PERMISSION_PREFIX + "forcebackup", new ForceBackupSubCmd(mainIn), this::forceBackupCompletion, "forcesave"));
        register(new CommandMetadata("enable", PERMISSION_PREFIX + "enable", new EnableSubCmd(mainIn), this::emptyCompletion));
        register(new CommandMetadata("disable", PERMISSION_PREFIX + "disable", new DisableSubCmd(mainIn), this::emptyCompletion));
        register(new CommandMetadata("reload", PERMISSION_PREFIX + "reload", new ReloadSubCmd(mainIn), this::emptyCompletion));
        register(new CommandMetadata("version", PERMISSION_PREFIX + "version", new VersionSubCmd(mainIn), this::emptyCompletion));
        register(new CommandMetadata("import", PERMISSION_PREFIX + "import", new ImportSubCmd(mainIn), this::importCompletion));
        register(new CommandMetadata("help", PERMISSION_PREFIX + "help", helpCommand, this::emptyCompletion));
    }

    private void register(CommandMetadata metadata) {
        commandRegistry.put(metadata.name, metadata);
        for (String alias : metadata.aliases) {
            commandRegistry.put(alias, metadata);
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("inventoryrollback") ||
                label.equalsIgnoreCase("ir") ||
                label.equalsIgnoreCase("irp") ||
                label.equalsIgnoreCase("inventoryrollbackplus")
        ) {
            if (args.length == 0) {
                helpCommand.sendHelp(sender);
                return true;
            }
            CommandMetadata metadata = commandRegistry.get(args[0].toLowerCase(Locale.ROOT));
            if (metadata != null) {
                metadata.command.onCommand(sender, cmd, label, args);
                return true;
            }
            sender.sendMessage(MessageData.getPluginPrefix() + MessageData.getError());
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String name, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Map.Entry<String, CommandMetadata> entry : commandRegistry.entrySet()) {
                if (!entry.getKey().equals(entry.getValue().name)) continue;
                CommandMetadata metadata = entry.getValue();
                if (!sender.hasPermission(metadata.permission)) continue;
                if (metadata.name.startsWith(prefix)) suggestions.add(metadata.name);
            }
            return suggestions;
        }

        CommandMetadata metadata = commandRegistry.get(args[0].toLowerCase(Locale.ROOT));
        if (metadata == null || !sender.hasPermission(metadata.permission)) {
            return Collections.emptyList();
        }

        return metadata.completer.apply(sender, args);
    }

    private List<String> restoreCompletion(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return onlinePlayerNames(args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> forceBackupCompletion(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return startsWith(Arrays.asList("all", "player"), args[1]);
        }
        if (args.length == 3 && "player".equalsIgnoreCase(args[1])) {
            return onlinePlayerNames(args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> importCompletion(CommandSender sender, String[] args) {
        if (args.length == 2 && (ImportSubCmd.shouldShowConfirmOption() || args[1].toLowerCase(Locale.ROOT).startsWith("c"))) {
            return startsWith(Collections.singletonList("confirm"), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> emptyCompletion(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    private List<String> onlinePlayerNames(String rawPrefix) {
        List<String> playerNames = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }
        return startsWith(playerNames, rawPrefix);
    }

    private List<String> startsWith(List<String> options, String rawPrefix) {
        String prefix = rawPrefix.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) suggestions.add(option);
        }
        return suggestions;
    }

    private static class CommandMetadata {
        private final String name;
        private final String permission;
        private final IRPCommand command;
        private final BiFunction<CommandSender, String[], List<String>> completer;
        private final List<String> aliases;

        private CommandMetadata(String name, String permission, IRPCommand command, BiFunction<CommandSender, String[], List<String>> completer, String... aliases) {
            this.name = name;
            this.permission = permission;
            this.command = command;
            this.completer = completer;
            this.aliases = Arrays.asList(aliases);
        }
    }
}
