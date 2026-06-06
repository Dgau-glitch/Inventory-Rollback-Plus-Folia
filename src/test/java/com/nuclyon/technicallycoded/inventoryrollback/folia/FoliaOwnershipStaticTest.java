package com.nuclyon.technicallycoded.inventoryrollback.folia;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FoliaOwnershipStaticTest {

    private static final Path PRODUCTION_SOURCES = Paths.get("src/main/java");
    private static final Pattern[] UNSAFE_SCHEDULER_PATTERNS = {
            Pattern.compile("Bukkit\\s*\\.\\s*getScheduler\\s*\\("),
            Pattern.compile("getServer\\s*\\(\\s*\\)\\s*\\.\\s*getScheduler\\s*\\("),
            Pattern.compile("import\\s+org\\.bukkit\\.scheduler\\.BukkitScheduler\\s*;"),
            Pattern.compile("Class\\s*\\.\\s*forName\\s*\\(\\s*\\\"io\\.papermc\\.paper\\.threadedregions\\.scheduler\\."),
            Pattern.compile("\\.\\s*getMethod\\s*\\(\\s*\\\"get(?:GlobalRegion|Region|Async|Entity)?Scheduler\\\"")
    };

    @Test
    void productionSourcesDoNotUseBukkitSchedulerOrReflectiveFoliaDiscovery() throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(PRODUCTION_SOURCES)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectUnsafeSchedulerPatterns(path, violations));
        }

        assertTrue(violations.isEmpty(), "Unsafe production scheduler access found:\n" + String.join("\n", violations));
    }


    @Test
    void pluginDisableDoesNotRegisterNewSchedulerTasks() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/nuclyon/technicallycoded/inventoryrollback/InventoryRollbackPlus.java")), StandardCharsets.UTF_8);
        String onDisableBody = source.substring(source.indexOf("public void onDisable()"));
        onDisableBody = onDisableBody.substring(0, onDisableBody.indexOf("public void setVersion"));

        assertTrue(!onDisableBody.contains("PlayerScheduler.run"),
                "onDisable must not register entity tasks because Folia has already disabled the plugin");
        assertTrue(!onDisableBody.contains("SchedulerUtils.runTask("),
                "onDisable must not register global or region tasks because Folia has already disabled the plugin");
        assertTrue(onDisableBody.contains("SchedulerUtils.cancelPluginTasks()"),
                "onDisable should only cancel already-owned scheduler tasks");
    }

    @Test
    void restoreServiceKeepsStorageLoadAndPlayerMutationInSeparateStages() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/me/danjono/inventoryrollback/services/RestoreService.java")), StandardCharsets.UTF_8);

        assertTrue(source.contains("SchedulerUtils.runTaskAsynchronously"),
                "RestoreService should keep backup storage loading off the entity tick");
        assertTrue(source.contains("PlayerScheduler.call(target"),
                "RestoreService should apply target player mutations on the target entity scheduler");
        assertTrue(source.contains("notifyStaff("),
                "RestoreService should route staff responses through a separate notification stage");
    }

    private static void collectUnsafeSchedulerPatterns(Path path, List<String> violations) {
        String source;
        try {
            source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }

        for (Pattern pattern : UNSAFE_SCHEDULER_PATTERNS) {
            if (pattern.matcher(source).find()) {
                violations.add(path + " matches " + pattern.pattern());
            }
        }
    }
}
