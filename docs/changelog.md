# Changelog

This page mirrors key datapack milestones from the main project changelog.

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
