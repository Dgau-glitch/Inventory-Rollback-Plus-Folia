package com.nuclyon.technicallycoded.inventoryrollback.commands;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandsTabCompletionTest {

    @Test
    void rootCompletionsAreFilteredBeforeSuggestionsAreBuilt() {
        Commands commands = new Commands(null);

        assertTrue(commands.onTabComplete(senderWith(), null, "irp", new String[]{""}).isEmpty());
        assertEquals(Collections.singletonList("restore"),
                commands.onTabComplete(senderWith("inventoryrollbackplus.viewbackups"), null, "irp", new String[]{"r"}));
        assertEquals(Collections.singletonList("forcebackup"),
                commands.onTabComplete(senderWith("inventoryrollbackplus.forcebackup"), null, "irp", new String[]{"force"}));
    }

    @Test
    void unauthorizedSubcommandBranchesReturnEmptyLists() {
        Commands commands = new Commands(null);

        assertTrue(commands.onTabComplete(senderWith(), null, "irp", new String[]{"forcebackup", ""}).isEmpty());
        assertTrue(commands.onTabComplete(senderWith("inventoryrollbackplus.forcebackup"), null, "irp", new String[]{"restore", "Steve"}).isEmpty());
        assertTrue(commands.onTabComplete(senderWith("inventoryrollbackplus.viewbackups"), null, "irp", new String[]{"unknown", ""}).isEmpty());
    }

    @Test
    void permittedSubcommandsProvideContextualCompletions() {
        Commands commands = new Commands(null);

        assertEquals(Arrays.asList("all", "player"),
                commands.onTabComplete(senderWith("inventoryrollbackplus.forcebackup"), null, "irp", new String[]{"forcebackup", ""}));
        assertEquals(Collections.singletonList("player"),
                commands.onTabComplete(senderWith("inventoryrollbackplus.forcebackup"), null, "irp", new String[]{"forcesave", "p"}));
        assertEquals(Collections.singletonList("confirm"),
                commands.onTabComplete(senderWith("inventoryrollbackplus.import"), null, "irp", new String[]{"import", "c"}));
    }

    private static CommandSender senderWith(String... permissions) {
        Set<String> granted = new HashSet<>(Arrays.asList(permissions));
        return (CommandSender) Proxy.newProxyInstance(
                CommandsTabCompletionTest.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("hasPermission".equals(methodName) && args != null && args.length == 1 && args[0] instanceof String) {
                        return granted.contains(args[0]);
                    }
                    if ("isPermissionSet".equals(methodName)) {
                        return false;
                    }
                    if ("isOp".equals(methodName)) {
                        return false;
                    }
                    if ("getName".equals(methodName)) {
                        return "unit-test";
                    }
                    if ("getEffectivePermissions".equals(methodName)) {
                        return Collections.emptySet();
                    }
                    if ("sendMessage".equals(methodName) || "recalculatePermissions".equals(methodName)) {
                        return null;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(Boolean.TYPE)) {
                        return false;
                    }
                    if (returnType.equals(Integer.TYPE) || returnType.equals(Long.TYPE)
                            || returnType.equals(Short.TYPE) || returnType.equals(Byte.TYPE)) {
                        return 0;
                    }
                    if (returnType.equals(Float.TYPE) || returnType.equals(Double.TYPE)) {
                        return 0.0;
                    }
                    return null;
                }
        );
    }
}
