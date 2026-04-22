# Developer Guide

This guide documents implementation architecture and contributor workflow for `v5.1.3`.

If you want conceptual runtime flow first, start with [How It Works](how-it-works.md). This page is focused on engineering-level extension and maintenance work.

If you want to contribute and write somewhat correct addons / fixes see [Contributing Guidelines](contributing-guidelines.md)
## 1) Core Entrypoints (`ra`)

Primary core functions:

- `ra:load`
- `ra:tick`
- `ra:give_all_items`
- `ra:uninstall`

`ra:give_all_items` now delegates to `ra:items/bundles/give_all`, which gives categorized prefilled bundles directly.

`minecraft:load` points to `ra:load`.

### `ra:load` responsibilities

- Initializes trigger and runtime scoreboards.
- Seeds shared storage in `ra:temp`.
- Initializes `ra_lib` and `ra_lib_multiblock`.
- Initializes all gameplay namespaces, including `ra_storage` and `ra_wires` transport networks.
- Starts main tick loop.

### `ra:tick` responsibilities

- Clears stale click tags.
- Runs modular input session processing and Data Handler action processing.
- Runs placement detection.
- Dispatches module ticks.
- Runs goggles scanning.
- Reschedules itself every tick.

## 2) Shared Library: `ra_lib`

`ra_lib` is the reusable systems layer.

### `ra_lib:init`

Calls module initializers:

- `orientation/init`
- `placement/init`
- `inventory/init`
- `redstone/init`
- `input/init`

### placement

Key functions:

- `ra_lib:placement/place`
- `ra_lib:placement/set_block`
- `ra_lib:placement/set_block_facing`
- `ra_lib:placement/set_block_simple`

Contract of `place`:

- Input macro fields: `block_id`, `block_tag`, `dir_type`.
- Resolves facing through orientation library.
- Places physical block.
- Summons marker with `ra.custom_block`, typed tag, and `ra.new`.
- Stores rotation/facing for downstream logic.

### orientation

Key functions:

- `ra_lib:orientation/get_facing`
- `ra_lib:orientation/set_facing`

`dir_type` behavior:

- `0`: no facing behavior
- `1`: horizontal facing only
- `2`: full directional (including up/down)

### redstone

Key function:

- `ra_lib:redstone/detect`

It computes:

- aggregate power (`ra.power`, range `0..16`)
- world-space directional power (`ra.power.north/south/east/west/up/down`)
- look-space directional power (`ra.power.front/back/left/right/local_up/local_down`)
- power tags (`ra.powered`, directional tags, strong tag)

Power level contract:

- `0` = no power
- `1..15` = normal redstone power
- `16` = superpower (direct powered repeater/comparator output into the block)
- `ra.powered.strong` is set only when `ra.power == 16`

Consumer model:

- Gates and wireless emitter now consume `ra_lib:redstone/detect` directly per marker tick.
- Legacy gate signal batching (`ra_gates:check_signals`) remains as a compatibility no-op and is not used by runtime tick flow.

Source-specific detectors are split under `ra_lib:redstone/detect/*`.

### inventory

Key functions:

- `ra_lib:inventory/insert`
- `ra_lib:inventory/remove`
- `ra_lib:inventory/clear`

`insert` uses loot insertion semantics for stack-safe behavior.
`remove` is slot-aware and returns success/failure for machine logic.

### transport

Key function:

- `ra_lib:transport/update_connection_status`

This helper computes immediate neighbor connectivity for transport nodes and writes compact status fields used by module visuals and goggles diagnostics.

### input

Key functions:

- `ra_lib:input/session/create`
- `ra_lib:input/router/select_backend`
- `ra_lib:input/router/open`
- `ra_lib:input/poll`
- `ra_lib:input/consume`
- `ra_lib:input/session/cleanup`

Backends:

- `trigger`: numeric input and range validation.
- `writable_book`: text capture from page 1.

Runtime behavior:

- Per-request state is stored under `storage ra:input` (`sessions.req_<id>`).
- `give_book_safe` only gives an Input Form when inventory has room.
- Full inventory produces a red user warning and skips book give.
- Dropped request books are cleaned through request-aware selectors.

## 3) Shared Multiblock Library: `ra_lib_multiblock`

`ra_lib_multiblock` provides generic lifecycle management for all multiblock types.

Initialization:

- creates `ra.multiblock`, `ra.mb_timer`, `ra.mb_enabled`
- prepares `ra:multiblock` and temp storage branches

Lifecycle functions:

- `try_assemble`: entrypoint called by wrench assembly flow.
- `create_marker`: summons aligned multiblock marker.
- `setup_marker`: writes standardized marker data model.
- `validate_all`: periodic validation pass.
- `validate_single`: per-marker check.
- `disassemble`: teardown and effects.

Hook tags:

- `#ra_lib_multiblock:validate`
- `#ra_lib_multiblock:setup_type`
- `#ra_lib_multiblock:check_structure`
- `#ra_lib_multiblock:on_break`

Any new multiblock type should plug into these hook tags instead of bypassing the library.

## 4) Data and Marker Conventions

Use this schema for custom block and multiblock markers:

- `data.properties`: user-configurable fields.
- `data.data`: runtime mutable state.
- `data.status`: readable status for goggles/UX.

Tag conventions:

- `ra.custom_block`
- `ra.custom_block.<id>`
- `ra.multiblock`
- `ra.new`
- `ra.broken`

Keep IDs consistent across:

- give item custom_data
- placement tag
- marker typed tag
- recipe result custom_data
- on-break drop item data

## 5) Tooling Integration

### Wrench

- mode cycling for compatible blocks
- assembly/toggle interactions for multiblock bases and markers

### Creative Data Handler

- property discovery and editing
- type-specific property menus
- internal data/status inspection helpers

### Data Handler

- non-OP-friendly property editing menu
- Shift+RMB target scan for nearby custom markers
- uses `ra_lib:input` backends for numeric and text property edits
- supports pending-edit cancel flow and menu refresh cycle

When adding new configurable properties, update CDH and Data Handler mappings/defaults.

### Goggles

- scans nearby custom markers and multiblocks
- renders billboards from block-defined info profiles
- refreshes in timed batches
- module-level `goggles/draw_displays` wrappers were removed; dispatch now lives directly in `ra:tools/goggles/scan_blocks` and `ra:tools/goggles/scan_multiblocks`

Block-defined billboard contract:

- Each block now decides if it renders overlay info by writing config in `storage ra:temp billboard` before calling `ra:tools/goggles/billboard/handle_billboard`.
- Supported fields: `name`, `show_name` (`1b`), `show_status` (`1b`), optional `name_x`, `name_y`, `name_z`, `name_scale`.
- If neither `show_name` nor `show_status` is set but `name` exists, the handler defaults to name rendering for backward compatibility.
- If neither flag is set and no `name` is present, no billboard is rendered for that block.
- Use this to keep low-information blocks clean while enabling richer overlays on data-heavy blocks.

`ra_wires` adds a goggles tinkering path:

- Sneak + goggles in main hand toggles nearest transport block state.
- On liquid/gas source blocks, tinkering cycles configured medium IDs.

When adding new status fields, update goggles scan/status handlers.

## 6) Contributor Workflow

Use this sequence for safe feature delivery.

1. Define block/multiblock ID and naming.
2. Implement give item, placement handler, tick, and break cleanup.
3. Register placement handler and namespace load/tick hooks.
4. Add recipe and advancement unlock path.
5. Add CDH property support for editable settings.
6. Add goggles status support for visible diagnostics.
7. Update docs and changelog.
8. Run in-world validation pass.

### New Block Checklist (Practical)

1. Item custom_data and `ra.place.*` tags are correct.
2. Placement handler returns `1` only for matching bats.
3. Block tick includes break detection and cleanup.
4. On-break drop reproduces the same item signature.
5. Block appears in module `give_all` and in the correct namespace bundle from `ra:give_all_items`.
6. Recipe and advancement IDs align.

### New Multiblock Checklist (Practical)

1. Validation hook registered in `#ra_lib_multiblock:validate`.
2. Setup hook registered in `#ra_lib_multiblock:setup_type`.
3. Periodic check hook registered in `#ra_lib_multiblock:check_structure`.
4. Cleanup hook registered in `#ra_lib_multiblock:on_break`.
5. Wrench assembly flow can stage required data in `storage ra:multiblock`.
6. Marker stores `type`, `facing`, `properties`, IO/control metadata.

## 7) Validation and Debug

After changes, run this minimum test set:

1. `/reload` with log inspection.
2. Place each changed block once and verify marker tags.
3. Break changed blocks and verify no orphan markers.
4. Verify recipe output tags/custom_data.
6. Test Data Handler and CDH edit operations on changed blocks, including full-inventory text-input warning behavior.
6. Test goggles status rendering for changed blocks.
7. For multiblocks, test both assemble and disassemble paths.

Useful selectors:

- `@e[tag=ra.custom_block,distance=..20]`
- `@e[tag=ra.multiblock,distance=..40]`
- `@a[tag=ra.debug]`

## 8) Common Failure Modes

- ID mismatch between recipe result and placement handler tag.
- Missing placement handler registration.
- Forgetting to remove one-time tags (for example `ra.new`).
- Not updating CDH/goggles when adding new properties.
- Not updating Data Handler input mapping when adding editable properties.
- Multiblock setup data missing required fields in storage before assembly.
- Assuming numeric wireless channels; runtime channels are string values.

---

Related pages:

- [How It Works](how-it-works.md)
- [Block Reference](item-reference.md)
- [Item Reference](item-reference.md)
- [Extension Examples](extension-examples.md)
