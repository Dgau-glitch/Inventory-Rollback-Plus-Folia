# Folia 1.21.11 migration plan

This document is a task-by-task migration plan for moving InventoryRollbackPlus to a native Folia 1.21.11 API target. Each numbered item is intentionally scoped so it can be requested and completed in a single follow-up message.

## API and documentation baseline

- Build dependency target: `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` with Maven `provided` scope, which is the Maven equivalent of Gradle `compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")`.
- Folia must be treated as region-threaded, not as a drop-in Bukkit main-thread server. PaperMC documents that `folia-supported: true` only permits loading and is not proof of real compatibility.
- Use the scheduler by ownership:
  - `EntityScheduler` for player/entity operations, because it follows entities across regions.
  - `RegionScheduler` for block, chunk, location and inventory/world work tied to a location.
  - `GlobalRegionScheduler` only for global server state such as console command dispatch, global game time, weather, world border and other global-region state.
  - `AsyncScheduler` for database, YAML/file IO, HTTP update checks and other work independent of the server tick process.


## Reference documentation checked

- Folia/Paper support guide: https://docs.papermc.io/paper/dev/folia-support/
- Folia 1.21.11 scheduler Javadocs: https://jd.papermc.io/folia/1.21.11/io/papermc/paper/threadedregions/scheduler/package-summary.html
- Paper `plugin.yml` reference: https://docs.papermc.io/paper/dev/plugin-yml/
- Folia region model overview: https://docs.papermc.io/folia/reference/overview/

## Current repository findings

- `plugin.yml` already declares `folia-supported: true`, but its `api-version` is still `1.13`; it should be evaluated in the metadata task once the migration intentionally drops old-server compatibility.
- The rest of the code still needs a full ownership audit before the plugin can be called natively Folia-safe.
- The project currently has two Java package trees: the modern entrypoint/API package under `com.nuclyon.technicallycoded.inventoryrollback` and the legacy implementation package under `me.danjono.inventoryrollback`.
- `SchedulerUtils` currently mixes compatibility behavior, reflection, global-region scheduling and async naming in one static utility. This makes it too easy to run entity-owned or location-owned work on the wrong Folia scheduler.
- GUI and restore flows contain many `openInventory`, `closeInventory`, inventory mutation and backup loading paths that must be separated into async storage stages and entity/region-owned Bukkit API stages.
- Command tab completion exists, but command registration and completion rules should be made data-driven so permission filtering happens before suggestions are generated for every command and argument.

## One-message task plan

### 1. Normalize the build to Folia API only

**Goal:** make the build target match `compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")` exactly for the Minecraft server API.

**Work:**
- Remove the separate `spigot-api` dependency from `pom.xml`.
- Keep `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` as `provided` scope.
- Keep repository configuration only where it is required to resolve the remaining dependencies.
- Decide whether to update `plugin.yml` `api-version` to `1.21.11` in the same compatibility baseline or defer it until old-server compatibility is intentionally dropped.
- Run `mvn -q -DskipTests package` and `mvn -q test`.

**Acceptance criteria:**
- The project compiles without `spigot-api`.
- No new runtime code changes are made in this task.

### 2. Introduce a Folia scheduler service abstraction

**Goal:** replace ad-hoc static scheduling with a small service API that expresses ownership explicitly.

**Work:**
- Create a scheduler service interface with methods such as `entity(Player/Entity, Runnable)`, `region(Location, Runnable)`, `global(Runnable)`, `async(Runnable)`, delayed variants and future-returning variants.
- Implement it with direct Folia API calls, no reflection.
- Keep a narrow adapter for legacy callers until later tasks migrate them.
- Remove misleading method names such as `runTaskAsynchronously` when the Folia branch actually uses the global region.

**Acceptance criteria:**
- All new scheduling entry points require the caller to choose entity, region, global or async ownership.
- Existing plugin behavior is unchanged after adapting old call sites to the new adapter.

### 3. Migrate player/entity operations to `EntityScheduler`

**Goal:** ensure all player-owned actions execute on the owning region of that player.

**Work:**
- Audit command handlers, restore actions, GUI reopen/close logic, teleport-related logic, player messaging that depends on player state, and player inventory access.
- Route player inventory mutations, `openInventory`, `closeInventory`, live player lookups and teleport/restore steps through the entity scheduler.
- Add retired callbacks for scheduled player tasks so quitting players do not leave dangling operations.

**Acceptance criteria:**
- No player/entity operation is scheduled through a location scheduler just because a last known location is available.
- Player quit/removal paths complete safely without accessing retired entity state.

### 4. Migrate location/world operations to `RegionScheduler`

**Goal:** ensure location-owned Bukkit API access executes in the region that owns that location.

**Work:**
- Audit death-location capture, world-change saves, block/chunk/location access, backup metadata with locations and any staff teleport target logic.
- Route location-bound work through region scheduling.
- Ensure any workflow that touches both staff player and target location is split into separate entity-owned and region-owned phases.

**Acceptance criteria:**
- Location/world operations do not use the global region scheduler.
- Multi-region workflows never assume two locations or two players are safe to access in the same callback.

### 5. Split storage IO from Bukkit API snapshots

**Goal:** keep YAML/MySQL/file IO async while taking live Bukkit snapshots only on the owning player/entity thread.

**Work:**
- Refactor `SaveInventory`, `PlayerData`, `YAML` and `MySQL` flows into two stages: live snapshot capture and async persistence.
- Make snapshot objects immutable enough to cross threads safely.
- Ensure purge and save operations do not access live Bukkit objects off-thread.

**Acceptance criteria:**
- Async tasks only handle serialized/snapshot data, files, database calls and CPU-only work.
- Bukkit `ItemStack`, inventory and player access is captured before async persistence starts, or is otherwise proven safe by API docs.

### 6. Refactor GUI menu flows for Folia-safe ownership

**Goal:** make restore menus, backup lists and click handlers safe under regionized threading.

**Work:**
- Move backup loading/filtering to async service methods.
- Return to the staff player's entity scheduler before opening menus or mutating staff inventory views.
- Keep menu construction separated from live inventory opening so IO and UI phases are explicit.

**Acceptance criteria:**
- `InventoryClickEvent` handlers do not perform blocking storage IO on an entity/region tick.
- Every `openInventory` and `closeInventory` call is reached from the viewer player's entity scheduler.

### 7. Refactor restore transactions into a service layer

**Goal:** centralize restore logic so Folia ownership rules are enforced once instead of duplicated in commands and GUI listeners.

**Work:**
- Create a restore service that owns validation, target lookup, snapshot preparation, target inventory application and optional staff notifications.
- Model restore as a staged transaction: async load, entity-owned target mutation, entity-owned staff response.
- Keep public behavior and messages stable.

**Acceptance criteria:**
- Commands and GUI click handlers delegate restore work to one service.
- Restore does not access target and staff player state in the same callback unless they are the same entity/thread context.

### 8. Convert command metadata and tab completion to a permission-first registry

**Goal:** satisfy command completion requirements consistently and avoid duplicating permission logic.

**Work:**
- Add command metadata containing name, aliases, permission, argument completers and executor.
- Update tab completion so permission checks happen before suggestions are built.
- Return empty lists instead of `null` when a player lacks permission for a branch, so hidden completions do not leak.

**Acceptance criteria:**
- Every command and subcommand has tab completion.
- Players without a permission never see that command or argument in suggestions.

### 9. Remove legacy Bukkit scheduler and reflection fallbacks

**Goal:** make the plugin a native Folia-targeted build rather than a Bukkit compatibility shim.

**Work:**
- Remove direct `BukkitScheduler` fallback calls from production paths.
- Remove reflection-based Folia scheduler discovery.
- Remove or quarantine old Paper/Spigot compatibility helpers that are no longer part of the Folia-only target.

**Acceptance criteria:**
- Production scheduling uses typed Folia API classes directly.
- A source search for `getScheduler()` and reflective scheduler access finds no production scheduling path.

### 10. Add Folia ownership regression tests and static checks

**Goal:** prevent future changes from reintroducing unsafe scheduler usage.

**Work:**
- Add unit/static tests or build checks that reject `Bukkit.getScheduler()`, `JavaPlugin#getServer().getScheduler()` and reflective scheduler access in production sources.
- Add tests for command tab completion permission filtering.
- Add tests around save/restore service staging where possible without a live server.

**Acceptance criteria:**
- `mvn test` fails if unsafe scheduler patterns return to production code.
- Permission-filtered tab completion behavior is covered by tests.

### 11. Runtime validation on a Folia 1.21.11 test server

**Goal:** verify the migrated plugin under real region threading.

**Work:**
- Start a local Folia 1.21.11 server with the built plugin.
- Validate enable/disable/reload/version/help/import command paths.
- Validate join, quit, death, world change, force backup, backup browsing and restore.
- Validate two-player cross-region restore and GUI flows while players are far apart.

**Acceptance criteria:**
- No Folia thread-access warnings or exceptions appear in the server log.
- Backup creation, viewing and restoration still match previous functional behavior.

### 12. Cleanup architecture after migration

**Goal:** reduce coupling and make future features easier to add without touching the core flow.

**Work:**
- Move reusable logic into clear modules: `commands`, `services`, `listeners`, `api`, `utils`.
- De-duplicate overlapping logic between `com.nuclyon...` and `me.danjono...` package trees where practical.
- Keep configuration-driven values in config files, not hardcoded in services.

**Acceptance criteria:**
- Scheduler, storage, restore and GUI responsibilities are separated.
- New restore-related features can be added by composing services rather than editing command and listener internals directly.
