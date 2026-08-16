# How It Works

This page explains the runtime architecture of Redstone Additions in detail.

Use this as the canonical technical reference for the pack flow. Module pages focus on behavior and block details, while this page explains system internals.

## Runtime At A Glance

```mermaid
flowchart TD
    A[minecraft:load] --> B[ra:load]
    B --> C[ra_lib:init]
    B --> D[Module loads]
    B --> E[ra_lib_multiblock:init]
    B --> F[ra:tick]
    F --> G[Tool tag cleanup]
    F --> H[ra_lib:input/tick]
    F --> I[ra:tools/data_handler/tick]
    F --> J[ra:placement/detect_bats]
    F --> K[Module ticks]
    F --> L[Goggles scan]
    F --> M[schedule ra:tick 1t]
```

## 1) Load Pipeline

Core load entrypoint is `ra:load`.

Responsibilities:

- Creates shared scoreboards for editing and runtime bookkeeping.
- Initializes shared temporary storage `ra:temp`.
- Initializes shared libraries via `ra_lib:init`.
- Initializes gameplay namespaces (`ra_interactive`, `ra_storage`, `ra_sensors`, `ra_gates`, `ra_wireless`, `ra_wires`, `ra_chunk_loader`, `ra_multiblock`).
- Initializes multiblock library state via `ra_lib_multiblock:init`.
- Starts the recurring tick loop by calling `ra:tick`.

## 2) Tick Orchestration

`ra:tick` is scheduled every tick and acts as the orchestrator:

1. Clears stale click/active tool tags.
2. Runs modular input sessions (`ra_lib:input/tick`).
3. Runs Data Handler action/pending processing (`ra:tools/data_handler/tick`).
4. Runs placement detection (`ra:placement/detect_bats`).
5. Dispatches module ticks in fixed order.
6. Runs goggles scan (`ra:tools/goggles/tick`).
7. Re-schedules itself (`schedule function ra:tick 1t`).

This architecture keeps all state transitions deterministic and centralized.

## 3) Placement Lifecycle (Item -> Block -> Marker)

Custom blocks are represented as bat spawn egg items with custom data and placement tags.

Required item traits:

- `custom_data` marker for block type.
- `entity_data` that spawns a bat with `ra.spawned` and `ra.place.<block_id>` tags.

Placement flow:

```mermaid
sequenceDiagram
    participant P as Player
    participant B as Spawned Bat
    participant R as ra placement router
    participant L as ra_lib:placement/place
    participant M as Marker Entity

    P->>B: Uses custom block item
    B->>R: Tagged bat detected
    R->>R: Placement handlers run (function tag)
    R->>L: Call with block_id/block_tag/dir_type
    L->>L: Resolve facing (if needed)
    L->>L: Place physical block
    L->>M: Summon marker with tags + initialized data
```

### Handler Routing

Placement handlers are resolved through function tags, allowing each module to register block-specific `handle_placement` functions without changing core router logic.

### Marker Creation Contract

`ra_lib:placement/place` performs all generic placement work:

- Resolves orientation (for direction-sensitive blocks).
- Places physical block using helper functions.
- Summons marker entity with base tags:
  - `ra.custom_block`
  - `ra.custom_block.<id>`
  - `ra.new`
- Initializes marker data root so later writes are safe.

Callers are responsible for removing `ra.new` after first-tick initialization.

## 4) Marker State Model

Runtime block state is marker-centric.

Conventions:

- `data.properties.*`: configurable values (CDH editable).
- `data.data.*`: runtime processing state.
- `data.status.*`: status values intended for goggles/display output.

Common tags:

- `ra.custom_block`: any custom marker.
- `ra.custom_block.<id>`: typed marker classification.
- `ra.new`: one-tick setup marker.
- `ra.broken`: break cleanup queue.

This separation keeps user-edited values and transient machine state distinct.

## 5) Shared Library Responsibilities (`ra_lib`)

`ra_lib` is the generic runtime toolbox for all gameplay namespaces.

### placement/

- `place`: canonical custom-block placement helper.
- `set_block`: dispatches simple or facing-aware placement.
- `set_block_facing` and `set_block_simple`: final block placement variants.

### orientation/

- `get_facing`: computes cardinal or full 3D facing from player rotation using `dir_type`.
- `set_facing`: stores resolved facing and canonical rotation values.

### redstone/

- `detect`: gathers power from multiple sources and updates both world-space and look-space power scoreboards.
- `detect/*`: source-specific detectors (dust, repeater, comparator, lever, button, block, torch).
- `clear`: resets tags and prior signal state before a new detection pass.

Redstone output contract:

- `ra.power`: aggregate max power (`0..16`)
- world-space: `ra.power.north/south/east/west/up/down`
- look-space: `ra.power.front/back/left/right/local_up/local_down`
- `16` is reserved for superpower from direct powered repeater/comparator output

Runtime usage note:

- Gates and wireless emitter run `ra_lib:redstone/detect` directly inside their per-block process functions.
- The old gate-wide signal sweep function is no longer part of active tick flow.

### inventory/

- `move_slot`: whole-slot transfer via `/item replace block ... from block ...`.
  The preferred primitive — the stack crosses verbatim, with no loot table and no
  NBT arithmetic.
- `insert_or_drop`: insert what fits, drop the rest as an item entity. Use this
  rather than `insert` for anything larger than a single item.
- `insert`: raw `loot insert`. **Destroys** whatever the destination cannot hold,
  so it is only safe with a count of 1 or behind `insert_or_drop`.
- `remove`: counted removal, any container size, handling amounts split across
  several stacks. All-or-nothing.
- `find_free_slot` / `has_free_slot` / `container_size`: slot helpers.
- `clear`: reset helper for temporary inventory storage states.

### transport/

The shared network engine used by fluids and item pipes.

- Adjacent nodes of the same class are grouped into networks by flood fill.
- Rebuilt only when a node is placed or broken, debounced to at most one rebuild
  every 5 ticks.
- Per-network amount and capacity live in scoreboards on a `net<id>` fake player;
  the medium is a readable string in `storage ra:transport`.
- `net/join`, `net/rejoin`, `net/leave`, `net/offer`, `net/take`, `net/read`.

### skin/

Draw one block's appearance over another block's mechanics, for cases where a
vanilla block carries behaviour that cannot be switched off — a dispenser firing
its own inventory on a redstone edge, for instance. `apply` / `apply_static` /
`clear`. Full rationale and caveats in the Developer Guide.

### input/

- `init`: creates input objectives and initializes session storage roots.
- `tick`: advances active sessions each tick via selected backend router.
- `poll` / `consume`: runtime API used by Data Handler pending-edit flow.

Backends:

- `trigger`: numeric input submit/validation flow.
- `writable_book`: text capture flow from temporary Input Form book.

Safety behavior:

- `give_book_safe` prevents giving books when inventory is full and emits a red warning.
- `kill_dropped_req` clears dropped request books using request-aware matching.
- cleanup/restore paths return the original held item state after text submission or cancel.

## 6) Tool Runtime

## Wrench

Used for mode cycling and multiblock interactions:

- Detects target custom marker.
- Dispatches per-block cycle/toggle logic.
- Runs multiblock assembly attempts for base blocks.

## Creative Data Handler (CDH)

Property editor for marker entities:

- Detects target marker and writes targeting state to storage.
- Enumerates/edit properties through dedicated property functions.
- Uses tellraw-driven interaction menus for edit operations.

## Data Handler

- Detects nearby target marker on Shift+RMB and opens trigger-driven edit menu.
- Routes numeric and text edits through `ra_lib:input` session APIs.
- Applies consumed values back into marker `data.properties` and refreshes menu.
- Supports explicit pending-edit cancel without requiring op-level raw `/data` edits.

## Goggles

Status overlay system:

- Detects players wearing/holding goggles.
- Throttles scans on timer (every 40 ticks).
- Clears old billboards each cycle.
- Collects every marker in range of any goggles wearer **once**, then draws each
  one. Drawing per player duplicated billboards when two wearers stood near the
  same block, and cost a whole-world sweep per block type per player.
- `ra:tools/goggles/draw_block` is pure routing.

Block-level rendering control:

- Each block owns its readout in `blocks/<name>/goggles.mcfunction`, next to its
  tick and placement logic.
- That function publishes the block's display name to `storage ra:temp block_name`
  first, then returns early if `storage ra:temp name_only` is set. This makes it
  the single source of the name: `ra:tools/block_name` reuses the same dispatch to
  answer the Data Handlers.
- Status lines are emitted with `prop_line` (a `data.properties` value),
  `data_line` (a `data.status` value) or `text_line` (a literal). A missing value
  renders "N/A" in red rather than disappearing.

## 7) Multiblock Lifecycle (`ra_lib_multiblock`)

`ra_lib_multiblock` abstracts assembly and validity checks so `ra_multiblock` can focus on structure-specific logic.

```mermaid
flowchart LR
    A[Wrench on base] --> B[ra_lib_multiblock:try_assemble]
    B --> C{#ra_lib_multiblock:validate}
    C -->|pass| D[create_marker]
    D --> E[setup_marker]
    E --> F[grace tag]
    F --> G[Periodic validate_all]
    G --> H{check_structure}
    H -->|fail| I[disassemble]
```

Core functions:

- `try_assemble`: runs type validator hooks and creates marker on success.
- `create_marker`: summons centered marker and triggers setup flow.
- `setup_marker`: writes standardized typed data (`type`, `facing`, `properties`, `inputs`, `outputs`, `controls`).
- `validate_all`: periodic pass over all multiblock markers.
- `validate_single`: per-marker structure validity check.
- `disassemble`: type-specific cleanup hook and marker removal.

## 8) Namespace Boundaries

- `ra`: orchestrator, placement detection, tools, uninstall.
- `ra_lib`: shared generic systems.
- `ra_lib_multiblock`: shared multiblock runtime.
- `ra_interactive`, `ra_storage`, `ra_gates`, `ra_sensors`, `ra_wireless`, `ra_wires`, `ra_chunk_loader`: module behavior.
- `ra_multiblock`: concrete multiblock structures and base blocks.
- `ra_advancements`: progression triggers/unlocks.

## 9) Contributor Mental Model

When adding features, decide first:

1. Is this generic and reusable? Add in `ra_lib` or `ra_lib_multiblock`.
2. Is this gameplay-specific? Add in the target module namespace.
3. Does it need configuration? Store in `data.properties`.
4. Does it need user-visible diagnostics? Store in `data.status` and expose via goggles.

Keep placement tags, block IDs, recipes, and advancements consistent. Most runtime bugs in datapacks come from mismatched IDs across those files.

---

Next: [Developer Guide](developer-guide.md) for implementation workflow and extension checklists.

