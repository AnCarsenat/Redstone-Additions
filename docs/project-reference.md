# Project Reference

Where every generated path comes from. Paths below are relative to
`redstone_additions_kore/src/main/kotlin/ra/`.

**Rule of thumb:** the Kotlin package mirrors the generated path. `ra_gates:blocks/clock/process`
is written by `modules/gates/Clock.kt`; `ra_lib_multiblock:io/at` by `multiblocklib/io/Io.kt`;
`ra:tools/wrench/init_registry` by `tools/wrench/WrenchRegistry.kt`. When the name is not
enough, grep for the path string, which appears once in a `ref(...)` or `block.fn(...)`:

```bash
grep -rn '"wrench/init_registry"\|"tools/wrench/init_registry"' src/main/kotlin
```

## Top level

| File | Writes / does |
| --- | --- |
| `Main.kt` | Entry point: `RedstoneAdditions.build(output).generate()` |
| `RedstoneAdditions.kt` | `VERSION`, `PACK_NAME`, `modules`, `parts`, `pack.mcmeta`, the validator call |

## Foundation: `core/` and `api/`

| File | Contents |
| --- | --- |
| `core/Ns.kt` | `enum Ns`: every namespace the pack writes |
| `core/Ids.kt` | `Obj` (objectives), `Tag` (entity tags), `Storage` (`storage ra:*`) shared by more than one part |
| `core/Fn.kt` | `ref`, `define`, `fn`, `raw`, `hook` |
| `core/Ra.kt` | `PackPart`, `GameplayModule`, `TagRoute`, `WrenchCycle`, `GiveBundle`, the `Ra` registry, `afterAllParts` |
| `core/PackValidator.kt` | Generation-time check for calls to undefined functions |
| `core/Exec.kt` | `executeRun`, `executeReturnRun`, `executeStoreRun`, `callWith`, `returnRun`, `macroCall`: calls behind a vanilla-text chain, the function still a value |
| `core/Messages.kt` | `RA_CHAT_PREFIX`, `raMessage`, `moduleLoaded`, `registeredLine`, `blockRegisterMessage` |
| `core/Scores.kt` | `ensureScore` (start a score only when it has none) |
| `core/item/` | `ItemSpec` (+ `fromSnbt`), `itemSpec { }`, the `Snbt` value classes (`SStr`, `SBool`, `SByte`, `SRaw`...), `loreLine`, `raData`, `flag`, `parseSnbt`; `CustomBlock.loreItem`, `entityDataSnbt` |
| `core/block/CustomBlock.kt` | `CustomBlock`: one placeable block described once |
| `core/block/BlockFunctions.kt` | `blockGive` |
| `core/block/BlockBreak.kt` | `BreakTest`, `breakCheck`, `forEachMarker`, `blockTick`, `blockOnBreak`, `dropBlockItem`, `dropItem`, `killVanillaDrop`, `breakSound`, `breakCloud`, `STONE_BREAK_SOUND` |
| `core/block/Recipes.kt` | `RecipeResult`, `Unlock`, `shapedRecipe`, `recipeUnlock`, `blockRecipe` |
| `core/block/Placement.kt` | `placementGuard`, `placeBlock`, `clearNewTag`, `finishPlacement`, `setOnNew`, `defaultProperty` |
| `core/block/Redstone.kt` | `outputShell`, `readProperty` |
| `core/block/Selectors.kt` | `markers`, `brokenMarkers`, `newMarker`, `soundListeners`; as text: `markerSel`, `newMarkerSel`, `soundListenersSel`, `particleViewersSel` |
| `core/block/GogglesLines.kt` | `gogglesHeader`, `propLine`, `dataLine`, `textLine`, `stackReset`, `stacked*Line` |
| `api/LibApi.kt` | `RaLib` (every public `ra_lib` entry point), `MultiblockLib` (+ `Hooks` tag names) |
| `api/ToolsApi.kt` | `Goggles` (tick, dispatch, billboard writers), `Tools` (registries, per-tick), `Hub` (`ra:uninstall`), `Settings` (read API, lifecycle), `ModulesApi` (cross-module calls) |

## `ra_lib` → `lib/`

`lib/RaLibPart.kt` calls each sub-system's registration; the package defines every reference in
`RaLib` ([`api/LibApi.kt`](#foundation-core-and-api)), in one folder per sub-system mirroring
`ra_lib:<sub-system>/…`.

| Folder | Writes | Notable files |
| --- | --- | --- |
| `placement/` | `placement/place`, `set_block*`, `init` | `Placement.kt` |
| `orientation/` | `orientation/get_facing`, `set_facing`, `init` | `Orientation.kt` |
| `redstone/` | `redstone/*` readers, `facing/*`, `local/*`, dust/torch rules, the four `#ra_lib:redstone/*` block tags | `Sides.kt` (side data tables), `SideReaders.kt`, `Rules.kt`, `Detect.kt`, `Local.kt`, `RedstoneTags.kt` |
| `inventory/` | `inventory/*` | `Inventory.kt`, `InsertOrDrop.kt`, `Remove.kt` |
| `transport/` | `transport/*`, `net/*`, `rebuild/*` | `Transport.kt`, `TransportCommands.kt`, `net/`, `rebuild/` |
| `skin/` | `skin/*` | `Skin.kt`, `Spawn.kt` (26.3 `block_state` `{id, properties}`) |
| `input/` | `input/*`, trigger and writable-book backends | `Input.kt`, `InputIds.kt`, `backend/`, `router/`, `session/` |
| `util/` | `util/property` | `Property.kt` |
| `light/` | `predicate/light/at_least_1..15`, generated from one loop | `LightPredicates.kt` |
| (root) | `#ra_lib:containers`, `passable`, `unbreakable`; macro-line helper | `BlockTags.kt`, `LibCommands.kt` |

## `ra` → `tools/` and `hub/`

The `ra` namespace is split between two parts by path.

### Tools (`ra:tools/*`, tool items)

| File | Writes |
| --- | --- |
| `tools/ToolsPart.kt` | Order of registration; the `tools` give bundle |
| `tools/ToolItems.kt` | `ToolItem`: each tool item described once, as an `ItemSpec` (give argument, recipe result, bundle NBT) |
| `tools/ToolResources.kt` | Tool recipes + `recipes/unlock_*`, `tools/*_use` advancements, `is_sneaking` / `is_sprinting` predicates |
| `tools/ToolsDsl.kt` | `tool(path)`, `RAY_TARGET`, `raycast`, `REACH_4` |
| `tools/BlockName.kt` | `ra:tools/block_name` |
| `tools/goggles/GogglesTool.kt` | `ra:tools/goggles/*` (tick, scan, `draw_block`, `draw_multiblock` from the registry) |
| `tools/goggles/billboard/BillboardLines.kt` | `ra:tools/goggles/billboard/{prop,data,text,item,stacked_*}_line`, `stack_reset`, `stack_next` |
| `tools/goggles/billboard/BillboardRender.kt` | `handle_billboard`, `handle_multiblock`, `show_io_line`, `show_input/output/control`, `render_*`, `init_offsets` |
| `tools/goggles/status/Status.kt` | `ra:tools/goggles/status/read_status_multiblock` |
| `tools/wrench/Wrench.kt` | Wrench click handling, plain-RMB `found_block` (toggles from the registry) |
| `tools/wrench/WrenchMenu.kt` | Shift+RMB cycle menu, `menu_action` |
| `tools/wrench/WrenchMultiblock.kt` | Assembly on multiblock bases |
| `tools/wrench/WrenchRegistry.kt` | `init_registry` from `ra.wrenchCycles`; the UNI gate cycler |
| `tools/readonly/Readonly.kt` | `ra:tools/readonly/init_registry` from `ra.readonly` |
| `tools/datahandler/DataHandlerRegistry.kt` | `ra:tools/data_handler/init_registry`: property list, numeric list, item-name list |
| `tools/datahandler/DataHandler.kt`, `DataHandlerActions.kt`, `props/*` | The Data Handler |
| `tools/creativedatahandler/*` | The Creative Data Handler |
| `tools/inputdatahandler/InputDataHandler.kt` | The debug Input Data Handler |
| `tools/clipboard/Clipboard.kt` | The Clipboard |
| `tools/multimeter/Multimeter.kt` | The Multimeter |

### Hub (everything else in `ra`, plus `minecraft`)

| File | Writes |
| --- | --- |
| `hub/HubPart.kt` | Order of registration; `ra:settings` |
| `hub/Load.kt` | `ra:load`, `#minecraft:load` |
| `hub/Tick.kt` | `ra:tick` |
| `hub/placement/Placement.kt` | `ra:placement/detect_bats`, `process_bat`, `#ra:placement_handlers`, `#ra:custom_blocks` |
| `hub/items/Bundles.kt` | `ra:give_all_items`, `ra:items/bundles/*` from `ra.bundles` |
| `hub/uninstall/Uninstall.kt`, `Confirm.kt` | `ra:uninstall`, `ra:uninstall/*` |

## `ra_settings` → `settings/`

| File | Writes |
| --- | --- |
| `settings/SettingsPart.kt` | Order of registration; runs the generator |
| `settings/pages/Row.kt` | `Page`, `Scope`, the row types |
| `settings/pages/*Page.kt` | One page each (was `tools/settings/<page>.json`) |
| `settings/pages/SettingsPages.kt` | `SettingsPages.all`: the page order |
| `settings/gen/*` | What `tools/settings_gen.py` generated: menu registry (`pages`), `defaults`, `sync`, the `admin/**` tree, `admin_actions`, `admin_pages`, `blockmap`, `uninstall` |
| `settings/Lifecycle.kt` | `init`, `tick`, `input_tick`, `hooks`, `api` |
| `settings/Read.kt` | `get`, `prop`, `user`, `enabled`, `user_str`, `user_str_at` |
| `settings/Menu.kt`, `page/`, `row/`, `act/`, `Edit.kt`, `apply/`, `ApplyStr.kt` | The `/trigger` menu runtime |
| `settings/Admin.kt` | The operator panel runtime |
| `settings/Disabled.kt` | `ra_settings:disabled` |
| `settings/placement/PlacementFns.kt` | `placement/check`, `refuse`, `seed` |
| `settings/Fns.kt`, `SetCommands.kt`, `SettingsObjectives.kt` | Shared references, command helpers, private objectives |

See [Settings](settings-internals.md).

## `ra_lib_multiblock` → `multiblocklib/`

| File | Writes |
| --- | --- |
| `multiblocklib/MultiblockLibPart.kt` | Order of registration |
| `multiblocklib/Registry.kt` | `init`, `register`; declares the six empty hook tags |
| `multiblocklib/Assembly.kt` | `try_assemble`, `try_tier`, marker creation and setup |
| `multiblocklib/Validation.kt` | `validate_all`, `validate_single`, `disassemble` |
| `multiblocklib/build/Build.kt` | `build/*`: rotates registered structures into the other three facings |
| `multiblocklib/structure/Structure.kt` | `structure/*`: checks every required block |
| `multiblocklib/generic/Generic.kt` | `generic/*`: validator / setup / check for registry-described types (hooked last via `afterAllParts`) |
| `multiblocklib/io/Io.kt` | `io/*`: named IO positions |
| `multiblocklib/MbCommands.kt` | Helpers for `storage ra:multiblock` |

See [Multiblocks](multiblock-library.md).

## Gameplay modules → `modules/<module>/`

Each module has `<Name>Module.kt` (the `GameplayModule` object: registration order, `load`,
`tick`, bundle) and one file per block.

| Package | Notable files |
| --- | --- |
| `modules/gates` | `Clock.kt` (the reference block), `UniGate.kt`, `Delayer.kt`, `Extender.kt`, `Randomizer.kt`, `Shortener.kt`, `GateCommon.kt` (`timingOnBreak`, `timingTick`, `gateGoggles`), `debug/` |
| `modules/wires` | `WiresBlocks.kt` (every item, recipe, bundle, wrench rows, read-only), `blocks/Placement.kt` + `PlacementSpecs.kt` (one shared handler), `blocks/<block>/`, `electric/`, `fluid/`, `liquid/`, `bridge/`, `media/`, `common/` (`Line.kt`, `ConduitModel.kt`), `Lifecycle.kt` |
| `modules/multiblock` | `blocks/Bases.kt` + `BaseTier.kt`, `blastforge/`, `upgradeplatform/`, `drills/RockMetallicDrill.kt`, `goggles/ShowIoLabel.kt` |
| `modules/jetpacks` | `Jp.kt` (identifiers), `Kits.kt`, `kit/` (`FitModifiers.kt` generates `fit_<tier>_<n>`), `flight/`, `mode/` |
| `modules/infinite` | `Generator.kt` (shared generator shape), one file per generator, `InfiniteItems.kt`, `InfiniteTags.kt` |
| `modules/enchanting` | `EnchantingModule.kt`, `RecipesTag.kt` (`#ra_enchanting:recipes`), `SacrificeRecipe.kt`, `sacrifice/` |
| `modules/chunkloader` | `ChunkLoader.kt` |
| `modules/interactive` | one file per block (`BlockBreaker.kt`, `BlockPlacer.kt`, `Breeder.kt` + `BreederBreeds.kt`, `ItemPipe.kt` + `ItemPipeTransfer.kt`, `ItemMover.kt` + `ItemMoverIo.kt`, `BigTorch.kt` + `BigTorchSweep.kt`, `Cauldrons.kt`, `MagicCrate.kt`, `MessageBlock.kt`, `Spitter.kt`), `EnchantedCoal.kt`, `InteractiveCommon.kt` |
| `modules/storage` | `Boxer.kt`, `Unboxer.kt`, `StorageBox.kt` |
| `modules/sensors` | `EntityDetector.kt`, `TagAdder.kt`, `TagRemover.kt`, `EdgeTrigger.kt` |
| `modules/wireless` | `Emitter.kt`, `Receiver.kt`, `WirelessCommon.kt`, `tools/Remote.kt` |
| `modules/ender` | `ItemVault.kt`, `FluidVault.kt`, `PowerVault.kt`, `TeleportAnchor.kt`, `EnderCommon.kt`, `link/Link.kt`, `tools/Anchor.kt`, `debug/` |

## Others

| Package | Writes |
| --- | --- |
| `migrations/` | `ra_migrations:run` and one function per version step (`5.1.7-to-5.1.8`…) |
| `advancements/` | The `ra_advancements` tab: `CoreTrees.kt`, `ResourceTrees.kt` as data, `Unlock.kt` turns each entry into a file |
| `example/ExamplePart.kt` | `ra_example`: the worked example behind [Extension Examples](extension-examples.md) |

## Outside `src/`

| Path | What |
| --- | --- |
| `build.gradle.kts` | Kore plugin config: `packName`, `mainClass`, the `-Pworld` install target, system properties (`ra.strict`, `ra.version`) |
| `local.properties.example` | Template for the untracked `local.properties` (`ra.world`, `ra.minecraftDir`) |
| `gradle.properties` | `version` (the pack version) and Gradle flags |
| `gradle/libs.versions.toml` | Kore and Kotlin versions |
| `pack.png` | The pack icon |
| `tools/compare_reference.py` | Diff against the beet build |
