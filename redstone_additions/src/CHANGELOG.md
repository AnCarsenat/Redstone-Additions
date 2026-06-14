# Changelog

## [v5.1.3] - 2026-04-22 - Chunk Loader Status + Stability Fixes

### Added
- Chunk Loader status display support, including the new `draw_display_chunk_loader` renderer.
- Goggles status rendering for Chunk Loader.

### Changed
- Updated Item Pipe processing behavior for improved handling.

### Fixed
- Fixed reload command-limit issues by scheduling `ra:tick` for the next tick.
- Fixed RA multiblock recipe definitions by removing unused crafting key symbols.

## [v5.1.2] - 2026-04-08 - Docs Refresh - Transport Cleanup

### Changed
- Refactored datapack function layout for readability and maintainability across modules.
- Normalized liquid/gas transport tiers back to copper and iron naming.
- Updated transport and multiblock recipe files to align with the renamed tiers.

### Fixed
- Fixed multiblock base recipe inconsistencies introduced during transport tier migration.
- Updated docs deployment workflow dependencies for stable GitHub Pages builds.

### Docs
- Reworked project documentation structure for GitHub Pages publishing.
- Refreshed block reference and recipe images to match current recipes.
- Renamed project references and expanded storage/interactive documentation coverage.

### Removed
- Removed experimental Pusher block content from the current interactive release set.

## [v5.1.1] - 2026-04-05 - Transport Networks - Creative QOL
### Added
- New `ra_wires` namespace for liquid pipes, gas pipes, and electric wire systems.
- Liquid blocks: copper/netherite pipes, tank, pump, valve, and drain.
- Gas blocks: copper/netherite pipes, tank, pump, and valve.
- Electric blocks: copper/netherite wires, EU generator, EU consumer, and EU switch.
- Shared helper `ra_lib:transport/update_connection_status` for local node connection state.
- Goggles support for `ra_wires` status overlays and sneaking tinker interactions.
- Full recipe and advancement coverage for all `ra_wires` items.

### Changed
- Core load/tick dispatch now includes `ra_wires:load` and `ra_wires:tick`.
- Global give-all now includes `ra_wires:items/give_all`.
- Placement handler registry now includes `ra_wires:blocks/handle_placement`.
- Uninstall cleanup now removes `ra_wires` scoreboards, tags, and storage state.
- `ra:give_all_items` now gives one prefilled bundle per namespace instead of loose item spam.
- Added `ra:items/bundles/give_all` and namespace bundle helper functions under `ra:items/bundles/*`.
- Goggles overlays are now block-defined: billboards only render for blocks that opt in with `show_name` / `show_status`.

### Fixed
- Fixed non-zero score checks in transport logic to valid score-match syntax.
- Added explicit fallback particles/status for liquid drain failure cases.

### Docs
- Updated wiki Home/Developer Guide command descriptions to reflect direct namespace bundles.
- Updated wiki architecture docs to document block-defined goggles rendering profiles.

## [v5.0.0] - 2026-04-04 - The survival update

### Added
- Added library for aqcuiring text input from non op players using writable books
- Added "suvrival" friendly data handler

### Changed
- Refactored writable-book input backend.
- Updated load messaging and root README version references to v5.1.1.

### Fixed
- Fixed Data Handler text-edit flow causing the editor item to disappear.
- Fixed temporary input books persisting as dropped entities after session transitions.
- Fixed full-inventory behavior so input books are not given when no slot is available.

### Removed
- CDH recipe

## [v5.1.1] - 2026-04-03

### Added

- Added advancement coverage for refactored redstone component recipes.
- New architecture documentation page in the wiki with full runtime flow details.

### Changed

- Reworked several crafting recipes and related advancements for cleaner progression.
- Replaced legacy Delayer/Extender/Shortener recipe ingredients with a quartz block progression path.
- Updated README and changelog documentation for the v5.1.1 release.

### Fixed

- Fixed remote advancement criteria mismatch (iron ingot requirement consistency).
- Fixed Blast Forge text display clipping inside the forge.
- Fixed Item pipes and item movers voiding items 😅

## [v5.1.1] - 2026-02-24

### Added

#### Goggles (New Tool)
- **New tool: Goggles** — wearable/holdable tool that reveals custom block info
  - Wear as helmet or hold in hand to scan nearby blocks (16 block range)
  - Shows block name billboards above each custom block
  - Shows block-specific status: gate mode, wireless channel, sensor target, clock period, delay
  - Shows multiblock I/O indicators with labeled inputs/outputs/controls
  - Shows blast forge heat status and enabled state
  - Crafting recipe: Copper Ingot + 2× Glass Pane + Gold Ingot
  - New files: `ra:tools/goggles/` (give, tick, scan_blocks, scan_multiblocks, billboard/, status/)

#### Blast Forge Heat System
- **Heat counter system** replacing flat 5-second timer
  - Heat stored per-forge in marker entity `data.status.heat`
  - Heat increases when fuel is consumed, decreases by 1 every 2 ticks passively
  - Processing only occurs when heat ≥ 100
  - Heat capped at 1000
- **Fuel tiers:**
  - Coal: +50 heat
  - Charcoal: +40 heat
  - Blaze Powder: +500 heat
  - Blaze Rod: +1500 heat (new fuel type)
  - Lava Bucket: +5000 heat (returns empty bucket)
- **Heat-based processing speed:**
  - Heat 100-299: process every 5 seconds
  - Heat 300-599: process every 3 seconds
  - Heat 600-899: process every 2 seconds
  - Heat 900+: process every 1 second
- **Visual feedback:** particle intensity scales with heat level (smoke → flames → lava drips)
- New file: `ra_multiblock:blast_forge/consume_fuel`

#### Clock Recipe
- Added crafting recipe for Clock block: Stone + Redstone + Clock item

#### Uninstall Confirmation
- `/function ra:uninstall` now shows clickable [CONFIRM] / [CANCEL] prompt
- `ra:uninstall/confirm` performs full cleanup:
  - Kills all custom blocks, multiblock markers, billboards, display entities
  - Removes all 26 scoreboards
  - Clears all data storage namespaces
  - Cancels scheduled ticks
  - Removes all player tags
- `ra:uninstall/cancel` sends cancellation message

#### Guidelines Document
- Added `GUIDELINES.md` with naming conventions, new block checklist, multiblock checklist, release checklist, and file templates

### Changed

#### Naming Convention Overhaul
- `ra_gates:load` now calls `register_block` for each gate block (matching `ra_interactive` and `ra_sensors` pattern)
- Created `register_block.mcfunction` for: UNI Gate, Clock, Delayer, Extender, Randomizer, Shortener
- Standardized header comments across all load/tick files with consistent format
- Organized `give_all_items.mcfunction` by category (Tools → Interactive → Gates → Sensors → Wireless → Multiblocks)
- Removed debug/test items (Output 1/2/3) from `give_all_items`

#### Version Updates
- Updated pack.mcmeta version to v5.1.1
- Updated load message to v5.1.1
- Updated README badge to v5.1.1
- Updated WIKI Home.md version to v5.1.1

### Fixed

#### Critical Fixes
- **Beamer ghost block:** Removed all beamer references (tick call, give_all call, advancement, CDH mapping, scoreboard). Beamer was never implemented — only had an advancement JSON
- **Conveyor recipe without implementation:** Disabled `conveyor.json` recipe (renamed to `.disabled`). The recipe produced Item Pipe items but no conveyor block exists
- **Stray pack.mcmeta:** Deleted `data/ra_wireless/pack.mcmeta` which should not exist inside a namespace folder
- **Duplicate ra.cooldown:** Removed duplicate `scoreboard objectives add ra.cooldown` from `ra:load` (kept in `ra_lib:placement/init` where it's used)
- **Version mismatches:** All version references now consistently say v5.1.1

### Removed
- `data/ra_advancements/advancement/ra_gates/get_beamer.json` — ghost advancement for unimplemented block
- `data/ra_wireless/pack.mcmeta` — stray file in namespace folder
- `data/ra_interactive/recipe/conveyor.json` → renamed to `.disabled`
- Debug items (Output 1/2/3) from `give_all_items.mcfunction`
- Beamer scoreboard (`ra.dir`) from `ra_gates:load`

---

## [v5.1.1] - 2026-02-23

### Changed

#### Macro-Based Multiblock Architecture
- **Eliminated 4× directional code duplication** using MC macro functions (`$` parameter substitution)
- Direction offsets stored in `storage ra:multiblock bf_dir.{north|south|east|west}` — each direction contains ~29 keys (positions, IO metadata)
- New macro functions: `validate_facing`, `check_facing`, `tick_facing`, `process_facing` — single implementation handles all 4 directions
- Dispatch files use 4-line pattern to resolve facing → macro call
- Deleted 9 obsolete per-direction files (validate/check_N/S/E/W, consume_and_output)

#### Marker Entity Data Fix
- Summon marker with `{data:{_init:1b}}` to prevent Minecraft from auto-removing empty `data:{}` compound
- `setup_marker` now uses atomic `data merge` to initialize all fields at once, then `data modify` to override from storage
- Fixes critical bug where multiblocks would assemble then immediately disassemble

#### Blast Forge
- Added **ancient debris** recipe: ancient_debris → 2× netherite scrap
- Updated wrench error message to reflect current structure requirements

### Fixed
- Fixed multiblock marker losing its `data` compound due to Minecraft removing empty NBT compounds
- Fixed non-atomic entity data initialization causing race conditions during assembly

---

## [v5.1.1] - 2026-02-23

### Changed

#### Multiblock System Overhaul
- **Standardized multiblock data structure** — All multiblocks now store:
  - `inputs` — Container positions (relative to base) for material/fuel inputs
  - `outputs` — Container positions for processed results
  - `properties` — Configurable properties (enabled, speed, tier)
  - `controls` — Redstone input/output positions for automation
- `setup_marker.mcfunction` now initializes all standard IO and control data from assembly
- Added `ra.mb_enabled` scoreboard for control state tracking

#### Blast Forge Reworked
- **Removed hopper** from structure — replaced with barrel-based IO
- **2 Inputs + 1 Output** barrel system:
  - Input 1 (material barrel): Raw ores, ore blocks, equipment to recycle
  - Input 2 (fuel barrel): Coal, charcoal, or blaze powder
  - Output barrel: Smelted/recycled results automatically inserted
- Structure now uses **blast furnace** instead of regular furnace
- Added **blaze powder** as valid fuel type
- **Redstone control**: Place a redstone block behind the base to lock/disable the forge
- **Block tag `#ra_multiblock:blast_forge_bricks`**: Accepts nether bricks, red nether bricks, cracked, and chiseled variants
- Process function now uses `ra_lib:inventory/insert` for proper output stacking

### Fixed

#### Blast Forge
- Fixed check_* functions checking from wrong position (hopper instead of base)
- Fixed inconsistency between validate (furnace) and check (blast_furnace) block types  
- Fixed missing `#ra_multiblock:blast_forge_bricks` block tag (was referenced but never created)
- Fixed structure validation checking from marker position correctly in all 4 directions

---

## [v5.1.1] - 2026-02-23

### Added

#### Multiblock System
- **New Module: `ra_lib_multiblock`** - Core library for multiblock structures
  - `init.mcfunction` - Initialize multiblock scoreboards
  - `try_assemble.mcfunction` - Entry point for multiblock assembly
  - `create_marker.mcfunction` - Spawn multiblock marker entity with epic effects
  - `setup_marker.mcfunction` - Configure marker with type-specific data
  - `validate_all.mcfunction` - Batch revalidate all multiblocks
  - `validate_single.mcfunction` - Validate individual multiblock
  - `disassemble.mcfunction` - Remove multiblock

- **New Module: `ra_multiblock`** - Multiblock handler and implementations
  - **Multiblock Bases** (5 tiers):
    - Copper Multiblock Base (Tier 1)
    - Iron Multiblock Base (Tier 2)
    - Gold Multiblock Base (Tier 3)
    - Diamond Multiblock Base (Tier 4)
    - Netherite Multiblock Base (Tier 5)
  - Recipes for all multiblock bases
  - Placement handlers with unique particle/sound effects per tier

- **Blast Forge Multiblock** (Copper Tier)
  - 3x3x3 structure: nether bricks shell + hopper + furnace + copper base
  - Auto-detects orientation (builds in any direction)
  - Ore doubling: raw iron/gold/copper → 2x ingots
  - Deep ore processing: iron/gold/copper ore → 2x ingots
  - Equipment recycling: tools/armor → nuggets based on durability
  - 5-second processing cycle with flame particles

#### Wrench Integration
- Right-click multiblock base with wrench to assemble
- Right-click assembled multiblock to toggle enabled/disabled
- Error feedback when structure is invalid

### Changed

#### Global Refactor: Entity-Local Properties
- All custom blocks now store properties in `@s data.properties` instead of `#variable ra.temp`
- Affected blocks: all gates, sensors, wireless, interactive blocks

#### Randomizer
- Added `chance` property (0-100%) for probability-based output
- Creative Data Handler support for chance modification

#### Wireless System
- Emitters, receivers, and remotes now use **string** channel identifiers instead of integers
- Allows named channels like "main", "door1", etc.

#### Tag Remover
- Simplified to use `entity_selector` (like Entity Detector) instead of separate `entity_type` + `range`

### Fixed

#### Tag Adder
- Fixed variable mismatch (`entity_type` vs `entity_selector`)
- Now properly uses stored selector from properties

#### Entity Detector
- Fixed distance check not anchored to block position
- Fixed self-detection issue (no longer detects its own armor stand)
- Added `at @s` anchoring and `unless entity @s[tag=ra.custom_block]` filter

---

## [v5.1.1] - Initial Release

- Core datapack structure
- Interactive blocks: Block Breaker, Block Placer, Spitter, Pusher, Conveyor, Breeder, Infinite Cauldrons, Message
- Sensor blocks: Entity Detector, Tag Adder, Tag Remover
- Gate blocks: Uni Gate, Clock, Delayer, Extender, Randomizer, Shortener
- Wireless blocks: Emitter, Receiver, Remote
- Tools: Wrench, Creative Data Handler
- Library modules: ra_lib (placement, orientation, inventory, redstone)
