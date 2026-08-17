# Changelog

## [v5.1.5] - 2026-08-17 - Enchant Crafting, Jetpacks, Infinite Generators, Ender Links

Four new modules, a recipe-image renderer, a Data Handler that can edit every
property it displays, and two long-standing electric transport bugs fixed.

**Supported versions:** 1.21.9 – 26.2 (data pack formats 88 – 107).

### Added

**Enchant crafting (`ra_enchanting`)**
- Items dropped on a vanilla enchanting table are sacrificed one per second for a
  chance at an upgrade: enchant particles and a level-up chime on a hit, lava
  particles and a hiss on a miss, and nothing back either way on a miss.
- Recipes come from the function tag `#ra_enchanting:recipes`, so an addon
  registers its own without touching the module. Contract in
  `ra_enchanting/README.md`.
- The item scan is the pack's only global `@e[type=item]` selector and runs once
  every five ticks. Products carry `ra.ench.done` so an upgrade that lands back
  on the table is not sacrificed again.

**Jetpacks (`ra_jetpacks`)**
- **Iron Jetpack Kit** — crafted; right-click while wearing any chestplate to fit
  it. The chestplate keeps its material, durability and enchantments.
- **Infinite Iron Jetpack Kit** — won by sacrificing an Iron Jetpack Kit on an
  enchanting table, 10% per kit. Burns no fuel and can be fitted over an existing
  iron jetpack.
- Two flight modes, switched with `/trigger ra.jp.mode`: **classic** (sneak to
  rise, three blocks a second) and **hover** (the `minecraft:gravity` attribute
  goes to zero, then sneak plus look up to climb at the same three blocks a
  second, or look down to sink). Sinking hands gravity back with slow falling on
  top rather than using levitation: the amplifier-255 wrap-around trick no longer
  reads as `-1`, so it fired the player upward at twelve blocks a second instead.
- Holding station in hover is a servo, not an absence of gravity. Vertical speed
  is read from the change in `Pos[1]` each tick and the `minecraft:gravity`
  modifier is aimed against it — `-0.16` for a full upward thruster, `-0.08` for
  weightless, `0.0` for vanilla fall, with a 0.006 b/t dead zone and a gentle
  middle tier. A levitation coast dies in two ticks and the tier is only rewritten
  when it changes.
  - This replaces an in-place `tp @s ~ ~ ~` that was supposed to clear velocity.
    It never did: [MC-275455](https://mojira.dev/MC-275455) was fixed in the
    1.21.2 snapshots, so relative teleports now *keep* motion, and no command sets
    a player's velocity any more. Only a force can.
- A worn jetpack adds `0.7` to `minecraft:sneaking_speed`, so holding sneak to
  fly no longer drops the player to 30% walking speed.
- Standing on a block idles the hover thrusters and hands back vanilla physics —
  walking around with gravity at zero felt wrong and the servo had nothing to hold
  up. Sneak plus look up is exempt, which is how you lift off.
- *Landing* — touching down after actually having been airborne, tracked in
  `ra.jp.air` — runs the same function `/trigger ra.jp.power` runs, so gravity comes
  back and flight only resumes when the player asks for it. Merely standing no
  longer does this: it switched the jetpack off the tick after hover was selected,
  leaving nothing armed to take off with.
- `/trigger ra.jp.power` disables the jetpack outright for one player: no hover, no
  sneak thrust, no fuel, no particles, no sound, in either mode. The chestplate
  keeps its components, so flight comes back with a second trigger.
- Hover runs a constant downward campfire-smoke wash and a heavier one while
  climbing or sinking. Its pitch dead zone is ±30°, so a glance no longer flips
  the state. No end rods — the white streaks read as glitched geometry rather than
  exhaust.
- The engine is `minecraft:item.elytra.flying` at volume 0.35, pitch 0.6, replayed
  once a second. One sound, one volume, whatever the jetpack is doing — hover hold
  included, since the thrusters are carrying the player there too. The old
  `entity.breeze_wind_charge.burst` was restarted every tick, and a one-shot fired
  twenty times a second is not a note — it was inaudible. `/trigger ra.jp.sound`
  mutes it per player.
- The sound logic is two functions and no tiers: `flight/sound` keeps the loop alive,
  `flight/sound_off` `stopsound`s it and clears `ra.jp.sound_on`. `sound_off` runs on
  every path that stops carrying the player — sneak released in classic, feet on a
  block in hover, chestplate off, out of fuel, switched off, muted — which fixes
  classic humming on after sneak was released, since a long sample kept playing to
  its end. `sound` stops before each replay so copies never stack.
- Exhaust particles only run while the player is off the ground. Classic mode thrusts
  on sneak, which is also how you walk quietly.
- Jetpack particles and sounds are played `as @s at @s`. Dispatching with `as`
  alone changed who ran the command but not where, so every effect fired at the
  command origin and none of them were visible or audible. The plume now sits
  just below the player's feet, out of the way of the view.
- Fuel: the iron tier burns one coal or charcoal per two minutes of *powered*
  flight. Running dry cuts the jetpack out until fuel is back in the inventory.

**Ender links (`ra_ender`)**
- **Ender Item Vault** — a real barrel with a `channel` and a `mode`. One whole
  stack every 4 ticks, into the first free slot of the nearest eligible vault on the
  channel. The move is `ra_lib:inventory/move_slot` (`/item replace … from block …`,
  then clear the source) into a slot checked empty first, so a stack is never in two
  places and never overwrites anything.
- Item vaults default to **`shared`**: stand within 4 blocks of any vault on the
  channel and the whole contents move into that barrel, so the vault you walk up to
  is the one holding everything. One `data modify … Items set from block …`, with the
  source cleared only if the copy reported success.
  - Not a mirror, and cannot be: the same stacks in two barrels means two extraction
    points, and two players or hoppers pulling in the same tick each get a copy
    before any function can run. Container clicks are not interceptable. There is
    always exactly one real copy in a channel.
  - A holder with someone standing at it keeps what it has, so two people at two
    ends do not pull it out from under each other. Contents inserted into one end
    while the far end holds the rest are merged a stack per cycle, since copying the
    whole list would collide slot numbers.
- `mode: link` is the automation two-way; `send` / `receive` remain for
  one-way pipes. Two-way needs a different rule per medium, because the obvious one
  shuffles: A hands its only stack to B, B now holds more than A and hands it back.
  - Items follow the outside world. Each vault records how many stacks it left
    behind and compares: more than before means something was inserted, so push;
    fewer means something was taken, so pull; unchanged means do nothing, so a quiet
    pair costs nothing per tick. Deliveries update the receiver's mark on arrival,
    so a delivered stack is not read as an insert and returned. Pull reuses the push
    path — the asking vault makes itself the only eligible receiver for one command.
  - Fluid and EU find their level instead: only the fuller side pushes, and only
    half the gap, with a dead zone (20 units, 4 EU) so the tail does not oscillate.
  - A two-way vault wears both the send and the receive tag, so partner searches
    exclude the searcher via `ra.ender.self`, which is why each cycle exits through
    `ra_ender:link/done`.
- **Ender Fluid Vault** — an ordinary fluid node with a 1000 buffer, so pipes and
  tanks treat it as one of their own. Take from the local network, offer to the
  partner's, return whatever the far side would not accept: full receivers and
  medium mismatches cost nothing.
- **Ender Power Vault** — tagged `ra.wires.electric_node` with a 400 EU buffer, so
  wires connect to it like a switch. The receiver reports how much room it had and
  only that much leaves the sender.
- **Teleport Anchor** — crying obsidian with a string `anchor_id` (`"A"`, `"base"`)
  and a table of fifteen target ids, one per redstone strength. Power it and stand within 2 blocks: the
  signal strength picks the row, the nearest player lands on the matching anchor.
  Strength 16 (a redstone block) is treated as 15. Self-targets are ignored, each
  anchor waits 20 ticks between firings, and an arriving player gets 30 ticks of
  grace so a powered destination cannot bounce them back — which is what makes a
  two-way door work.
  - `ra_ender:tools/anchor/{help,set_id,set_table,set_target,show}` edit and print
    the nearest anchor's table. `set_table` takes the list as typed text —
    `{table:["A","B","C"]}`, signal 1 first, short lists padded — and the Data
    Handler now lists `anchor_id` and `targets` with buttons that suggest those
    commands. The Handler has a hand-written row per property name and no way to
    type a list, so the table was visible but unchangeable; ids became strings at
    the same time. the Goggles show its id, the live signal and where that signal
    points.
- Vaults are directional by design. Two pushing vaults would shuffle one stack
  forever, and a pair that equalised could never move a single stack.
- Recipes follow the existing shapes — ender pearls plus the material each vault
  moves — and nothing in the module can duplicate: every transfer removes before or
  as it adds, and a broken block drops exactly the item that placed it.
- Channel matching happens in a macro, since no selector can compare one entity's
  property against another's.

**Infinite generators (`ra_infinite`)**
- `#ra_infinite:flower_ground` lists block ids instead of borrowing
  `#minecraft:dirt`. **26.2 narrowed `#minecraft:dirt`** to dirt, coarse dirt and
  rooted dirt and moved grass blocks to the new `#minecraft:substrate_overworld`, so
  a Poppy Generator standing next to a grass block found no ground, planted nothing
  and reported `Ground: none` — on 26.2 only. Borrowing the new tag instead would
  break 1.21.10, where it does not exist.
- `/function ra_infinite:debug/poppy` reports every Poppy Generator's marker
  position, rotation, facing, cooldown, the block in front and the ground verdict.
- **Generator Casing** — eight copper grates around a netherite scrap.
- **Mineral / Nether / Poppy Core** — won on an enchanting table by sacrificing
  stone, netherrack or poppies, 1% a piece.
- **Mineral / Nether / Poppy Generator** — casing plus the matching core. Each
  grows a weighted-random block in front of itself whenever that spot is empty,
  so a Block Breaker pointed at one is a self-feeding farm. Periods are 100, 120
  and 80 ticks; the mineral table works out to a diamond every seven minutes or
  so, the nether table to a piece of ancient debris every ten. Netherite blocks
  are off the table entirely.
- Generators are plain droppers, as item and as block, so the face they grow from
  is obvious. Their redstone ejection is harmless: a generator holds nothing in
  its own inventory and never reads redstone.
- The Poppy Generator plants a single flower or a 3×3 patch, cycled with shift+RMB
  of the Wrench, into anything in `#ra_infinite:flower_ground` — the vanilla dirt
  family plus farmland. It does not terraform: providing the ground is the player's
  job.
  - `single` mode searches the 3×3 around the block in front and plants in the first
    spot that takes, trying three heights per cell: level with the soil, on top of
    soil it stares at, and one down for a generator on a pedestal. Insisting on the
    one block dead ahead is what made it look broken — a generator facing slightly
    off, or with its grass beside rather than in front of it, planted nothing.
  - One block, one marker: a placement that ran twice left two markers stacked on a
    generator, ticking it twice and drawing the Goggles readout twice on top of
    itself. Each generator's tick now clears the duplicate.
  - Why it looked broken: `dir_type:2` faces a block **up** when it is placed while
    looking down, which is how most blocks get placed, and a generator facing up
    targets the block above itself — where the thing under the target is the
    generator. No ground, no flower, no message, forever. Place it looking level so
    it faces sideways at dirt or grass.
  - The Goggles now show a **Ground** line, `ok` or `none`, so an unplanted generator
    says whether any of those three spots is plantable.
- The Nether Generator's period is 100 ticks, matching the Mineral Generator — five
  seconds each. It was 120.
- Vanilla recipes match ingredients by item id alone — components exist on a
  recipe's result, never on its ingredients — so the casing and the three cores
  each sit on a different `GameMasterBlockItem` (repeating command block, jigsaw,
  structure block, chain command block). Survival cannot obtain or place any of
  them, which makes the three generator recipes distinguishable and a bare
  netherite scrap useless as a casing, with no guard advancement or intermediate
  item involved. The Item Crate keeps the plain command block, so no recipe here
  can swallow a loaded crate.

### Fixed

- **The Data Handler asked for a number when editing any string.** Its type probe
  treated "`data get` succeeded" as "this is a number", but `data get` succeeds on a
  string too — it returns the string's length. Every string property therefore got the
  number editor, and using it wrote an int. A vault `channel` written that way matches
  no string comparison, which is why a sending vault stopped finding its partner. The
  probe now asks `data modify … set string`, which only accepts a string.
  - A vault edited while the probe was wrong keeps its int `channel` — set it again
    with the fixed editor, or break and replace the block. No migration runs for it:
    a per-tick type check on every vault forever is a poor trade for a bug that
    existed for hours.
  - `/function ra_ender:debug/vaults` prints each vault's position, properties, what it
    can send to or receive from, and whether its channel is a string at all — the one
    thing that is invisible in chat, since `5` and `"5"` print identically.

- Ender links: three bugs caught in review before they shipped.
  - A teleport anchor never fired. `scores={ra.ender.tp_cd=..0}` does not match an
    entity that has no score at all, so a freshly placed anchor was never eligible,
    and neither was a player who had never teleported (`ra.ender.grace`). Both are
    seeded with `add … 0`.
  - A pull never delivered. `link/send_items` excluded `ra.ender.self`, which marks
    the vault whose cycle is running — on a pull that is the *receiver*. The
    exclusion is now `ra.ender.sending`, held only by whoever is pushing.
  - `tools/anchor/set_table` cleared the table before parsing the typed list, so a
    typo wiped it, and a stale list from an earlier call could be copied. It parses
    first and only writes once the list is readable.
  - The fluid vault tagged itself `ra.wires.fluid_block`; `ra_wires` marks fluid
    blocks `ra.wires.fluid_node`.
- **Electric charge only ever reached the first two blocks of a run.** Two separate
  bugs, both in the node-to-node push:
  - A node handed EU to whichever neighbour had room, including the one that had
    just supplied it, and `transfer_adjacent` tries `+X` first — so charge bounced
    between the first two blocks and never travelled further. A solar panel filled
    the two wires beside it and left the rest of the line at zero. Transfer now
    requires the destination to hold less than the source and moves half the
    difference: half converges the way water finds its level, where the whole
    difference just makes the pair swap places and oscillate.
  - **The transfer latch was never released.**
  `ra.wires.did_move` marks a node that has already handed charge to a neighbour on
  the current tick, but nothing ever cleared it, so the first push a node made
  tagged it permanently and `transfer_adjacent` skipped every direction from then
  on. Two solar panels placed side by side would trade once, tag each other, and
    never feed the wire run again. `ra_wires:electric/tick` now clears the tag at the
    start of each pass.
- `{enabled:1b}` matches only a byte, and the Data Handler's `[+Add]` pastes whatever
  value text it is given, so an `enabled` typed as `1` read the same in chat and
  failed every gate — as did a property that was missing altogether. A wire run or
  pipe line then did nothing, with nothing to say why. The ten runtime gates in the
  electric and fluid paths now ask the tolerant way round, off only when explicitly
  `0b`, matching the rest of the pack.
- `ra_lib_multiblock:build/*` was never committed: the directory is called `build`
  and `.gitignore` excluded `build/` anywhere in the tree, so a fresh clone built a
  pack whose multiblock assembly called eight functions that did not exist. The pack
  output is tracked again too, so an older commit checks out as a working pack.
- Block skins rendered black. A `block_display` reads the light level at its own
  position, and that position is inside the opaque block the skin covers, where
  the light level is zero. `ra_lib:skin/spawn` and `spawn_static` now set
  `brightness:{sky:15,block:0}`, which lights a skin like an ordinary surface
  block — still dimming at night rather than glowing. This also fixes the
  Unboxer's dispenser skin.

### Removed

- The Item Crate crafting recipe (`ra_storage:storage_box`) and its unlock
  advancement. Crates come from a Boxer; a recipe whose result is a command block
  was never meant to be reachable, and the docs now say so.

### Documentation

- **Licence rewritten (v1.1).** The old §3 granted permission to redistribute the
  pack "in part and modified" in its Addons paragraph while forbidding exactly that
  one paragraph earlier, so the two rules contradicted each other and the permissive
  reading was available to anyone shipping RA files. Addons are now their own clause:
  build and publish them anywhere on any terms, with no RA namespace inside and no
  permission needed; redistributing any portion of RA still needs written permission.
  - Snippets from the documentation and `ra_example` are explicitly licensed for addon
    use, since the docs hand out code that the old terms forbade copying.
  - "RA Namespace" is defined as *shipping in the official download* rather than by an
    `ra_` prefix, because the example addon in the docs is called `ra_myaddon`. The
    current eighteen are listed.
  - Adds a contributions clause, a Mojang assets clause — the recipe pictures are
    rendered from vanilla textures and are not the author's to reserve rights over —
    and a version and date, so a copy says which terms it came with.
  - Revocation now applies to distribution permission rather than to the use and addon
    grants, so an addon someone has written does not stop being allowed.

- **[Recipe Atlas](https://ancarsenat.github.io/Redstone-Additions/recipe-atlas/)** —
  one page with all 58 recipes, alphabetically and again grouped by module, with the
  station each is made at and the namespace id. Generated by
  `tools/recipe_render/render.py --atlas` from the recipe files themselves, so it
  cannot drift from the pack. The home page's "Visual Module Atlas" was one sample
  recipe per module with three modules carrying prose instead of a picture; it now
  points at the atlas and every row has a picture.
  - The atlas also lists what has *no* recipe: the three generator Cores and the
    Infinite Iron Jetpack Kit, which are gambled for on an enchanting table. Those
    four are read out of the `enchant_recipes` functions — sacrifice, result and
    chance — so they cannot drift either, and they appear in the A-to-Z list, which is
    what a player searching for "Mineral Core" actually uses.
- Enchant Crafting opens with a four-line summary of the whole mechanic, since the
  page previously took a couple of paragraphs to say what it does.

- The home page carried a "Breaking change" warning saying redstone on the Boxer and
  Unboxer had become a lock — unpowered runs, powered pauses — and told players to
  remove the signal from existing builds. That inversion was a mid-v5.1.4 workaround
  for a vanilla dispenser firing its own contents, reverted in the same release once
  the Unboxer became a barrel, and both blocks have always shipped running while
  powered. `docs/storage.md` said so correctly; the home page did not, and following
  it would have broken working builds.

### Diagnostics

- `/function ra_infinite:debug/poppy` — every Poppy Generator's marker position,
  rotation, facing, cooldown, the block in front and the ground verdict.
- `/function ra_wires:debug/electric` — every electric node's buffer, rate and
  properties, whether its `enabled` flag is a byte at all, whether the transfer latch
  is stuck, and how many neighbours the adjacency probe reaches, using the same
  offsets and radius the transfer itself uses.

### Changed

- The survival Data Handler hides tuning fields: `cooldown`, `transfer_rate`,
  `generation_rate`, `eu_use`, `tier` and `tier_level` are builder and addon-author
  knobs, not something to retune while holding the survival tool. Creative mode is the
  test, since a data pack cannot read permission level, and the number of withheld
  fields is reported so a block does not look settings-free. The Creative Data Handler
  still shows everything.
- The Creative Data Handler builds its property rows from one registry instead of a
  hand-written function per property name. `ra:tools/data_handler/init_registry` holds
  the list of names; for each name a block actually has, `props/render` probes the
  value's type — bool, number, list, string — and draws the matching editor. A row's
  button carries `100 + its registry index`, so `run_action` and `apply_pending` each
  need one branch for every property rather than one per name, and adding a property
  to a block now means adding its name to the registry and nothing else.
  - This is why blocks could show a property the Handler had no way to change: a
    wire's `transfer_rate`, a tank's `tier`, an anchor's id. All of them are editable
    now, and a list is edited by writing it out — `["A","B","C"]`.
  - Type detection order matters: `data get` reports a number for a byte and a length
    for a list, so the bool and list tests run after the numeric one.
  - Twenty-one `props/show_*` rows and the per-name branches in `run_action` and
    `apply_pending` are gone. `ra:dh` state stays global — the Handler is a
    single-player tool, as before.
- Goggles lines can stack instead of being hand-placed, tighter and never buried.
  `stacked_data_line` completes the set alongside `stacked_prop_line` and
  `stacked_text_line`.
  `ra:tools/goggles/billboard/stack_reset {top,step}` sets where a block's ladder
  starts and how far apart the rungs are, in hundredths of a block, and
  `stacked_prop_line` / `stacked_text_line` take no `y` at all. The old per-line `y`
  functions are untouched, so nothing else had to change. The Poppy Generator, whose
  fifth line used to render at `y:0.0` — inside the block, where it cannot be read —
  is the first user.
- Recipe pictures are drawn instead of screenshotted. `tools/recipe_render/` takes a
  recipe file and writes `docs/images/recipes/<namespace>/<name>.png` — 704x324, the
  vanilla window cropped to slots, arrow and result, the same crop and slot
  coordinates misode.github.io uses, with the window's bottom border pasted back on
  so the panel is not sliced open. Assets come from the client jar of whichever
  version is asked for (`latest` by default), cached per version and git-ignored.
  - Blocks are rendered from their **real model elements**, the way the game and
    deepslate do it: `display.gui` transform about the block centre (rotation
    `[30, 225, 0]`, scale `0.625`), orthographic projection, back-face culling,
    painter's algorithm, and vanilla's per-face shading constants. Stairs look like
    stairs, a daylight detector is slab height, and a dropper's front face lands on
    the side the game puts it — the first draft faked every block as a cube and had
    the front on the wrong side.
  - Display rotation is applied Z, then Y, then X, matching `rotationXYZ`'s
    `Rx·Ry·Rz` order. Spinning before tilting left every block icon leaning over.
  - Each face is backed by its own average colour before the texture is pasted. The
    affine transform samples transparent right on a face's edge, which showed as a
    hairline of background between two faces of the same block.
  - Items resolve the way the game resolves them: item definition, model parent
    chain, textures. A result's `minecraft:item_model` component is honoured — which
    is why the generators appear as droppers. Ingredients cannot carry components,
    so disguised RA ingredients are declared in `overrides.json`.
  - Every recipe screen is covered, not just the crafting table: furnace, blast
    furnace, smoker, campfire (on the furnace GUI), stonecutter and smithing table,
    with slot coordinates taken from the vanilla menus. Types with no screen of
    their own are skipped rather than drawn on the wrong background.
  - Documented in `docs/recipe-renderer.md`. The five new recipes (Generator Casing,
    the three generators, the Iron Jetpack Kit) are the first to use it.
- `ra:load` and `ra:tick` dispatch the three new modules; `#ra:placement_handlers`,
  the goggles `draw_block` table, the Wrench's `cycle_block` table and the
  categorized bundles all carry the new entries.

## [v5.1.4] - 2026-08-16 - Transport Rewrite, Item Safety, Library Audit

A large maintenance release. The fluid and gas system was rebuilt on a shared
network engine, several item-destroying and item-duplicating bugs were fixed, and
a full audit pass removed dead code and a class of per-tick performance problems.

**Supported versions:** 1.21.9 – 26.2 (data pack formats 88 – 107). Most of the
content does not need 1.21.9 — see the compatibility table in the README for what breaks on older versions and
when.

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
- `ra_lib:skin/*` — draw one block's appearance over another block's mechanics,
  for vanilla blocks carrying behaviour that cannot be switched off. Documented
  under "Block Skins" in the Developer Guide.
- Goggles line helpers `prop_line`, `data_line` and `text_line`.
- Drain **"place" mode**, cycled with the goggles tinker: spends network contents
  putting source blocks back into the world.
- Infinite-source rule: nine or more matching sources within two blocks counts as
  inexhaustible; anything smaller genuinely empties.

### Changed

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
  Forge and every multiblock IO insert. These now place the stack into an
  explicitly chosen free slot, dropping it whole when there is none.
- **Delivered stacks were inserted AND dropped.** The first version of
  `insert_or_drop` worked out the leftover by subtracting `loot insert`'s return
  value from the requested count — but that command reports the number of item
  entries it handled, not the number of items, so a full stack looked almost
  entirely un-inserted and was duplicated onto the floor. Nothing on that path
  uses `loot insert` any more.
- **The Unboxer duplicated items** — it inserted into the output before removing
  from the box, so any path reaching one and not the other produced both.
- **The Unboxer only emptied part of a crate.** It moved one stored stack per
  cooldown, so a crate sat half-full in the input between cycles. A crate is now
  emptied completely in a single activation, and the spent crate is consumed.
- **The Unboxer threw the crate on the floor.** The block was a vanilla dispenser
  and the Unboxer holds the crates it is unboxing in its own inventory, so any
  rising redstone edge reaching it fired the contents out. It is now a barrel —
  same inventory and GUI, no dispense behaviour — wearing a dispenser skin drawn
  as a block_display, so it still reads as an Unboxer. Note it now opens a
  barrel's 27 slots rather than a dispenser's 3x3.
- **Item Crates cannot be placed.** The crate's base item is now a command block
  rendered as a head, rather than a real player head. A crate carries its entire
  contents in `custom_data`; placing it as a skull dropped that silently and
  destroyed everything inside. Command blocks require creative and permission
  level 2 to place, so a survival player cannot lose a crate that way.
- **The Boxer ran while unpowered.** An earlier attempt at the dispenser problem
  inverted the redstone control on both storage blocks; the Unboxer was put back
  but the Boxer was missed. Both run while powered again.
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
- **The declared pack format range stopped short of the current game.**
  `max_format` was 102, which is `26.2-snapshot-3` — an early snapshot, so the
  range never reached the 26.2 release (format 107) it was aiming at. It now
  declares `min_format` 88 through `max_format` 107, covering 1.21.9 up to 26.2.
- **`ra:predicate/is_sneaking` broke at format 102 and now ships in both
  spellings.** That snapshot reworked entity predicates "from a structure with
  multiple optional fields to one similar to data component maps", which is
  exactly the shape this predicate used — and it gates the wrench shift-action,
  the goggles tinker and the Remote's channel prompt, so all three would have
  gone silent on 26.2. The pre-102 form stays in `data/`, and a new
  `overlay_102/` supplies the component-map form (`"minecraft:flags"` instead of
  `"flags"`), which the game applies automatically from `26.2-snapshot-3` onward.
  Overlay entries carry their own `min_format`/`max_format`, available since
  `25w31a` — the same version that introduced those fields on the pack itself, so
  every version this pack loads on understands the overlay.
  The rest of the pack was audited against the 88 → 107 breaking-change list and
  touches none of it: no `filtered` loot function, no `contents` dynamic loot
  entry, no renamed game rules, no special crafting recipe types, no removed
  entity NBT.
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
- The **Pusher** item, which shipped in the Interactive bundle and was recognised
  by both data handlers but had no placement handler, block folder or tick logic.
  Placing it did nothing.
- Unused objectives `ra.channel`, `ra.edit_step`, `ra.inv.slot`, `ra.mb_enabled`.
- Redundant stone-button pass in redstone detection, eight no-op tag sweeps in
  `ra_wires:common/tick_cleanup`, and the inert `#ra_gates:*` / `#ra_multiblock:*`
  function tags.

### Docs

- README and the wiki home page state the real supported range and carry a
  compatibility table giving the version floor for each feature — the hard floor
  is the `pack.mcmeta` schema, not the content, so most of the pack runs on
  1.21.5+ with only Item Pipe filters missing. Both previously advertised
  "Minecraft 1.21+".
- Block and recipe counts corrected to 45 and 50, counted from source.
- New "Block Skins" section in the Developer Guide covering `ra_lib:skin`,
  including what the technique does not hide.

### Known issues

- Blast Forge and Upgrade Platform still use hand-written per-facing coordinate
  tables rather than the new multiblock registry.
- This release has not been play-tested in game.

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
