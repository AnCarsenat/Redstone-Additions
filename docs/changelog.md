# Changelog

This page mirrors key datapack milestones from the main project changelog.

## v5.1.7 (2026-08-18) — Grids, Bridges, Millilitres

Electric stopped pretending to be a network and became one. Fluids got a unit,
a hand-loading point and experience. Two library bugs that had been quietly
wrong for a long time are fixed, and the redstone reader was rebuilt.

### Added

- **Clipboard** and **Multimeter** tools. The Clipboard makes the first block you
  shift-click the *origin* and matches every block you shift-click after it, same
  kind only; shift-click at nothing to clear. Everything is shift+RMB because most
  configurable blocks are backed by a container and a plain click opens their GUI.
  The Multimeter reads a block's network in chat: which grid, what it stores, what
  this block contributes, what it draws or makes.
- **Industrial Light** — redstone *and* EU, projects a 10-block beam of
  `minecraft:light`. Stops at the first solid block, and clears only light blocks,
  so nothing a player built can be removed by it.

- **Clipboard** — click a block to make it the *origin*, then click others of the
  same kind to match them to it. Shift-click a block to re-origin, shift-click at
  nothing to clear. Same-kind only, because half the pack has a property called
  `enabled` and a Gas Valve's rate landing on a Randomizer's chance would quietly
  mean something else. Only `data.properties` travels; a block's private working
  state stays put.
- **Multimeter** — click a block to read its network in chat: which grid, what it
  stores, what this block contributes, and what it draws or makes.
- **Industrial Light** — redstone *and* EU, projects a 10-block beam of
  `minecraft:light`. Stops at the first solid block; when it goes out it clears
  only light blocks, so nothing a player built can be removed by it.

- **Electric runs on the transport network engine.** Adjacent nodes flood-fill
  into a grid, the charge belongs to the grid, and a generator's output is
  spendable by a consumer anywhere on it on the same tick. See the note under
  *Fixed* for what it replaced.
- **Battery** — 10000 EU of grid storage. Wires, switches and breakers now store
  **nothing**, so a grid holds what its batteries hold: build them, or spend your
  generation on the tick it is made.
- **EU Breaker**, and the **Valve** repurposed to match: a *bridge* belongs to
  neither of the networks it sits between, and while powered by redstone it moves
  contents from the network behind it into the network in front. It never merges
  them — a run with a bridge in it is two networks, always.
- **Ender Power Vault is a wireless bridge.** It joins its local grid, contributes
  no capacity, and moves EU out of the grid at one end into the grid at the other.
  Anything the far side cannot take is put straight back.
- **Millilitres.** A bucket is 5000 mL, a pipe holds 1000, a tank 100000. Network
  totals moved out of scoreboards and into `storage ra:transport nets.n<id>`,
  which is where the per-medium map will go when networks can hold more than one
  thing at a time.
- **Experience and potions are media.** One experience point is 100 mL, in both
  directions.
- **A vertical drain is the hand-loading point.** Sneak beside it holding a full
  bucket and its contents go into the network, empty bucket back in hand; with
  nothing in hand it takes your experience instead, ten points a cycle. Set to
  *place*, an experience network gives it back as orbs at the same rate.
- **Wrench-configurable throughput**: Liquid Drain at 2.5 / 5 / 10 L per second,
  EU Consumer at 20 / 40 / 80 EU per tick.
- `ra_lib:redstone/any`, `detect_switch`, `local/{front,back,left,right,up,down}`
  and `side` — read only what you need, instead of every block paying for a full
  twelve-direction sweep it then threw away.
- Goggles redraw **once a second** instead of once every two.

### Fixed

- The **Liquid Pump could not pump next to water**. A pump on two pipes had a
  4000 mL network and a water source is 5000 mL, and a source block is
  all-or-nothing — offering half and deleting the block anyway would quietly
  destroy a lake. Pumps and drains hold 5000 mL now, one bucket, so they work
  standing alone. `network_too_small` is told apart from `network_full`: one means
  add a tank, the other means wait.
- **Bridges did nothing unless they happened to face along the pipe run.** A valve
  dropped into an east-west line while the player faced south pointed at two empty
  blocks, with nothing on the block to say so. They have no facing at all now —
  they look at all six neighbours, find the networks themselves, and move from the
  fullest to the emptiest.
- A Valve's readout reported the fluid-node fields it does not have and printed
  `N/A` for every one of them.

- Bridges have **no facing at all** now. They look at all six neighbours and find
  the networks themselves, moving from the fullest to the emptiest. Bridging the
  block in front and the block behind meant a valve dropped into an east-west pipe
  run while the player happened to be facing south pointed at two empty spaces and
  did nothing — with nothing on the block to say so.
- A Valve's readout reported the fluid-node fields it does not have and printed
  `N/A` for all of them. It says what it is moving, and why not when it is not.
- The Liquid Pump, Gas Pump and Drain hold **5000 mL** — one bucket — so a pump
  works standing alone. A pump on two pipes had a 4000 mL network and could never
  accept the 5000 mL source block it was sitting next to, so it refused for ever.
  `network_too_small` is now told apart from `network_full`: one means add a tank,
  the other means wait.

- **One name for one meaning: `cooldown`.** A block that waits N ticks before
  acting again calls that `cooldown`, everywhere, and its readout says
  `Cooldown:`. The Clock's `delay` and the Liquid Drain's `interval` are renamed
  to it and migrate themselves; the three generators' goggles said `Period:` over
  a field called `cooldown`, which is the kind of drift that makes a setting look
  like it belongs to something else. `delay`, `extend` and `pulse` on the Delayer,
  Extender and Shortener stay as they are — those are signal *durations*, not rate
  limits, and folding three meanings into one name would be worse than the drift.
- **The Block Breaker and Block Placer had no cooldown at all**, and the reason
  was the same shape as the Clock's: five near-identical `execute` lines
  re-testing one condition, with the reset on the last. The first line destroys
  the block in front, so `unless block ^ ^ ^1 #air` was false by the time the
  reset re-tested it and the reset never ran — the counter climbed for ever and
  the gate was open on every tick. The placer emptied itself at a block a tick the
  same way. Both are one gated call into a `fire` function now, which resets
  before it touches the world, and five whole-world selector sweeps per block per
  tick became one.
- The Data Handler's numeric-type list went back to being **one entry beside the
  registry** rather than seven per-block functions. The registry exists so that
  adding a property means editing one list; the per-block version undid that.
- `interval` and `rate` were missing from the registry entirely, so the Handler
  could not render them — the Liquid Drain's throughput and the bridges' rate were
  invisible and uneditable. The Industrial Light's `eu_use` and the bridges' `rate`
  are now declared as tuning fields hidden from survival.

- **The Clock ignored its setting, because it had two of them.** A clock could
  carry both `delay` and `cooldown`: the Data Handler listed both, `process` read
  `cooldown`, and `delay` — the one a player naturally reaches for — did nothing.
  The compatibility line meant to merge them only ran when `cooldown` was
  *absent*, which is precisely the case that did not need fixing. The clock now
  runs on a single property, **`delay`, in ticks**, and folds any leftover
  `cooldown` into it on every tick. A delay of 1 pulses every tick.
- **A number typed into the Data Handler could be stored as a string, and every
  block then read its LENGTH.** `data get` does not fail on a string — it succeeds
  and returns the character count. So a Clock set to `"5"` ran with a period of 1
  and one set to `"100"` ran with a period of 3: the number you typed changed
  nothing but the number of digits, which is exactly what "it ignores its setting"
  looks like. Nothing errors and nothing logs. The type detection was never wrong;
  once a value is a string the editor faithfully keeps it one.
  `ra_lib:util/property` now re-parses through a macro — substitution inserts a
  string's raw characters, so `"5"` and `5` both parse as an integer — and writes
  the result back as an int, so the repair happens once and the handler offers a
  number editor from then on.
- **`execute store result … run data get <missing path>` writes zero**, and a
  period of zero means "fire every tick". Three generators and the EU Consumer
  read their period that way with no guard, so a block that lost its property did
  not slow down — it ran flat out. There is now one guarded reader,
  `ra_lib:util/property`, with a default and a floor.
- **The Liquid Valve, Gas Valve and EU Breaker could not be placed at all.**
  Giving them a facing meant `setblock waxed_cut_copper[facing=south]`, and that
  block has no states whatsoever — the command failed and nothing appeared. A new
  `dir_type:3` orients the marker and leaves the block plain.
- **The Block Placer had no cooldown**, so a held signal fired it every tick and
  emptied a stocked placer in under two seconds. Both it and the Block Breaker now
  run at one action per second, hidden from the survival Data Handler.

- **Electric charge could not travel.** Every wire held its own buffer and handed
  half the difference to one neighbour per tick, which levels charge out rather
  than delivering it: it crawled a block a tick, a generator with six neighbours
  fed one of them, and once a run had evened out to within 1 EU the transfer guard
  stopped it moving at all.
- **A reload emptied a network's identity.** `transport/init` overwrote the
  storage holding each network's medium on every load, while the amount sat in a
  scoreboard that survived — so a network filled since the last rebuild came back
  holding contents with no medium, and refused everything its own pumps offered
  until a drain emptied it.
- **Pressure plates did nothing.** The redstone reader knew about dust, levers,
  buttons, redstone blocks, torches, repeaters and comparators, and nothing else.
  Pressure plates, weighted plates, tripwire hooks, observers, lightning rods and
  daylight detectors are all read now, from block tags rather than six hand-copied
  direction tests.
- **A torch standing on a block powered it.** Vanilla never powers the block a
  torch is mounted on.
- **The Randomizer overwrote the redstone library's output with a dice roll**,
  mid-read, on the same marker. One roll in a hundred came up zero and made it
  wipe its own output the instant it fired.
- **Two adjacent skinned blocks deleted each other's appearance.** A skin sat on
  the block corner while the marker sat at the centre, and from a centre every
  surrounding corner is the same 0.866 away — no radius could tell them apart.
  Skins are centre-anchored now, and old ones migrate themselves.
- `facing/up` and `facing/down` both claimed left was east, so flipping a block
  over left its hands unchanged.
- `count_inputs` was a hand-copied mirror of the redstone reader, with a comment
  admitting the two had to be kept in step by hand. They share one implementation.

### Changed

- **The EU Generator is a barrel wearing a furnace skin.** A real blast furnace
  brought two input slots and its own smelting, of which only the fuel slot ever
  meant anything, so players filled the top with ore and waited. Drop coal in a
  box is the whole interaction now. Burn times live in a `fuels` registry beside
  the other tables; steam still works, so the Boiler chain is intact.
- **The Breeder is a barrel wearing a dispenser skin**, for the same reason it
  mattered on the Unboxer: a dispenser throws its own inventory on any rising
  redstone edge, so a breeder loaded with wheat scattered it across the field the
  moment you powered it. It reads redstone directly now, since a barrel has no
  `triggered` state.
- **Tiered pipes and wires are gone.** One Copper Pipe, one Wire. The four pipe
  variants placed the same block with the same marker and the same capacity — gas
  and liquid pipes were never different, and the tiers stopped meaning anything
  once a pipe held a litre. Retired place tags still map to the survivors so items
  already in a chest still place something.
- **The Poppy Generator's `patch` mode is gone.** A second code path over the same
  ground for a block whose whole job is one flower at a time. The Wrench no longer
  cycles anything on it.
- The survival Data Handler **locks fields instead of hiding them**: the value is
  shown with a struck-through red `[Modify]` and a hover saying why. A censored
  row made a block look like it had fewer settings than it does. The gamemode test
  is gone with it — creative players use the Creative Data Handler — and so are
  `redact_next`, `redact_one` and the "N fields hidden" footer. The refusal is
  enforced server-side too, since a row's action is a `/trigger` a player can type.

- The **EU Generator is a barrel wearing a furnace**, not a blast furnace. A real
  furnace brings its own two input slots and its own smelting, of which only the
  fuel slot ever meant anything — so players filled the top slot with ore and
  waited. A barrel is a plain inventory: drop coal in and that is the whole
  interaction, and `ra_lib:skin/apply` puts the furnace back on the outside so it
  still reads as one. It burns solid fuel from its own inventory now, with burn
  times in a `fuels` registry beside the other tables. Steam still works, so the
  water -> Boiler -> steam -> EU chain is intact: fuel is the direct route, steam
  is the built one.

- **The Ender Item Vault's `shared` mode is gone.** Three modes remain — `link`,
  `send`, `receive` — and a vault still set to `shared` becomes `link`, the
  closest of the three. It took two whole-world entity sweeps a tick to work out
  which vault a player was standing next to, and it was the one mode that followed
  people rather than machines, which made it the odd one out in a module about
  automation.

- Bridges — the Liquid Valve, Gas Valve and EU Breaker — **work both ways**. They
  take from whichever side holds more and give to whichever holds less, stopping
  when the two are level. One-way was a mistake: the placement had a right and a
  wrong answer that nothing on the block told you about, and a build that looked
  correct simply did nothing. The facing now picks the axis, not the direction.

- Goggles print units: **EU** on electric readouts, **mL** on fluid ones.
- Wire readouts replaced `Enabled: on` — true of every wire ever placed — with the
  grid's state and its change per second.
- The Solar Panel's `Light:` line said "Grid full" when the grid backed up. Split
  into a grid line and a `Sun:` line that only ever talks about the sun.
- **Wire tiers removed.** L2 differed only in a capacity it no longer has and a
  transfer rate that stopped existing when charge moved onto the grid.
- The **EU Switch** is placed as `waxed_cut_copper`, the same block as the Valve.

- **The Valve is no longer a shutoff.** It is a one-way pump between two networks
  and needs redstone. Existing builds using one as a closed tap will find it does
  not conduct until powered, and then only in the direction it faces. To cut a
  fluid line, remove a pipe. The **EU Switch** is still a true shutoff.
- `ra_lib:redstone/detect` no longer computes look-space scores or the twelve
  direction tags; nothing in the pack read them. `detect_local` does, for anyone
  who wants them. The per-source tags are gone.
- Netherite pipes hold the same 1000 mL as copper ones.

### Documentation

- Every recipe picture regenerated, and the Recipe Atlas rebuilt — 59 recipes.
  Two stale pictures for retired tiers removed.
- The **Breeder now documents what it breeds**: all 22 animals and the food each
  takes, extracted from the dispatch table so the page cannot drift from the code.
- Item reference, interactive machines, infinite generators, transport networks and
  ender links updated for everything above.

### Known

- Nothing in this release has been tested in game.
- Networks still hold **one medium at a time**. Filters, and telling one potion
  from another, wait on that.
- Battery, EU Breaker and Industrial Light have no unlock advancement, and the
  Clipboard and Multimeter have no recipe — both are given by function for now.

## v5.1.6 (2026-08-17) — Recipe Atlas, Data Handler Repairs, Licence

A follow-up to v5.1.5: one page holding every recipe, a Data Handler that no longer
mangles what it edits, blocks that decide for themselves what a survival player may
retune, and a licence that says what it was always meant to say.

### Added

- **[Recipe Atlas](recipe-atlas.md)** — all 58 recipes on one page, A to Z and by
  module, with the station each is made at and the give-everything command. Generated
  from the recipe files, so it cannot drift. It lists the four items that have **no**
  recipe too, with the sacrifice and chance that win them on an
  [enchanting table](enchant-crafting.md).
- Blocks declare which of their own fields the survival Data Handler hides, so one
  block's tuning knob is no longer every block's: `cooldown` is tuning on a generator
  and the whole point of a Clock. Creative mode shows everything.
- `/function ra_ender:debug/vaults` reports each vault's mode, channel and reachable
  partners.

### Fixed

- The Data Handler asked for a **number when editing any string**, and writing one
  broke what it touched — an ender vault channel written that way stopped matching its
  partner, so the vault stopped sending. `data get` succeeds on a string and returns
  its length, which is what the old type test mistook for a number.
- Properties with no editor were invisible, and hidden ones were still printed in the
  raw list. Everything a block carries is shown now; only what a block explicitly hides
  is withheld.
- 23 block types never said they registered, and 13 more said it to **everyone** rather
  than to players wearing `ra.debug`.
- Coal in the **offhand** did not count as jetpack fuel.
- A Teleport Anchor could not fire on a player's first tick in the world.
- Two enchanting tables a block apart could take each other's result.

### Documentation

- The **licence** is rewritten: addons are explicitly free to build and publish, the
  snippets in the docs are licensed for that purpose, and the clause that accidentally
  permitted redistributing the pack "in part and modified" is gone. Adds contributions
  and Mojang-assets clauses, and a version stamp.
- The home page no longer claims redstone on the Boxer and Unboxer is a lock: both run
  while powered, and following that warning would have broken working builds.
- The readme covers the four new modules, with corrected counts (52 blocks, 58
  recipes).

## v5.1.5 (2026-08-17) — Enchant Crafting, Jetpacks, Infinite Generators, Ender Links

Four new gameplay modules, drawn recipe images, a Data Handler that can edit every
property it shows, and two electric transport bugs that had been quietly breaking
wire runs.

**Supported versions:** 1.21.9 – 26.2 (data pack formats 88 – 107).

### New modules

- **[Enchant Crafting](enchant-crafting.md)** — sacrifice items on a vanilla
  enchanting table for a small chance at an upgrade. Extensible through the
  `#ra_enchanting:recipes` function tag.
- **[Jetpacks](jetpacks.md)** — Iron and Infinite Iron upgrade kits that fit onto
  any chestplate. Classic and hover flight, switched with `/trigger ra.jp.mode`;
  hover holds station with a servo that reads your vertical speed and aims gravity
  against it. `/trigger ra.jp.power` switches the whole thing off, and landing does
  it for you.
- **[Infinite Generators](infinite-generators.md)** — a crafted Generator Casing
  plus a Core gambled off an enchanting table build the Mineral, Nether and Poppy
  generators, which regrow their own material in front of themselves.
- **[Ender Links](ender-links.md)** — vaults that link two places by channel for
  items, fluids and EU, plus Teleport Anchors with a string id and a table of
  fifteen targets, one per redstone strength. Item vaults are `shared` by default:
  the contents follow whoever walks up, since mirroring one stack into two barrels
  would give it two extraction points.

### Tooling

- Recipe pictures are drawn from vanilla assets by
  [`tools/recipe_render`](recipe-renderer.md) instead of being screenshotted, with
  blocks rendered from their real models. All 58 regenerated.
- The [Creative Data Handler](developer-guide.md#creative-data-handler) builds its
  rows from a registry and picks the editor from each value's actual type, so
  properties it used to display but could not change — a wire's `transfer_rate`, a
  tank's `tier`, an anchor's id — are editable, lists included.

### Documentation

- The home page warned that redstone on the Boxer and Unboxer had become a lock and
  told players to remove the signal from existing builds. Both have always shipped
  **running while powered** — the inversion was a mid-v5.1.4 workaround, reverted in
  that same release. Following the warning would have broken working builds.

### Fixed

- Electric charge only ever reached the first two blocks of a run: a node handed EU
  back to the neighbour that had just supplied it, and a transfer latch was never
  released. Charge now moves downhill only, half the gap at a time.
- A `enabled` flag written as `1` rather than `1b` silently disabled a whole wire run
  or pipe line. The runtime gates are tolerant now.
- Poppy Generators planted nothing: 26.2 narrowed `#minecraft:dirt`, which no longer
  contains grass blocks.
- Block skins rendered black inside the block they cover.
- Eight multiblock library functions had never been committed, because the directory
  they live in is called `build`.

## v5.1.4 (2026-08-16) — Transport Rewrite, Item Safety, Library Audit

A large maintenance release. The fluid and gas system was rebuilt on a shared
network engine, several item-destroying and item-duplicating bugs were fixed, and
a full audit pass removed dead code and a class of per-tick performance problems.

**Supported versions:** 1.21.9 – 26.2 (data pack formats 88 – 107). Most of the
content does not need 1.21.9 — see the compatibility table on the home page for what breaks on older versions.

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
