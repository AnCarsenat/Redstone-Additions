# Changelog

This page mirrors key datapack milestones from the main project changelog.

## v5.1.4 (2026-08-16) — Transport Rewrite, Item Safety, Library Audit

A large maintenance release. The fluid and gas system was rebuilt on a shared
network engine, several item-destroying and item-duplicating bugs were fixed, and
a full audit pass removed dead code and a class of per-tick performance problems.

**Breaking:** redstone control on the Boxer and Unboxer is inverted — unpowered
runs, powered pauses. See *Changed*.

**Supported versions:** 1.21.9 – 1.21.10 (data pack format 88). The pack declares
only the range it is tested against. Most of the content does not need 1.21.9 —
see the compatibility table on the home page for what breaks on older versions.

### Added

**Transport engine**
- `ra_lib:transport` — a shared network engine. Adjacent nodes are grouped by
  flood fill, recomputed only when something is placed or broken and debounced to
  at most one rebuild every 5 ticks. Per-network totals live in scoreboards; the
  medium is a string in storage. Fluid and item networks both use it.

**Blocks**
- **Boiler** — water in one side, steam out the other, over any block in
  `#ra_wires:heat_sources`.
- **Solar Panel** — generates EU from sky light, read from the vanilla daylight
  detector's own `power` state, so night, rain, roofs and snow cover all work
  without a custom predicate.
- **Rock Metallic Drill** — rebuilt on the new multiblock API and actually
  reachable; it was previously registered in no tag and internally broken.

**Multiblock authoring**
- A structure is declared **once**, facing north, in
  `ra_multiblock:register_types`; the library derives the other three facings.
- IO helpers `ra_lib_multiblock:io/{at,insert,extract,peek,count,is_block}` give
  named access to a multiblock's inputs and outputs, so tick logic never branches
  on facing. They work for the existing multiblocks too.
- Generic `validate`, `check_structure` and `setup_type` hooks; a registered type
  only needs `load` and `tick` entries.
- `ra_lib_multiblock:try_tier` — the wrench asks the registry for a tier instead
  of naming one hardcoded type, so adding a multiblock never edits the wrench.
- `ra_lib_multiblock/README.md` documents the whole API.

**Library**
- `ra_lib:inventory/insert_or_drop` — insert what fits, drop the rest. `loot
  insert` silently destroys the remainder.
- `ra_lib:inventory/move_slot` — whole-slot transfer via `/item replace block …
  from block …`, plus `find_free_slot`, `has_free_slot` and `container_size`.
- `ra_lib:redstone/count_inputs` — counts sides carrying a redstone source,
  powered or not.
- `ra:tools/block_name` — resolves a block's display name from the block itself.
- Goggles line helpers `prop_line`, `data_line` and `text_line`.
- Drain **"place" mode**, cycled with the goggles tinker: spends network contents
  putting source blocks back into the world.
- Infinite-source rule: nine or more matching sources within two blocks counts as
  inexhaustible; anything smaller genuinely empties.

### Changed

- **Redstone on the Boxer and Unboxer now LOCKS rather than starts them.** Both
  are vanilla dispenser/dropper blocks, and a powered dispenser fires its own
  contents — so requiring power to run also threw the contents on the floor.
  Existing builds that pulse these blocks must have the signal removed.
- **Fluid contents belong to the network, not to each node.** Pipes, tanks and
  valves do no per-tick work; only pumps, drains and boilers tick.
- **Media are strings.** `medium_id` 1/2/5 and the `+10` gas offset are replaced
  by a registry keyed by name holding display name, state, colour, particle,
  world block and bucket.
- **EU Generator burns steam** instead of producing power from nothing.
- **A closed valve splits its network**, so the two halves keep separate
  contents and separate media.
- **Every block owns its goggles readout and its display name**, in
  `blocks/<name>/goggles.mcfunction`. `draw_block` is pure routing.
- **Electric wires are visually distinct from fluid pipes** — a 0.26 core in
  concrete against the pipes' 0.56 metal. An L1 wire and an L1 pipe were
  previously the same copper block at the same size.
- Item pipes move whole stacks and cache their filter frame.
- Added `type=` to 385 entity selectors; the tick graph had 324 untyped ones,
  each of which walked every loaded entity.
- `ra_lib:redstone/detect/dust` gates each direction on one connection test:
  128 commands per call became 8 when no dust is adjacent.
- UNI Gate writes its 3x3x3 output shell only on a change of result, with a
  periodic resync.
- RA Wires one-time migrations are version-gated instead of re-running forever;
  placement is one spec per block instead of sixteen near-identical copies.
- Goggles scanning collects markers in range once and draws each one.
- The Redstone Remote's channel is set through the shared writable-book prompt,
  which needs no command permissions.

### Fixed

**Item loss and duplication**
- **A full output container destroyed the overflow** in the Unboxer, the Blast
  Forge and every multiblock IO insert.
- **The Unboxer duplicated items** — it inserted into the output before removing
  from the box, so any path reaching one and not the other produced both.
- **The Unboxer threw the box on the floor**, via the vanilla dispenser trigger.
- `ra_lib:inventory/remove` silently failed past slot 8 — every container larger
  than a dispenser — and reported success anyway. It now handles any container
  size and amounts split across stacks, all-or-nothing.

**Fluid and gas**
- **Pumps did nothing.** They probed `^ ^ ^1`, local coordinates on a marker
  whose rotation is always the default, so a pump could only ever see the block
  to its south.
- **Pumps fabricated fluid** from nothing when no source was found.
- **Propagation was order-dependent and slow** — one block per tick, with the
  result depending on entity iteration order.
- Liquid drain, liquid pump and gas pump had no facing.
- Reopening a gas valve gave it the liquid valve's capacity.

**Interface**
- **Data Handler [Toggle] did nothing.** Both boolean toggles read the property
  again on the line after writing it, so the pair always undid itself.
- **Data Handler reported RA Wires blocks as "Unknown Block"** while reading
  their properties correctly. Each handler had its own name table and the two had
  drifted to 22 and 38 entries.
- **Item pipe filters never matched.** A frame attached to a block sits in the
  neighbouring block, beyond the 0.75 search radius — and a wider radius cannot
  tell whose filter it is. Bound by the frame's `block_pos` now.
- **The Redstone Remote could not be retuned without cheats**; its channel menu
  suggested a `/function` command.
- **The Input Form book vanished when selected.** Dropping the book now cancels
  the request.

**Rendering**
- **Z-fighting on every connected pipe and wire pair** — both nodes drew a
  full-length bar over the same volume. Each now draws only its own half.
- **Connections left dangling next to a destroyed block**, because neighbours
  were refreshed while the dying marker still advertised itself as a node.
- Clock and UNI Gate item displays floated half a block above their block;
  offsets are measured from the block centre, not its floor.

**Other**
- Load message reported v5.1.2 while the pack reported v5.1.3.
- `ra_lib_multiblock:create_marker` could set up an unrelated marker anywhere in
  the world — its selector had no type or distance limit.
- UNI Gate AND/NAND treated unpowered repeaters, comparators, torches and buttons
  as absent inputs.
- Goggles drew every billboard twice when two wearers stood near the same block.
- `ra_lib:placement/place` inherited the previous placement's facing when no
  placer was nearby.

### Removed

- `ra:crafting` — never initialised, never called.
- Dead `ra.custom_block.gas_pipe` handling; the tag is stripped during migration
  before any of it could match.
- `enabled` on liquid pipes, liquid tanks and gas tanks. Nothing read it, so it
  appeared in the Data Handler as a toggle that changed nothing.
- Unused objectives `ra.channel`, `ra.edit_step`, `ra.inv.slot`, `ra.mb_enabled`.
- Redundant stone-button pass in redstone detection, eight no-op tag sweeps in
  `ra_wires:common/tick_cleanup`, and the inert `#ra_gates:*` / `#ra_multiblock:*`
  function tags.

### Known issues

- The **Pusher** is offered in the Interactive bundle and recognised by the data
  handlers, but has no placement handler or tick logic — placing it does nothing.
- Blast Forge and Upgrade Platform still use hand-written per-facing coordinate
  tables rather than the new multiblock registry.
- This release has not been play-tested in game.

## v5.1.3 (2026-04-22)

### Added
- Chunk Loader status display support, including the new `draw_display_chunk_loader` renderer.
- Goggles status rendering for Chunk Loader.

### Changed
- Updated Item Pipe processing behavior for improved handling.

### Fixed
- Fixed reload command-limit issues by scheduling `ra:tick` for the next tick.
- Fixed RA multiblock recipe definitions by removing unused crafting key symbols.

## v5.1.2 (2026-04-09)
### Changed
- Renamed all "message" blocks to "message_block"
- Renamed all "rand" blocks to "randomizer"
## v5.1.2 (2026-04-08)

### Changed

- Refactored internal datapack structure for maintainability across modules.
- Normalized liquid and gas transport tiers back to copper/iron naming.
- Updated transport and multiblock recipe definitions to match tier renames.

### Fixed

- Corrected multiblock base recipe regressions introduced during transport refactors.
- Updated docs deployment workflow actions to current GitHub Actions releases.

### Removed

- Removed the experimental Pusher block from the active interactive release set.

### Docs

- Reworked and reorganized docs for GitHub Pages/MkDocs publishing.
- Refreshed block reference and recipe imagery across core module pages.
- Added storage page coverage and improved interactive/storage documentation clarity.

## v5.1.1 (2026-04-07)

### Added

- New `ra_storage` namespace with Boxer/Unboxer runtime, recipes, and storage-box workflows.
- Added `ra:items/bundles/give_storage_bundle` for direct storage bundle handout.

### Changed

- Core load/tick now initialize and dispatch `ra_storage`.
- Placement handler registry now includes Boxer and Unboxer.
- Creative Data Handler now identifies Boxer and Unboxer markers by name.
- Goggles module `draw_displays` wrappers were removed; dispatch is now centralized in `ra:tools/goggles/scan_blocks` and `ra:tools/goggles/scan_multiblocks`.
- Sensor goggles wrapper files were inlined into the core scanner flow.
- `ra:give_all_items` now gives categorized namespace bundles directly instead of loose item spam.
- Added `ra:items/bundles/give_all` as the direct bundle-kit entrypoint.
- Each gameplay namespace now has a prefilled bundle for faster test access.
- Goggles overlays are now block-defined: billboards render only when blocks explicitly opt in via `show_name` / `show_status`.

### Fixed

- Restored backward-compatible goggles name rendering when callers only provide `billboard.name`.
- Removed UTF-8 BOM from refactored tick functions to prevent line-1 parse failures on reload.

### Docs

- Updated Home and Developer Guide command notes for storage namespace/bundle coverage.
- Updated architecture docs for centralized goggles scanner dispatch and compatibility behavior.

## v5.1.1 (2026-04-05)

### Added

- New `ra_wires` module for transport/electric gameplay.
- Liquid network blocks: copper/netherite pipes, tank, pump, valve, and drain.
- Gas network blocks: copper/netherite pipes, tank, pump, and valve.
- Electric network blocks: copper/netherite wires, EU generator, EU consumer, and EU switch.
- Shared transport helper `ra_lib:transport/update_connection_status` for neighbor status updates.
- Goggles transport overlays and sneaking tinker interactions for nearest `ra_wires` block.
- Full recipe unlock advancements and get-item advancements for all `ra_wires` items.

### Changed

- Core load/tick and give-all flows now dispatch `ra_wires`.
- Placement handler registry now includes `ra_wires:blocks/handle_placement`.
- Uninstall flow now removes `ra_wires` scoreboards, tags, and storage state.
- Documentation pages now include transport network architecture and extension notes.

### Fixed

- Corrected transport score-match syntax in liquid/gas transfer logic.
- Added explicit liquid drain fallback states and particles when world drain is not possible.

## v5.1.1 (2026-04-04)

### Changed

- Logic gates and wireless emitter migrated to direct `ra_lib:redstone/detect` usage.
- Removed runtime dependency on legacy gate signal objectives (`ra.act_red`, `ra.inac_red`).
- Removed placement-time legacy tag wiring for redstone sweep participation.
- Data Handler text input flow now uses inventory-safe Input Form distribution.
- Writable-book restore flow now uses dedicated slot/inventory/offhand helper functions.
- Request-scoped dropped Input Form cleanup runs in scan and restore paths.
- Removed obsolete writable-book helper files no longer used by active runtime paths.

### Docs

- Rewrote gate and wireless documentation around the unified `ra.power` (`0..16`) model.
- Updated architecture/contributor docs to reflect per-block redstone detection flow.
- Updated Home, Block Reference, Developer Guide, and How It Works with Data Handler/input runtime details.

## v5.1.1 (2026-04-03)

### Added

- New architecture deep-dive page with full runtime flow and lifecycle diagrams.
- Advancement coverage for refactored redstone component recipes.

### Changed

- Recipe and progression cleanup across redstone component recipes.
- Unified Delayer/Extender/Shortener recipe path around quartz block progression.
- README and changelog documentation refresh for the v5.1.1 release.

### Fixed

- Fixed remote advancement criteria mismatch.
- Fixed Blast Forge text clipping inside the forge.

## v5.1.1 (2026-02-24)

### Added

- Goggles tool with nearby status rendering for custom blocks and multiblocks.
- Blast Forge heat system with fuel tiers and heat-scaled processing speed.
- Clock crafting recipe.
- Uninstall confirmation flow (`/function ra:uninstall` -> confirm/cancel).
- Formal project conventions in `GUIDELINES.md`.

### Changed

- Naming and registration consistency improvements across modules.
- Cleanup of debug/test give output in core give-all flow.

### Fixed

- Removed references to non-implemented beamer content.
- Disabled stale conveyor recipe file.
- Removed stray namespace-local `pack.mcmeta` file.
- Fixed duplicated/misaligned objective setup and version references.

## v5.1.1 (2026-02-23)

- Macro-based multiblock directional architecture.
- Marker data initialization reliability improvements.
- Blast Forge structure and recipe updates.

## v5.1.1 (2026-02-23)

- Multiblock data model normalization (`inputs`, `outputs`, `properties`, `controls`).
- Blast Forge IO and structure refactor.

## v5.1.1 (2026-02-23)

- Introduced `ra_lib_multiblock` and `ra_multiblock` module foundations.
- Added multiblock bases and initial wrench assembly support.
- Switched wireless channels from numeric to string identifiers.

## v5.1.1 (Initial)

- Initial release of core modules, tools, and custom block families.

---
