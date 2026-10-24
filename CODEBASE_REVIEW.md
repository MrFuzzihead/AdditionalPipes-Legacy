# Additional Pipes (Legacy) — Codebase Review

**Analyzed:** `main` @ `2ca25dd` ("Update builds 2026") — working tree clean
**Target:** Minecraft 1.7.10 / Forge 10.13.4.1614 / BuildCraft 7.1.63 (GTNH toolchain)
**Method:** static review of `src/` (~8.3k LOC), `./gradlew compileJava` and `spotlessCheck` (both pass), and cross-checking BuildCraft 7.1.63 sources for API semantics.

**Context before you start:**

- Sources live under `com/mrfuzzihead/additionalpipes/…` but still declare `package buildcraft.additionalpipes;`. It compiles, but Gradle metadata (`modGroup`, `apiPackage`, generated `Tags` package) targets `com.mrfuzzihead…` — the repo is mid-rename.
- Your own commits `92a74a2` ("Critical fixes") and `7678cfc` ("High bugs") already fix several packet / command / obsidian issues, and upstream `1.7.10-bugfixes` has chunk-loader / distribution fixes. Do not redo those — review and merge them. This report focuses on what is still broken in `main` **and** what those branches still miss.
- A prior `CODEBASE_ANALYSIS.md` exists in commit `92a74a2`; this review is complementary and adds the systemic bugs (lifecycle, config ordering, LP recursion, item loss, etc.).

---

## 🔴 Critical

### 1. Logistics Teleport Pipe: infinite recursion → `StackOverflowError`
`pipes/PipeLogisticsTeleport.java:32-55`

Unlike `PipeItemsTeleport`, this handler has **no `justTeleported` guard**. When two logistics teleport pipes share a frequency and both can send (the default state is `Send Only`, so this is the default!), A injects into B, B's `Entered` handler sees A as its only partner and injects back into A, recursively, inside the same `injectItem` call. One item crashes the server with a stack overflow.

```java
PipeLogisticsTeleport otherPipe = getConnectedPipe();
if (otherPipe == null || !canSend()) return;
...
otherPipe.transport.injectItem(event.item, newOrientation); // B's Entered fires here
```

**Fix:** copy the `getExtraData()/justTeleported` guard from `PipeItemsTeleport`, and/or enforce sender-only ↔ receiver-only. Most urgent bug in the repo.

### 2. Teleport pipes silently stop working after a chunk unload
`pipes/PipeTeleport.java:107-121`

`initialize()` adds to `TeleportManager`; `onChunkUnload()`/`invalidate()` remove. But BC's `TileGenericPipe.onChunkUnload()` does **not** call `validate()` again when a dormant chunk is re-fetched (`ChunkProviderServer` → `ForgeChunkManager.fetchDormantChunk` → `Chunk.onChunkLoad` → `func_147448_a`), and `Pipe.initialized` remains `true`. So `initialize()` is never called again and the pipe never returns to the manager. It teleports again only after a full reload from disk or after replacing the pipe.

**Fix:** don't rely only on `initialize()`. Keep a `registered` flag and re-add in `updateEntity` when false (with a `!world.isRemote` guard), or listen for `ChunkEvent.Load` and re-add the chunk's teleport pipes. Make `TeleportManager.add` idempotent (see #12). Matches upstream commit `0f5a617`.

### 3. Obsidian Fluid Pipe compares fluid ID to millibuckets
`pipes/PipeLiquidsObsidian.java:362`

```java
fluidID.intValue() != transport.fluidType.amount
```

`fluidType` is a `FluidStack`; `.amount` is 0 for the pipe's "type" stack (`PipeTransportFluids.fill` does `new FluidStack(resource, 0)`). Water's fluid ID is 1, so **any** pipe that already has fluid rejects **every** container.

**Fix:** `transport.fluidType.getFluidID()` (already done on `feature-HighBugs`).

### 4. Obsidian Fluid Pipe: fluid duplication / item loss on partial pulls
`pipes/PipeLiquidsObsidian.java:200-280`

`pullItemIntoPipe` computes a partial `stack` via `contained.splitStack(...)` but then calls `storeAndDrainItem(contained)` — i.e. it drains the **remainder still in the world**, not the portion it took. With a large stack and a partially charged battery you store fluid for the whole stack while the entity keeps the containers (and for `IFluidContainerItem` you also mutate the entity's stack). Also:

- `storeAndDrainItem` calls `getCapacity(stack)` / `drain(stack, capacity, true)` on a **stack**; `IFluidContainerItem` implementations assume stack size 1, so multi-item stacks are handled inconsistently.
- The `TravelingItem travelingItem = ...` at line 273 is dead code.
- If `currentItem` is null (e.g. `drainFluidContainer` returns null), `dropCurrentItem()` → `TravelingItem.make(..., null)` → `PipeEventItem.DropItem` handler at line 325 (`event.entity.getEntityId()`) NPEs before the null check.
- If the pipe's fluid type changes mid-buffer, `transport.fill(UNKNOWN, ...)` returns 0 forever and `fluidInItem` never drains.

**Fix:** process one container per operation (`stackSize == 1`), pass the actually-removed stack, handle empty-container semantics consistently, null-check `currentItem` / `event.entity`, and use BC's `WeakHashMap<Entity, Long>` pattern instead of `HashSet<Integer> entitiesDropped` (recycled entity IDs can permanently block legitimate pickups).

### 5. Closed Pipe buffer is lost when the pipe is broken
`PipeItemsClosed.dropContents()` doesn't drop `inventory[0..9]`; only the GUI recovers them. Breaking the pipe loses the whole buffer.

**Fix:** override `getDroppedItems()` / `dropContents()` to return/drop the inventory contents (see `PipeItemsJeweled.dropContents` for the pattern).

### 6. First launch never registers the chunk loader/tether/keybind
`APConfiguration.java:36-38`, `AdditionalPipes.java:160,175,209`

`loadConfigs(false, file)` returns immediately if the config file doesn't exist. On a brand-new install `enableChunkloader` therefore stays at its Java default (`false`), so `init()` skips the tether block, recipe, chunk-view proxy and key handler. Only `loadConfigs(true, …)` later in `init()` creates the file. The second launch works. The `(exists, init)` gating logic is also inverted-looking and fragile.

**Fix:** always load (and save defaults for) the config in `preInit`; remove the two-phase hack. While there, clamp `waterPumpWaterPerTick`, `gravityFeedPipeTicksPerPull` and `chunkSightRange` (negative/zero values currently produce nonsense; `chunkSightRange` only clamps the upper bound).

### 7. Priority Insertion Pipe ignores lower priorities when the top priority is full
`pipes/PipeItemsPriorityInsertion.java:57-76`

`foundAny = true` is set whenever an inventory **exists** at that priority, even if `transactor.add(...)` moved 0 items. The loop then `break`s, `result` stays empty, and `event.destinations` is left untouched → the item is routed to a random valid side instead of falling through to the next priority.

**Fix:** set `foundAny` only when at least one side accepted.

### 8. Distribution Pipe can send items into an unconnected side
`pipes/PipeItemsDistributor.java:38-73`

The handler clears `event.destinations` and adds `VALID_DIRECTIONS[distSide]` unconditionally. `toNextOpenSide()` only checks `isPipeConnected` while advancing; between advances the side may be wrenched off. BC then drops the item at `ReachedEnd`. Also, if no side is connected/positive, the loop exits with `distSide` wherever it landed.

**Fix:** only add the side if it is still in `event.destinations`; otherwise advance/skip, or bounce the item.

---

## 🟠 High

### 9. Packet handlers are unvalidated (and mostly unauthenticated)
`network/message/*.java`

- `MessageDistPipe` / `MessagePriorityPipe` / `MessageJeweledPipeOptionsServer` / `MessageTelePipeUpdate` check `te instanceof TileGenericPipe` but cast the inner pipe without an `instanceof` check → `ClassCastException` for any other pipe type.
- `MessageJeweledPipeOptionsServer` does `pipe.filterData[message.index - 1]` with no bounds check (`index` is an attacker-controlled byte).
- `MessageTelePipeData` casts `(TileGenericPipe) te` with no check at all.
- No handler verifies the player is near the pipe or owns it (except the partial owner check in `MessageTelePipeUpdate`). Any client can reconfigure any distributor/priority/jeweled pipe anywhere on a server.
- `MessageChunkloadRequest` and `MessageChunkloadData` / `MessageTelePipeData` allocate arrays from unbounded network lengths.

Exceptions inside these handlers are logged/dropped by FML rather than crashing the server, but they are easy log-spam/desync vectors. A shared `PositionMessage` base with `getPipe(Class<T>)` + `isPlayerNearBlock` (as in `7678cfc`) is the right refactor.

### 10. Teleport frequency changes only re-register when the GUI closes
`gui/ContainerTeleportPipe.java:141-152`, `message/MessageTelePipeUpdate.java`

The packet sets `pipe.setFrequency(...)` immediately; the manager `remove(old)/add(new)` happens in `onContainerClosed`. A packet sent without a GUI (malicious client, automation, replay) leaves the pipe registered under the old key while `getFrequency()` reports the new one. Two simultaneous GUIs double-remove/add.

**Fix:** make `PipeTeleport.setFrequency` update the manager atomically (`remove(this, old); frequency = new; add(this, new)`), and have the container not do it.

### 11. `canPlayerModifyPipe` NPE + no range check
`pipes/PipeTeleport.java:344-348`

`pipe.ownerUUID.equals(...)` NPEs for a pipe with no owner (placed by automation/fake player). A crafted `MessageTelePipeUpdate` hits this before ownership is checked. Treat `ownerUUID == null` as public/unowned, and add the 8-block distance check (still unfixed on `feature-HighBugs`).

### 12. `TeleportManager` is a fragile global
`pipes/TeleportManager.java`

- `add` doesn't deduplicate; a pipe re-added without a matching remove (e.g. a future `validate` hook) appears multiple times in channels — skews random selection and GUI counts.
- Singleton state is only reset on `FMLServerStartingEvent`; add an `FMLServerStoppedEvent` reset (and clear on world unload).
- Four parallel multimaps + raw `Collection` switches → use one `Map<PipeType, Multimap<Integer, ITeleportPipe>>` (or a `Channel` class) to delete ~80 lines and the raw casts.
- `getWorldSave()` is dead and nonsensical.

### 13. Chunk loader persistence is broken
`chunkloader/TileChunkLoader.java`, `chunkloader/ChunkLoadingHandler.java`

- `forceChunkLoading` never writes `xCoord/yCoord/zCoord` into `ticket.getModData()`. After a restart, `ticketsLoaded` reads zeros, can't find the tile, and neither adopts nor releases the ticket. Effect: tethers don't resume force-loading after restart (until a player visits), and orphaned tickets can leak.
- `loadDistance` is never saved (`readFromNBT`/`writeToNBT` are empty overrides).
- `setLoadDistance(dist)` calls `forceChunkLoading(chunkTicket)`; `forceChunkLoading` begins with `stopChunkLoading()` → it **releases the ticket it is about to reuse**. Guard `if (ticket == chunkTicket) return;` or release only a different ticket.
- `unforceChunkLoading()` NPEs if `chunkTicket == null` (dead code).
- `requestTicket` returning null (ticket limit) is silent; log a warning.
- `forceChunkLoading` logs every forced chunk at `INFO` — 9 lines per tether per load. Use `Log.debug` once per tether.
- `enableChunkloader=false` removes the block registration entirely; worlds with existing tethers then lose the block. Add `enableChunkloaderRecipe` (as upstream did) and keep the block registered.

### 14. Chunk-boundary viewer: request spam and off-centre scan
`ChunkLoadViewDataProxy.java:159-232`

- `tickEnd(WorldTickEvent)` calls `requestPersistentChunks()` **every tick** while lasers are active. `nextTickSpacing()` (5 s) is dead legacy code. This is ~20 requests/s, each scanning 17×17 chunks and sending a packet.
- `playerX = (posX>>4) - sightRange/2`, then loop `-sightRange..sightRange` → the scanned window is `[pc-1.5r, pc+0.5r]`, shifted half a range toward −X/−Z. Drop the offset (or loop `-r/2..r/2` and document the size).
- Iterate `ForgeChunkManager.getPersistentChunksFor(world).keySet()` and filter by distance instead of doing a grid `containsKey` over ~289 positions.
- `activateLasers` creates 6 `EntityBlock`s per chunk (up to ~1700 entities for 289 chunks). Consider a single `RenderWorldLastEvent` box renderer, or only render chunks within ~128 blocks of the player.
- `requestPersistentChunks` NPEs on the server when `enableChunkloader=false` (`chunkLoadViewer` is null) — add a null check.
- Rewrite `MessageChunkloadData` to send a bounding region or delta-encoded shorts instead of 2 ints per chunk.

### 15. Water Pump Pipe runs on the client
`pipes/PipeLiquidsWaterPump.java:27-36`

No `getWorld().isRemote` guard: the client calls `transport.fill(...)` every tick, mutating client-side fluid sections/`fluidType` that the server never had (visual desync/flicker; BC's fluid update packets will fight it). One-line fix. Also consider a per-pipe GUI/config for the pump rate rather than only the global config.

### 16. Power Teleport accounting is questionable
`pipes/PipePowerTeleport.java:55-140`

- `receiveEnergy` always returns `energy`, even though `powerToSend` is only 90 % and `MathHelper.ceiling_double_int(powerToSend / needsPower.size())` can sum to **more** than `powerToSend`. Small packets can create RF from nothing; rejected deliveries are silently voided.
- `requestEnergy` reads `nextPowerQuery[...]` immediately after calling `requestEnergy(...)`, which both mutates the neighbor's next-tick query and is then counted again by the caller. The author's `// TODO does this work??` is justified. It also ignores the `value` cap and never reports demand from a machine placed directly next to the teleport pipe (only `IPipeTile` neighbors are considered).

Add a focused in-game test matrix (idle/full/partially full/removed receiver, transmittance 0/50/90/100) and compare with BC's `PipePowerDiamond`.

### 17. Other high-impact smaller bugs

- `PipeItemsClosed`: inventory is 10 slots but the GUI (`ContainerPipeClosed`) only exposes 9; `decrStackSize` NPEs on an empty slot; and the overflow branch destroys the oldest item **and** voids the incoming item when the transactor moved 0 items (write the rotation only when `add` moved > 0). — `PipeItemsClosed.java:21,53-65,113`
- `PipeTransportItemsLogistics.pipelineEndsinLogisticsPipe` is unbounded recursion with a `catch (StackOverflowError)` band-aid; convert to iterative BFS with a visited set/depth cap. It runs on every `canPipeConnect` for long LP pipelines.
- `PipeTeleportInformationProvider.isPowerPipe()` returns `true` for a **logistics** (item) teleport pipe; `acceptItem` uses `||` where `&&` is intended (`BlockGenericPipe.isValid(pipe) || pipe.canSend()`); `refreshTileCacheOnSide` indexes `cache[side.ordinal()]` without handling `UNKNOWN`.
- `PipeItemsPriorityInsertion.readFromNBT` accepts a `priorities` array of any length; `PipeTeleport.readFromNBT` calls `UUID.fromString` unguarded. Corrupt/old NBT → chunk-load crash.
- `PipeItemsDistributor` / `PipeItemsAddition` rebuild destinations from all 6 sides instead of intersecting with `event.destinations`. Prefer filtering `event.destinations`.
- `PipeTeleport` uses a 3-superclass reflection walk to BC's **private** `updateSignalState(PipeWire)`, logs a stack trace if it fails, and then calls the null `Method` later. Wrap the whole teleport-wire feature in a `supported` flag. Also note that invoking BC's private one-color method on teleport pipes runs BC's propagation, bypassing the teleport-aware override — worth an in-game wire test.
- `PipeLogisticsTeleport.blockActivated` calls `transport.switchSource()` **before** the `isRemote` check, so the connection buffer is mutated client-side. Guard the mutation, not just the GUI open.
- `GuiDistributionPipe` / `GuiPriorityInsertionPipe`: `int guiX = ...` in `initGui` shadows the `public int guiX` field, so the title is drawn at `(42,22)` / `(33,22)` screen-relative instead of over the GUI. Also the middle "value" buttons are clickable and increment the value, and both GUIs lose rapid clicks because they send absolute values computed from stale client state.
- `ItemDogDeaggravator`: `isOnSameTeam` is fine for tamed wolves (`EntityTameable` checks the owner), but `setAttackTarget(null)` and `playSoundAtEntity` run on the client too; add a server guard (and a cooldown to prevent right-click spam scanning 33×21×33 blocks).
- `EntityBetterCat`: dead/unregistered; `ticksExisted % 40 * 12 == 0` is `(ticksExisted % 40) * 12 == 0` (every 40 ticks, not every 480) and it has a `System.out.println`. Delete it.
- `PipeItemsClosed`'s 10th slot is effectively a hidden staging slot; either expose all 10 or simplify to 9 and adjust the rotation.
- `PipeItemsJeweled` `SideFilterData.markDirty()` is a no-op and the container never marks the pipe dirty; periodic saves can miss filter changes until an unload/save. Call `pipe.container.markDirty()`.
- `PipeItemsJeweled` / `Textures` reference `GuiJeweledPipe.NUM_TABS` (a `@SideOnly(CLIENT)` class) from common code. It works only because `NUM_TABS` is a compile-time constant; move the tab count to a common constant before it stops being constant.
- `TileChunkLoader` logs at INFO per chunk; `ChunkLoadViewDataProxy` / `TeleportManager` log strings are built even when debug is off. Add `Log` overloads that take a `Throwable`; stop using `printStackTrace`.
- `CommandAdditionalPipes`: `args[1]` AIOOBE, wrong list for `"power"`, no `"logistics"`, no aliases/dimension in output (partially fixed on your branch).
- Recipe dead keys: `PipeItemsGravityFeed` declares `'S', "stone"` but the pattern has no `S`; `PipeLiquidsWaterPump` declares lowercase `'w', waterproof` but the pattern has no `w`. The `allowWRRemove` block re-adds BC's uncraft recipes that BC 7.1.63 already adds itself (comment acknowledges it) — remove the BC-pipe half.
- AP's obsidian-fluid-pipe recipe is the same shapeless `obsidian item pipe + waterproof` that BC uses for its experimental `PipeFluidsObsidian`; with `B:experimentalContent=true` the two recipes collide (BC's is registered first, so AP's is unreachable). Rename/differentiate the AP pipe or drop it when BC's experimental pipe is enabled.

---

## 🔵 Low / cleanup

- `TeleportManagerTest` runs in production `init()`, renames the current thread to "Server thread" to fool `getEffectiveSide`, and has no `finally` around the rename. Move it to `src/test/java` with JUnit; remove the runtime call.
- `AdditionalPipes.VERSION = "4.7.7"` is hardcoded while the build generates `com.mrfuzzihead.additionalpipes.Tags.VERSION` (4.7.6+). Use the token (or move/rename `Tags`) so `@Mod.version` matches `mcmod.info`.
- `@Mod` uses `after:BuildCraft|Silicon` but uses `BuildCraftSilicon.redstoneChipset` unconditionally in recipes and Assembly; make Silicon `required-after` or null-check. Likewise the whole mod is `after` BC, not `required-after` — it will load and throw `NoClassDefFoundError` without BC.
- The jar ships 9 duplicate `logisticspipes/api/*` classes (and `IRequestAPI`/`IHUDArmor`/etc. are unused). `ILPPipeTile` is the only one used, and LP is already `compileOnly`; remove the copies and import LP's API (or keep only the one interface, documented as an intentional shim). This avoids class-shadowing and shrinks the jar.
- Package/directory mismatch + Gradle `modGroup`/`apiPackage`/`generateGradleTokenClass` all disagree. Complete the rename (packages, `Class.forName` string in `AdditionalPipes.postInit`, `@API` package for the LP shim) or revert the directory move. Right now every `import buildcraft.additionalpipes.*` points at a folder that doesn't exist.
- `LICENSE` is MIT but 13 source files carry the BuildCraft Open Source License ("does *NOT* grant the right to redistribute"). Add the BuildCraft license/NOTICE and clarify file-level licensing, or relicense/rewrite those files.
- `README.md` still documents MC 1.5/BC3, `setupDecompWorkspace`, and Eclipse; rewrite for the GTNH/RFG toolchain (`./gradlew build`, `runClient`, Java 25, `./gradlew spotlessCheck`).
- Remove dead code: `TeleportManager.getWorldSave/frequencyNames/*` (no UI), `APConfiguration.enableTriggers`/`chunkSightAutorefresh` (never read), `ChunkLoadViewDataProxy.nextTickSpacing/getLabel`, `TileChunkLoader.unforceChunkLoading`, `PipeLiquidsObsidian.entitiesDroppedIndex`, `MessageTelePipeUpdate._newData`, commented legacy code in the GUIs, `GuiTeleportPipe`'s `StringBuilder` chains.
- Localization: `GuiTeleportPipe`, `GuiPipeClosed` (uses non-namespaced `"Closed Pipe"`), and `GuiJeweledPipe`'s English strings are hardcoded; other lang files are 20–40 % complete. Move strings to lang keys.
- `NetworkUtils` uses `(len/8)+1` bytes (one wasted when `len % 8 == 0`); `PipeItemsJeweled` syncs three boolean arrays when one bitmask byte would do.
- `GuiJeweledPipe` adds **all 162 filter slots** to the container (only 27 visible); `Container.detectAndSendChanges` loops all 198 slots every tick. A single active-tab container (27 slots) with a small "switch tab" packet would be materially cheaper.
- `PipeItemsPriorityInsertion` / `PipeItemsAddition` run a simulated `Transactor.add` for every neighbor on every item arrival; cache per side/item for a tick if profiling shows it.

---

## ⚡ Optimizations worth doing

1. **Throttle chunk-laser refresh** (biggest win): one request every 100 ticks instead of every tick; ideally push updates only when the persistent-chunk set changes.
2. **Cache teleport channel membership per tick** in `TeleportManager` (or a revision counter): `PipeItemsTeleport` / `PipeLiquidsTeleport` / `PipePowerTeleport` / `updateSignalState` all re-scan/reallocate `ArrayList`s per event. In power networks this is the hot path.
3. **Avoid 4 AABB scans per tick** in `PipeLiquidsObsidian` when idle: scan the largest box once and filter by distance, and only run every N ticks (or only when an item entity is near, via `World.getEntitiesWithinAABB` once).
4. **Cache switch-pipe powered state** per tick instead of calling `isBlockIndirectlyGettingPowered` for every face/icon request every frame.
5. **Turn the LP pipeline recursion into an iterative walk** with a visited set; cache results until a neighbor changes.
6. **Iterate persistent chunks** instead of the grid in `sendPersistentChunksToPlayer`; send a region/delta list instead of 8 bytes/chunk.
7. **Log laziness**: guard `String.format`/concatenation with `enableDebugLog`; add `Log.error(String, Throwable)` and delete `printStackTrace`s.
8. **Container slot count** (see above) and **GUI absolute-value packets** (send deltas or optimistically update the client).

---

## ✨ Features I'd recommend

- **WAILA/HWYLA + BC debugger** for teleport pipes: frequency, owner, send/receive state, network size, partner coordinates. Currently the debugger only shows jeweled filter flags.
- **Frequency naming UI** (the manager already has `frequencyNames`, but nothing sets or shows them) plus `/additionalpipes teleport all|logistics` with tab completion and dimension in the output.
- **Teleport pipe wrench actions**: copy frequency/owner between pipes, toggle send/receive, and a visual per-frequency tint/particle.
- **Teleport scanner** (already a TODO at `AdditionalPipes.java:106`): a handheld item that highlights pipes on a frequency, useful for large bases.
- **Gate triggers/actions for teleport pipes** (`GateProvider` has an empty `if (pipe instanceof PipeTeleport)`): "frequency has N pipes", "item/fluid/power teleported", "owner online"; actions to change frequency/toggle state.
- **Chunk-loader GUI** (radius, chunk count, ticket status) and a config `enableChunkloaderRecipe`; make `loadDistance` configurable/persisted.
- **Config GUI** (`IModGuiFactory`) + client/server config split; clamp all numeric values; expose `chunkSightAutorefresh` and `enableTriggers` or delete them.
- **Ghost slots + NBT/ore-dict matching + "copy filters to all sides"** for jeweled filters; empty filter = allow all; shift-click support in all pipe containers.
- **Per-pipe water pump rate** and a **closed-pipe overflow policy** (void oldest / stop accepting / drop) instead of silent double loss.
- **Recipe config** to disable individual pipes (use BC's `BCRegistry` instead of `BlockGenericPipe.registerPipe` directly) and NEI hiding for the deprecated insertion pipe.
- **JUnit test suite** (manager, filters, NBT round-trips, config clamping, packet validation) instead of runtime tests; add a manual test matrix for teleport loops, chunk unload/reload, LP on/off, and power accounting.
- **Finish or clearly mark the LP integration**: add the teleport tag guard, make `isPowerPipe()` false, fix `acceptItem`'s `||`, and test both LP-present and LP-absent dedicated servers.

---

## 🧱 Refactors / build hygiene

- Complete (or revert) the package rename, then align `modGroup`, `apiPackage`, and `generateGradleTokenClass`.
- Remove the duplicated LP API classes; use the `compileOnly` LP dependency.
- Split `AdditionalPipes` (541 lines) into `PipeRegistry`, `RecipeRegistry`, `ChunkLoaderRegistry`, and `LPCompat`; the `postInit` reflection belongs in `LPCompat`.
- Add a `PipePositionMessage` base class for the 9 messages (coords + `getPipe(Class<?>)` + range/ownership check).
- Replace the four `TeleportManager` multimaps/`switch` blocks with a typed channel map; make `add` deduplicating and life-cycle aware.
- Use `EnumMap<ForgeDirection, Integer>`/`int[]` helpers instead of 6-way `switch` copies for icons/side GUIs; extract the repeated `getIconIndex` face mapping.
- Replace `IInventory` boilerplate in `PipeItemsClosed` with a small `PipeInventory extends Inventory` (or BC's inventory helpers) and implement `markDirty` correctly.
- Use `Tags.VERSION` for the mod version; fix the `@Mod` dependency list (`required-after` BC/Silicon, `after` LP/Factory).
- Add the BuildCraft license/NOTICE; update the README for RFG/GTNH; remove `.pdn` art sources from the jar resources.
- Merge/review the fixes already on `92a74a2`/`7678cfc` and the upstream `1.7.10-bugfixes` branch (`0f5a617` chunk-unload re-add, `5d7769c` switch-pipe update, `457f582`/`b921d6e` tether recipe/model, `ead515b` distribution, `8001e65` gravity feed) before writing new patches; the current `main` is behind its own fix branches.

---

## Redundancy vs BuildCraft 7.1.63 (GTNH)

### Baseline and rubric

This compares the AP item set against the **BuildCraft 7.1.63 sources the mod compiles against**, with the GTNH default config:

- Every item/fluid/power pipe in `buildcraft/transport/pipes/` was reviewed.
- `run/client/config/buildcraft/objects.cfg` enables `PipeItemsDiamond`, `PipeItemsClay`, `PipeItemsVoid`, `PipeItemsWood`, etc.
- `main.cfg` has `B:experimentalContent=false`, so BC's `PipeFluidsObsidian` is **not** registered by default. It only becomes relevant if a pack turns experimental content on.

BC 7.1.63 already ships: **Wood/Iron** (extraction), **Clay** (insertion), **Diamond** (per-side filters, round-robin), **Obsidian** (item suction), **Void**, plus fluid/power variants and the gate **Pipe Valve** action (`ActionValve`: per-side open/input-only/output-only/closed).

**Redundancy % rubric** — estimated fraction of the AP pipe's *common use cases* that a stock BC 7.1.63 pipe + gate can satisfy:

| Range | Meaning | Default action |
|---|---|---|
| 0–20 % | Unique behavior; BC cannot replace it | Leave |
| 21–50 % | Partial overlap; AP still has a distinct use case | Keep or tweak |
| 51–75 % | Substantial overlap; only niche features remain | Tweak (or deprecate if unmaintained) |
| 76–100 % | Effectively redundant; BC does it better/equal | Deprecate/remove |

### Summary table

| AP item | Closest BC 7.1.63 equivalent | Redundancy | What AP still uniquely adds | Recommendation |
|---|---|---:|---|---|
| `PipeItemsJeweled` | `PipeItemsDiamond` (54 total filter slots, 9 per side, round-robin, damage matching) | **~55 %** | NBT-match toggle (BC's `StackHelper.isMatchingItemOrList` passes `matchNBT=false`), ignore-metadata toggle, 27 slots per side, per-side "accept unsorted" | **Tweak/keep** — NBT filtering is genuinely useful in GTNH; trim the 162-slot GUI |
| `PipeItemsDistributor` | `PipeItemsDiamond` (random/even split; weighted only by duplicating filter entries for one item type) | **~40 %** | General weighted counts per side, independent of item type | **Keep/tweak** (fix the unconnected-side bug) |
| `PipeItemsPriorityInsertion` | `PipeItemsClay` (plain insertion) | **~20 %** | Per-side priority 1–6 | **Keep** |
| `PipeItemsAddition` | none | **~5 %** | Insert only into inventories that already contain the item | **Keep** |
| `PipeItemsClosed` | `PipeItemsVoid` | **~50 %** | 10-slot buffer + "pipe closed" gate trigger | **Tweak/keep**; rebrand as "Buffered Void Pipe" |
| `PipeSwitchItems` / `Fluids` / `Power` | Gate with `TriggerRedstoneInput` + `ActionValve` (`OPEN`/`INPUT_ONLY`/`OUTPUT_ONLY`/`CLOSED`, per side) | **~85 % functional** (0 % cost/space) | No gate/chipset/trigger required; instantaneous redstone reaction; texture feedback | **Leave** (small, cheap, popular), or **tweak** to add side/input-output modes to justify it |
| `PipeItemsGravityFeed` | `PipeItemsWood` oriented upward (needs RF, faster) / `PipeItemsIron` | **~40 %** | Passive (no RF), vertical-only, configurable slow rate | **Keep** |
| `PipeLiquidsWaterPump` | BC Pump machine (powered, multi-block); `PipeFluidsObsidian` (experimental, drains blocks) | **~30 %** | Passive 1-block water generation | **Keep** |
| `PipeLiquidsObsidian` | `PipeFluidsObsidian` (experimental only; drains world fluid blocks) / `PipeFluidsVoid` | **~10–15 %** (0 % with `experimentalContent=false`) | Vacuums fluid **containers** from the ground (buckets/tanks), not blocks | **Keep, rename** to avoid the "Obsidian" collision; differentiate the recipe when BC experimental content is on |
| `PipeItemsTeleport` / `PipeLiquidsTeleport` / `PipePowerTeleport` / `PipeLogisticsTeleport` | none | **0 %** | Teleportation | **Keep** |
| Chunk loader block (`TeleportTether`) | none standalone (BC's quarry only tickets its own chunk) | **0 %** | Standalone 3×3 chunk loader | **Keep** |
| `ItemDogDeaggravator` | none | **0 %** | (not a pipe) | **Keep** |

**Bottom line:** with `Advanced Wood` and `Advanced Insertion` removed, **no remaining pipe is fully redundant**. `Jeweled` (~55 %) and `Closed` (~50 %) have substantial overlap but keep distinct features, the switch pipes are ~85 % functionally covered by gates but remain a cheap gate-free alternative, and the rest (teleports, priority/addition, distributor, gravity feed, water pump, obsidian fluid, chunk loader) are unique or nearly so.

### Detailed notes

**`PipeItemsJeweled` — keep the NBT toggle, trim the GUI.** BC's Diamond pipe's filters are 54 slots (9 per side) and its `StackHelper.isMatchingItemOrList` ultimately calls `isMatchingItem(a, b, true, false)` — i.e. it **matches damage but ignores NBT**. AP's Jeweled pipe's `matchNBT` and `matchMetadata` toggles are therefore genuinely additive for modded packs full of NBT-variant items, and 27 slots per side is more room. That said, the current implementation puts **162 slots** in the container and loops them all every tick; a 27-slot active-tab container plus a "switch tab" packet would retain the feature at a fraction of the cost. Also consider adding ore-dictionary matching (BC has it, AP does not) and ghost slots so filters do not consume items.

**`PipeItemsDistributor` — weighted distribution is unique.** BC's Diamond pipe can distribute evenly/randomly, and can approximate weights by putting the same item in multiple filter slots on one side, but only for items that match those filters. AP's `distData` gives general per-side weights for any item. Keep it, fix the disconnected-side item drop, and consider adding a round-robin mode so it can also serve as a cheap Diamond-pipe alternative.

**`PipeItemsPriorityInsertion` / `PipeItemsAddition` — unique, keep.** BC's Clay insertion pipe has no per-side priority, and no BC pipe implements "only insert into inventories that already contain this item". These are the kind of pipes that justify the addon.

**`PipeItemsClosed` — rebrand, do not remove.** BC's Void pipe already covers "never drop items". The closed pipe's value is the **buffer + gate trigger** (and the GUI retrieval). Keep it, but call it a "Buffered Void Pipe" so players do not see it as a duplicate Void pipe; also fix the double item loss on overflow and drop the buffer on break (currently lost).

**Switch pipes — functionally covered by gates, but cheap.** A BC gate with `TriggerRedstoneInput` → `ActionValve` (CLOSED) reproduces the redstone-controlled valve, and `ActionValve` can also do per-side/input-only/output-only, which AP's switch cannot. The AP pipe's advantages are cost, no gate/chipset, instant reaction, and the texture feedback. With only ~56 lines in `PipeSwitch` plus three tiny subclasses, the maintenance cost is low — **leave them**, or add side-selection modes if you want a stronger reason to exist.

**`PipeItemsGravityFeed` / `PipeLiquidsWaterPump` — keep.** Both are passive and RF-free, while BC's equivalents are powered machines/pipes (Wood pipe needs RF; the Pump is a multi-block machine). They fill a real niche, even if a powered BC setup can approximate them.

**`PipeLiquidsObsidian` — keep, but rename and fix.** BC's `PipeFluidsObsidian` only exists when `experimentalContent=true` and drains fluid **blocks** from the world; AP's pipe vacuums fluid **containers** (buckets/tanks). Those are different behaviors, so the pipe is not redundant in the GTNH default. The name and recipe collide when experimental content is enabled, so either rename AP's pipe (e.g., "Fluid Container Vacuum Pipe") or register it conditionally/skip it when `PipeFluidsObsidian` is present. Given its current fluid-ID and partial-pull duplication bugs, this pipe needs the fixes in #3/#4 before it is worth keeping.

### Removal note (applied in this change)

`PipeItemsAdvancedWood` (with `PipeTransportAdvancedWood`, its GUI/container and `MessageAdvWoodPipe`) and `PipeItemsAdvancedInsertion` were removed outright, including their recipes, lang keys and textures. BC stores the pipe as a **numeric item ID** in the tile NBT:

```java
// TileGenericPipe.writeToNBT
nbt.setInteger("pipeId", Item.getIdFromItem(pipe.item));
// TileGenericPipe.readFromNBT
pipe = BlockGenericPipe.createPipe((Item) Item.getItemById(coreState.pipeId));
```

Any existing pipe of these types in an older world will therefore fail `createPipe` on load and be deleted. For a released fork this is a breaking change worth calling out in the changelog; if world compatibility is needed later, keep the items registered as stubs/BC-equivalent classes or add `FMLMissingMappingsEvent` remaps.

### Recommended disposition

| Action | Pipes |
|---|---|
| **Removed in this change** | `PipeItemsAdvancedWood`, `PipeItemsAdvancedInsertion` |
| **Tweak then keep** | `PipeItemsJeweled` (NBT/meta toggles, trim GUI, ghost slots), `PipeItemsDistributor` (fix disconnected-side drop), `PipeItemsClosed` (rename Buffered Void, fix overflow/loss), `PipeLiquidsObsidian` (rename/recipe + #3/#4 fixes) |
| **Leave alone** | `PipeItemsPriorityInsertion`, `PipeItemsAddition`, `PipeItemsGravityFeed`, `PipeLiquidsWaterPump`, switch pipes, all teleport pipes, chunk loader, Dog Deaggravator |

## Suggested fix order

1. **#1** LP teleport recursion (server crash).
2. **#2** teleport re-registration after chunk unload.
3. **#3 / #4** obsidian fluid ID + partial-pull duplication (one line + a small rewrite).
4. **#6** first-run config ordering (chunkloader missing until restart).
5. **#5** drop the Closed Pipe buffer on break.
6. **#9–#12** packet validation + teleport manager lifecycle/frequency/owner.
7. **#13 / #14** chunk loader persistence + laser request throttling.
8. **#15–#17** power/closed pipe/GUI bugs.
9. Everything in 🔵 and the refactors.
10. The two immediate one-liners: `getFluidID()` in the obsidian pipe and `isRemote` in the water pump.

**Separate workstream:** `PipeItemsAdvancedWood` and `PipeItemsAdvancedInsertion` are removed; the remaining redundancy decisions are to tweak `Jeweled`/`Distributor`/`Closed`/`Obsidian` and leave the rest alone.
