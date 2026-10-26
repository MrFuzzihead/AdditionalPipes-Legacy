# Additional Pipes — Test Plan

This covers the bug-fix batches through pass 2. Unit tests and a headless dedicated-server self-test are automated; the remaining checklist is for client-only rendering/GUI behaviour and LP-present integration.

## Headless dedicated-server self-test

Run:

```bash
./gradlew selfTest --no-configuration-cache
```

This prepares a disposable flat world under `build/selftest-server`, starts the dedicated server with `-Dadditionalpipes.selftest=true`, and runs `SelfTest` from `FMLServerStartedEvent`. It verifies:

- every Additional Pipes item is registered with a registry name;
- teleport manager registration, pairing, chunk-unload removal, re-registration, and frequency moves;
- Closed Pipe `DropItem` buffering, overflow, and clearing on drop;
- Water Pump server-side water production;
- Power Teleport delivery under 50 % transmittance, including a full receiver with no demand;
- Addition Pipe choosing the inventory that already contains the item;
- the `additionalpipes teleport` command family does not throw;
- teleport manager reset clears every channel type.

The Gradle task fails if the log contains `[SELFTEST] FAIL` or lacks `[SELFTEST] DONE failures=0`. The log is `build/selftest-server/logs/fml-server-latest.log`.

`--no-configuration-cache` is required because the harness reads its own server log in a task action.

## Automated unit tests

Run:

```bash
./gradlew test
./gradlew spotlessCheck
```

There are **65 JUnit tests** in `src/test/java`:

| Test class | Covers |
|---|---|
| `ClosedPipeBufferTest` | merge/fill, FIFO eviction without double loss, NBT-sensitive non-merge, empty stacks, remove/clear |
| `DistributorRoutingTest` | next-side rotation, zero weights, disconnected sides, wrap-around, no usable side, malformed input |
| `DistributorDataTest` | weight clamping, invalid indexes, all-zero reset |
| `DistributorEventTest` | distributor `FindDest` handler with disconnected/zero-weight sides |
| `PowerAllocationTest` | proportional demand allocation, remainder handling, no energy creation, integer overflow |
| `APConfigurationTest` | water-pump and gravity-feed config clamping |
| `LogisticsPipelineTest` | straight LP pipeline, branching rejection, no-LP end, cycle termination |
| `PipeTeleportReflectionTest` | private BC signal method is resolved from `Pipe`, not a superclass walk |
| `PipeItemsClosedEventTest` | `DropItem` buffering, entity suppression, overflow eviction |
| `TeleportManagerTest` | typed channel isolation, current-manager lookup, idempotent add, remove, send-only/receive-only filtering, private owner matching, public-channel matching, clear |
| `PipeTeleportFrequencyTest` | `setFrequency` only moves a registered pipe in the manager |
| `PipeTeleportNbtTest` | corrupt owner UUID, negative frequency, state-bit clamping |
| `PipeLiquidsWaterPumpTest` | server-only pump decision helper |
| `TeleportTagTest` | LP/item teleport marker set/consume |
| `PipePermissionsTest` | public, creative, owner, other-player, unowned-private, unowned-public |
| `PipeTooltipsTest` | coloured mB/t and RF/t rate lines, loaded descriptions and Shift details |
| `MessageSerializationTest` | `MessageDistPipe`, `MessageTelePipeUpdate`, and `MessageTelePipeData` byte round-trips |

These are pure-logic tests and do not start a Minecraft world.

## In-game verification checklist

Use a creative test world and keep the log open (`run/logs/latest.log` or the console).

### 1. Logistics Teleport Pipe recursion (critical)

1. Place two `PipeLogisticsTeleport` pipes on the **same frequency**; connect each to an LP pipeline or a chest.
2. Feed one item into one pipe.
3. Expected: **no `StackOverflowError` / server crash**.
4. Repeat with:
   - both pipes left at the default **Send Only**;
   - one **Send Only** + one **Receive Only**;
   - both **Send & Receive**, each with a valid output.
5. If a pipe has no output, the item may bounce between the pair; that is a routing loop, not a crash. Confirm the server keeps ticking and no stack trace appears.
6. Wrench a `PipeLogisticsTeleport` on a multiplayer client. Verify the connection switches on the server and the client does not mutate its own connection state.

### 2. Teleport pipe chunk unload / reload (critical)

1. Place item teleport pipes A and B on the same frequency in different chunks, each with an input and output chest. Verify items teleport.
2. Move far enough away that B's chunk unloads (at least beyond the server view distance; use `F3+G` to see chunk borders).
3. Return and verify items teleport again **without breaking or replacing B**.
4. Repeat 2–3 a few times. Try a quick trip just beyond the unload distance and back (this exercises the dormant-chunk cache), then a longer trip.
5. Check the GUI connected-pipe count: it should not show duplicates after repeated unload/reload cycles.

### 3. Frequency re-registration (high)

1. Put A and B on frequency **1**; verify items flow.
2. Open A's GUI, change the frequency to **2**, close the GUI.
3. Change B to **2** as well. Verify items flow **immediately**, without reopening A.
4. Change A back to **1** while B remains on 2. Verify flow stops (no stale registration on the old channel).
5. Change A to 2 again; verify flow resumes.
6. Also test state / public changes through the GUI and confirm they persist after a world reload.

### 4. Closed Pipe buffer (critical)

1. Place a `PipeItemsClosed` at a dead end (no outputs) and feed items in. Verify:
   - no `EntityItem`s spawn on the ground;
   - the GUI shows the buffered stacks;
   - a gate with **Pipe Closed (Storing Items)** activates.
2. Fill more than 9 stacks with distinct items. Verify the **oldest** stack is evicted and the **newest** stack is retained; no double loss.
3. Break the pipe. Verify **all buffered stacks drop** as item entities.
4. Extract via the GUI and shift-click. Verify counts and no NPE.
5. Reload the world. Verify the buffer persists and no stale/duplicate slots appear.
6. Test with NBT-bearing items (e.g. enchanted books) to confirm only matching NBT merges.

### 5. Distribution Pipe (critical)

1. Place a distributor with three outputs and set weights (e.g. 10 / 1 / 1). Feed 12 items; verify the distribution is approximately 10/1/1.
2. While items are flowing, wrench-disconnect the currently selected output. Verify **no item entities drop** and items use a remaining valid output (or bounce).
3. Set all weights to 0. Verify items are routed/bounced normally, not voided.
4. Reload the world; verify weights and the current side persist.
5. Click the middle value button; verify it does nothing.
6. Click `+` several times quickly; verify each click is applied (optimistic client update) and the server value matches.
7. Send a forged distributor packet from a pipe more than 8 blocks away / aimed at another pipe type; verify it is rejected without a `ClassCastException`.

### 5b. Power Teleport (high)

1. Pair two power teleport pipes on a frequency; feed one and connect the other to an energy consumer.
2. Set `powerTransmittance` to 50, 90, and 100; verify the consumer receives at most the configured fraction and never more than the input.
3. Fill the consumer to capacity; verify the teleport pipe returns 0 and does not void energy elsewhere.
4. Put a non-power pipe (item/fluid) next to a power teleport pipe; verify no `ClassCastException` and no power is sent.
5. Compare a small packet (< receiver count) with a diamond power pipe; verify no energy multiplication.

### 6. Teleport ownership and range (high)

1. Player A places and configures a private teleport pipe; player B right-clicks it. Verify **Access denied** and no GUI.
2. Verify a creative player can modify it.
3. Move more than 8 blocks away from an open GUI (or have a second player attempt an update). The update should be rejected by the container/GUI and by `MessageTelePipeUpdate`.
4. Place a pipe without an owner (e.g. via automation/fake player, or a crafted tile NBT with no `ownerUUID`) and interact with it. Verify **no NPE** in the log; the first real player to open it claims it.
5. Corrupt a pipe's `ownerUUID` / set a negative frequency / set `state` above 3 in NBT (NBTExplorer), load the world, and verify the pipe loads without crashing and with clamped values.

### 7. Water Pump Pipe (high)

1. Place a Water Pump Pipe on a water source with a fluid pipe output into a tank. Verify water flows.
2. On a client, watch for flicker or phantom fluid in the pump pipe. The client should not be filling its own transport.
3. Test in a dedicated server + client setup, not only the integrated server.

### 8. Messages and GUI (high)

1. Open a teleport pipe GUI. Verify owner, frequency, state, public flag, connected count, and the first three connected positions.
2. Change frequency/state/public from the GUI; verify the server applies it and the client updates.
3. On a channel with **more than 9** connected pipes, verify the GUI does not crash (the network list is capped).
4. Verify no client-side `ClassCastException`/NPE when opening a GUI for a pipe whose tile was removed or replaced.

### 9. Commands

1. `/additionalpipes teleport` (no argument): no crash, usage shown.
2. `/additionalpipes teleport items`, `fluids`, `power`, `logistics`: correct list for each type (power must not list fluids), with dimension and owner.
3. `/additionalpipes teleport nonsense`: no crash.

### 10. Addition Pipe (high)

1. Chest A already contains item X; chest B is empty. Feed X and a different item Y through an Addition Pipe.
2. Verify X can enter A and Y is not force-inserted into B/voided.
3. Wrench-disconnect A; verify the pipe does not insert through the disconnected side.

### 11. Pipe tooltips

1. Open the creative inventory/NEI and hover every Additional Pipes pipe.
2. Verify each shows a useful one-line description, and that holding Shift adds the extra details.
3. Verify the rate line:
   - fluid pipes show a **cyan** `N mB/t` line;
   - power pipes show a **red** `N RF/t` line;
   - item pipes show neither.
4. Check the configured rates:
   - Fluid Teleport = diamond fluid pipe rate;
   - Power Teleport = diamond power pipe rate;
   - Switch Fluid = gold fluid pipe rate;
   - Switch Kinesis = average of iron and gold power rates;
   - Water Pump = the configured `waterPumpWaterPerTick`.
5. Switch the game language to one without AP translations (e.g. `de_DE`); verify the English tooltip is used and no `tip.` key appears literally.
6. Verify there are no duplicate description/rate lines (AP pipes use their own tooltip, not BuildCraft's pipe tooltip).
7. Verify the teleport tooltips now describe frequency, send/receive, ownership, and (for logistics) the one-partner rule.

### 12. Regression pass

Smoke-test the remaining pipes:

- item/fluid/power teleport pairs;
- switch pipes on redstone;
- gravity feed pipe;
- water pump;
- distributor;
- closed pipe;
- addition pipe.

Also run `./gradlew runClient` and a dedicated server once, and check the log for missing textures/classes, especially after the recent pipe removals.

## Known open issues (not fixed in this pass)

- The distribution GUI has no automated client test; the rapid-click and middle-button checks above are manual.
- LP provider fixes compile and LP is now a test dependency; the headless server still does not load LP, so the LP-present path needs a manual server/client check.
- The remaining items are optimizations/refactors/features listed in `CODEBASE_REVIEW.md`; no known correctness bug from the original review remains open.

## If an in-game check fails

Capture:

- the full log section around the failure;
- the exact pipe types/frequencies/state and whether the chunk was unloaded;
- client vs dedicated server;
- the pipe NBT if it persists across reload (`/data` is not available in 1.7.10; use an NBT editor on the region file or reproduce from a clean pipe).
