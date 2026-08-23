# Changelog

This page mirrors key datapack milestones from the main project changelog.

## v5.1.16 (2026-08-20) — Mixed pipes

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

### Added

- **Networks hold several media at once.** Water, lava and steam can share a pipe
  run, each with its own entry, and the run clogs on the **sum** rather than on
  any one medium. A network used to refuse anything but what it already held, so
  a run was single-purpose for as long as it held anything at all. Nothing refuses
  an offer now except a lack of room.
- **Liquid Filter.** A Valve that passes only the medium it is set to, so a mixed
  run can be sorted back into single-medium branches. It levels per medium: two
  runs each holding 5000 mL are not level to a water filter if one is all lava.
- **Potions keep their effects.** A potion poured into a Drain is 1000 mL of the
  `potion` medium and the network remembers which potion. A Drain set to output
  applies it to everyone within four blocks, with the duration scaled by how much
  was drawn and the level the potion says it has. The first potion into a network
  keeps its identity.
- **`item_name` properties, and [Set from hand].** A property holding an item id
  gets a second button that copies whatever you are holding. Nobody should have to
  spell `minecraft:polished_blackstone_slab` to configure a sorter.
- **Big Torch.** An end rod wearing a torch scaled to a full block tall, always
  standing up. Denies hostile spawns within 1-100 blocks, set with the Data
  Handler. It removes what **spawned** inside the radius and leaves what walked in,
  by remembering every mob in a band 16 blocks past it. Built from Enchanted Coal
  — sacrifice coal on an enchanting table at 1% — nine of which make an Enchanted
  Coal Block, which over a stick makes the torch.

### Changed

- **The Big Torch drops mobs into the void instead of killing them, and its
  radius stops advertising a reach it does not have.** A kill fires the death,
  so a torch in a dark room was a passive mob farm that also lit the room; a
  denied mob is now teleported out of the bottom of the world and nobody
  collects anything. The destination is deliberately not derived from `min_y` —
  void damage starts some distance below it and that distance is not the same
  across the versions this pack supports, so it goes far enough down that no
  supported dimension reaches it. The 100-block ceiling was applied to the
  working value but not written back, so a torch set to 500 swept 100 while the
  goggles and the Data Handler both said 500.

- **Item Pipe filters are a property, not an item frame.** Set with [Set from
  hand]; the goggles draw the item it is filtering for, above the id. Reading a filter used to
  mean an entity selector per pipe plus a `block_pos` comparison per candidate
  frame, cached and rescanned every 20 ticks — so a frame you had just hung did
  nothing for up to a second. It is now one `data modify` with no selector, no
  cache and no stale second. Existing pipes migrate; their frames are left alone.
- **Tools no longer stack.** Every tool is `max_stack_size=1`. They carry
  per-tool state — the Clipboard's origin, the Remote's channel — and two in one
  slot is a stack whose state belongs to whichever was picked up last. The
  bundles still hold the full set: `bundle_contents` set by a command is not
  subject to the weight limit that applies to inserting by hand.
- **A network keeps its number.** Ids used to be handed out 1, 2, 3… on every
  rebuild, so they said nothing about identity: break one run and every run
  numbered above it slid down one. The Multimeter would report "Network 22", the
  run would be torn out, and `nets.n22` would still be sitting in storage
  afterwards holding somebody else's water — which reads exactly like a network
  that refused to die. A component that still contains the old network's root now
  inherits its number, anything genuinely new takes the next number a counter has
  never issued, and a retired number is never issued again.
- **The Goggles say `Multimedium`; the Multimeter says what is in there.** A
  billboard is one short line read from across the room, and
  `Water 5000 mL, Lava 5000 mL, Steam 2000 mL` is neither short nor legible at
  that distance — so a mixed run reads as `Multimedium` on the Goggles over the
  usual total, and the Multimeter prints a line per medium in chat, where there
  is room. A single-medium run still reads as its plain name, which is what the
  Data Handler and every single-medium block expect to read back.
- **The Electric Furnace's top mode cost three EU Generators to run.** 1000 EU an
  item at five ticks an item is 200 EU/t, or about twenty Solar Panels across a
  daylight cycle — priced out of the game rather than expensive. The table is now
  anchored on one EU Generator at 60 EU/t: 40/80/160/300 EU per item for
  low/medium/high/superpowered. Each step still costs more per unit of speed than
  the one below it.
- **The Creative Fluid Source tops up at a rate instead of claiming the run.** It
  offered the whole of the network's free space every tick, which is what a
  single-medium source wanted and exactly the wrong thing now that a run can hold
  several media: one source took the entire network on the tick it was placed —
  100000 mL of water in one go — and every other source, Pump and Drain on that
  run then found a full network for ever. A mixed run could not be built anywhere
  one was attached, and it looked like the run refusing the new medium rather
  than like the source having already taken the room. It now adds `rate` mL per
  tick, 1000 by default and settable on the Data Handler, so sources sharing a
  run interleave.

### Fixed

- **A nameless total survived every rebuild, and renamed itself to whatever came
  next.** `rebuild/snapshot_read` would carry a network's bare `amount` across a
  rebuild when it had no breakdown to carry, on the grounds that losing the
  contents silently was worse than losing their name. It is worse. The carried
  total had nothing to reconstruct `amounts` from, so the next rebuild carried it
  again, and the first medium offered afterwards became the only entry in `media`
  and therefore the name of all of it — 15000 mL of nothing reporting itself as
  15000 mL of the next thing you poured in. Networks reached a state holding a
  five-figure amount with no medium at all, which neither migration could repair
  because both are gated on a `medium` those networks did not have. A rebuild now
  carries the breakdown and nothing else, and the total is summed back from it,
  so `amount` is always exactly the sum of `amounts`.
- **The read-time migration could never fire on a network that needed it.** It
  tested `unless data ... media`, and `rebuild/reset_net` writes `media:[]` — an
  empty list is present as far as `if data` is concerned, so a network holding an
  amount with an empty breakdown was never migrated. `rebuild/snapshot_read` made
  the same test as `media[0]` and got the right answer; the two disagreeing is
  what let the state persist.
- **The Big Torch, the Magic Crate and the Breeder each duplicated their own
  block.** All three killed the vanilla drop with a bare `kill @e[type=item,
  ...,distance=..2]` — no `execute as @e[tag=ra.broken,...] at @s run` in front of
  it — so the distance was measured from wherever the module's tick function
  stood rather than from the block that had just been broken. The end rod or
  barrel survived and was handed back alongside the custom item. Every other
  block in the pack already wrapped this correctly. The bare form also ran every
  tick, so any end rod or barrel item that drifted near the tick position was
  deleted.

- **A rebuild collapsed a mixed run into one medium.** Pour 5000 mL of water into
  a network, place a block — any block, anywhere on the run — then pour 5000 mL
  of lava in, and it reported 10000 mL of Lava. The rebuild that runs when the
  topology changes parked only the total and the primary medium's name on the
  network's root node; the per-medium breakdown was dropped, and because a fresh
  network is written with an empty `media` list rather than none at all, the
  read-time migration never fired to repair it either. Whatever was offered next
  became the front of the list and so became the name on the whole total. The
  breakdown is now carried through the rebuild in full, and the total is summed
  from it rather than read separately, so the two cannot disagree.
- **A rebuild forgot which potion a network held.** Same cause: `potion` lives on
  the network next to the amounts, and nothing carried it across. A potion run
  lost its effects the moment a block was placed anywhere on it.
- **The Solar Panel made a twentieth of its stated output.** It generated on a
  20-tick duty cycle but kept the per-tick amount, so a panel peaked at 2.5 EU/t
  rather than the 50 EU/t the Electric Furnace's whole mode table is priced
  against. Twenty panels came to 50 EU/t at absolute noon, which an Electric
  Furnace on superpowered (60 EU/t) outruns for ever — so a base with twenty
  panels and two Batteries sat pinned at zero stored EU in full sun, with nothing
  visibly wrong. It offers what it makes every tick now. Batching it up instead
  would average the same and behave worse: a 1000 EU burst needs somewhere to
  land on the tick it arrives, and a panel contributes only 50 of capacity.
- **The Solar Panel never showed its own output.** Its readout was the grid's
  total, so there was no way to see the rate above from in game. It publishes
  `Making: N EU/t` beside the sun line, as the EU Generator already did.
- **Transport Networks' figures.** The Boiler was documented as 100 water to 100
  steam per cycle; it moves 1000 mL each way, every 20 ticks. The capacity table
  put Pumps and Drains at 2000 mL when they hold 5000, and had no row at all for
  the Gas Tank, either Ender vault, the Electric Furnace or the Creative EU
  Source.
- **The Data Handler's registry never reached an existing world.** It was seeded
  lazily, `unless data storage ra:dh numeric`, from three call sites — so a world
  upgraded from an earlier version already had `numeric`, the guard passed, and
  the registry was never rewritten. It kept the old list, with no `filter_item`
  and no `filter_medium` in it and no `item_names` list at all, so neither filter
  had a row in the editor on any block. It is now written on every load, like the
  wrench and readonly registries beside it, and a name added to it reaches an
  existing world on the next `/reload`.
- **The Creative Fluid Source's medium could only be set with the wrench.** Its
  `medium` property was never in the Data Handler's registry, so it had no row —
  and neither did `enabled`, which the wrench toggles on emitters and multiblocks.
  Both are registered now. A medium typed rather than cycled is checked against
  the media registry before anything is pushed, because free text can name a
  medium that does not exist and a junk key in a multi-medium network is one
  nothing could ever drain.
- **The Data Handler had no row for either filter.** It draws a row only for a
  property the block actually has, and Item Pipes were placed carrying none at
  all, while `filter_medium` was never added to the registry. Pipes are now
  placed with an empty `filter_item`, existing ones get it from the migration,
  and both names are registered.
- **The same item came out different depending on where it came from.** Twenty
  items had lore that disagreed between the recipe, the give function, the
  namespace bundle and the block's own break drop — so a crafted Liquid Filter
  and a broken one were different items and would not stack. All four sources now
  agree, from the recipe.
- **The Redstone Remote's recipe claimed `Channel: default` for ever.** The lore
  line was static and `set_channel` is a `copy_custom_data` modifier that never
  touches lore, so a remote switched to another channel went on saying default.
  The line is gone.
- **The up and down sides read every redstone source as 0.** `ra_lib:redstone/side`
  substituted the back direction into a `redstone_wire` connection state, which
  has no `up` or `down` — so the macro line failed to parse and stopped the whole
  function before it ran. A lever sitting directly on a block never turned it on,
  and neither did a torch underneath or a redstone block on top. Every block using
  the library was blind above and below.
- **The readme's counts.** The subtitle claimed 52 blocks, 5 tools and 58 recipes
  while the badges beside it said 57, 7 and 66; the badges were right. The block
  and tool reference tables were short by four blocks and the Redstone Remote.

## v5.1.15 (2026-08-19) — Settings

### Added

- **[Settings](settings.md).** Two kinds, reached two ways. Server settings live
  under `/function ra_settings:admin/` and autocomplete — a function path is the
  only command surface that does. Player preferences are
  `/trigger ra.settings.open`, which needs no permissions. `/function ra:settings`
  is the short way in.
- **Blocks can be turned off**, per type, with a page listing which and a warning
  on load. The item is handed back rather than swallowed, and anything already
  built keeps working.
- **Defaults can be retuned** — generator EU/tick, clock interval, vault rates,
  crate radius. Applied at placement, with **[Apply to placed]** to push a change
  onto blocks already standing.
- **Sound and particle switches, per player.** All 118 `playsound` and `particle`
  calls respect them.
- **Uninstall warns twice** and says what it is about to destroy.

### Fixed

- **Ender vaults could not find each other** — the tag a sending vault searches
  for was cleared every tick and never set. Broken since v5.1.8, in all three
  vault types.
- **The Electric Furnace flickered and z-fought** while working; a working furnace
  was drawn as switched off for four ticks in five on superpowered; a steam-fed EU
  Generator ran while drawn permanently unlit.
- **Jetpack upgrade kits fired with the jetpack off** — in normal mode, jumping and
  sprinting gave you the Thruster for free — and kept working on an empty tank.
- **Text input never completed anywhere it was used by the settings**, because the
  session was opened at the wrong point in the tick.
- **`/trigger` completion is no longer cluttered**: nine blanket-enabled triggers
  down to one.

## v5.1.14 (2026-08-18) — Pictures

### Added

- **Screenshots in the wiki.** Transport Networks opens on a running EU grid and
  shows a fluid network, the Electric Furnace and the Industrial Light lit beside
  unlit; Jetpacks shows hover flight. Seven in `docs/images/screenshots/`.
- **A banner per module in the README**, with a generated
  `docs/images/banners/media-missing.png` placeholder for the ones that have no
  screenshot yet. Swapping a placeholder for a real picture is a one-line edit.
- An **Industrial Light** section in Transport Networks, which the page never had.

### Fixed

- **The README was stale in ways that mattered.** It listed Netherite Liquid Pipe,
  Netherite Gas Pipe and Netherite Electric Wire as separate blocks — the tiers
  were removed, and those item tags now place the single version. It was missing
  eight Transport Networks blocks (Battery, EU Breaker, Industrial Light, Boiler,
  Solar Panel, Electric Furnace and both Creative sources), the Magic Crate, the
  three new jetpack kits, and the Clipboard and Multimeter. Counts corrected:
  **57 blocks, 7 tools, 66 recipes** — the badges said 52, 5 and 58.
- **Transport Networks still described tiered pipes** in its Block Families table,
  and pointed at `ra_wires:items/give_all`, which does not exist. It now names the
  Wires Bundle and the creative give function.
- `readme.bbcode` regenerated from the corrected README.

## v5.1.13 (2026-08-18) — Documented

### Added

- **Recipes for the Clipboard and Multimeter.** Both shipped with a give function
  and nothing else, so in survival they did not exist.
- **A [Tools](tools.md) page**, which the wiki never had. The Wrench, Goggles,
  Data Handler, Clipboard and Multimeter were described only in scattered module
  notes and the technical pages; the Clipboard and Multimeter had no usage
  documentation at all, only a row in the item table.
- **Missing unlock advancements** for the Battery, EU Breaker, Industrial Light,
  Boiler, Solar Panel — and the Wrench and Data Handler themselves — plus `get_*`
  advancements for the Battery, EU Breaker,
  Industrial Light, Clipboard and Multimeter. Those blocks were craftable but the
  recipe never unlocked itself.

### Removed

- **Three dead advancements**: `unlock_electric_wire_netherite` and
  `unlock_liquid_pipe_netherite`, left behind when the tiered wires and pipes were
  removed, and `unlock_conveyor`, which pointed at a recipe and a block that do
  not exist anywhere in the pack.

### Changed

- The wrench and Data Handler sections of **How It Works** described the old
  per-block dispatch and the goggles tinker. Rewritten for the registry-driven
  menu, including the `stacked_prop_line` / `stacked_status_line` /
  `stacked_data_line` distinction that silently renders `N/A` when you pick wrong.
- The **Developer Guide** now documents the migrations system and the read-only
  registry.

## v5.1.12 (2026-08-18) — Depth

### Fixed

- **Skins z-fought with the block underneath.** The overlay was scaled 1.004,
  leaving two thousandths of a block of clearance on each face. That is
  geometrically separate and practically not: Minecraft's depth buffer loses
  precision with distance, so a few chunks out both surfaces resolve to the same
  depth and flicker. The margin is now 1.02 — a hundredth of a block per face,
  five times the separation, and still about a sixth of a pixel of overhang on a
  sixteen-pixel texture, so nothing looks fat. Where a neighbour is solid the
  overhang is buried inside it and never drawn.
  `ra_migrations:5.1.11-to-5.1.12` clears existing skins so they redraw with the
  new geometry — displays keep whatever transformation they were summoned with,
  and every skinned block already redraws a missing skin within a tick.

## v5.1.11 (2026-08-18) — Read Only

### Added

- **One declaration for read-only properties**, `ra:tools/readonly/init_registry`,
  keyed by block type. A property listed there is shown by the Data Handler with a
  struck-through `[Modify]` and a reason on hover, and is **never offered by the
  wrench**. Marking something the block owns — a generator's `generation_rate`, a
  valve's `rate`, a breaker's `cooldown` — is now one edit that both tools obey,
  instead of one tool knowing and the other not.

  This replaces five per-module `tools/hidden_fields.mcfunction` files reached
  through a `#ra:hidden_fields` function tag, each a chain of tag tests — the same
  per-block-table shape the Data Handler registry was consolidated to remove. The
  set is stored as a compound rather than a list because both readers ask "is
  *this* name read-only?", which a compound answers in one command and a list
  needs a walk for.

### Removed

- **`enabled` from the Ender vaults, the Teleport Anchor and the Infinite
  generators.** None of them had anything that could toggle it — no redstone, no
  wrench action — so it was a property you could set and then watch do nothing.
  It stays on the **Wireless Emitter/Receiver** and the **Multiblock Base**, which
  are toggled by the wrench's plain right-click and would have no control at all
  without it. `ra_migrations:5.1.9-to-5.1.10` strips the dead copies.

### Fixed

- **The wrench filtered read-only entries after counting them**, so a block whose
  only cyclable property was read-only would have cycled it silently on a plain
  shift-click instead of reporting that it does not cycle. The filter now runs
  first, and both the menu and the click path build the list through the same
  function — a button carries a row *index*, so a click path that built the list
  any differently would act on a different property than the one on screen.

## v5.1.10 (2026-08-18) — No Second Switch

### Fixed

- **The Electric Furnace duplicated items** — it smelted without consuming the
  input. When `find_try` was rewritten so every slot counts as input, the line
  that put the slot number into `ef.hit` went with it and only the score was left.
  `take_input` is a macro function reading `ef.hit`, so it had no `$(slot)` to
  substitute; a macro with a missing argument fails without running a single one
  of its lines, so nothing was removed **and** the `#ef.took` flag it was supposed
  to clear still held the `1` from the previous successful smelt. `deliver` ran on
  that stale flag and produced an ingot from an ore that was never eaten. The slot
  is back in `ef.hit`, and the caller now clears `#ef.took` itself, so a take that
  does not run can no longer read as a take that did.

### Removed

- **The `enabled` property, throughout `ra_wires`.** It was a second off switch
  sitting next to redstone, on blocks that mostly had nothing else to configure —
  so the new wrench menu opened on a great many blocks just to offer one useless
  row. Breaking the wire does the same job and reads better.
  `ra_migrations:5.1.9-to-5.1.10` strips it from existing markers so it stops
  appearing in the Data Handler as a property that does nothing.

### Changed

- **The EU Switch runs on redstone.** It was the one block whose only control was
  `enabled`, which meant the only way to work a switch was to open a menu — in a
  redstone pack. Powered conducts, unpowered cuts, matching the EU Breaker and
  the valves, so a lever wired to it now does what a lever obviously should.
- **Menu buttons are bold and spaced** — `[ CYCLE ]`, `[ ON ]`, `[ OFF ]`,
  `[ REMOVE ]` — so they read as buttons and are easier to hit.

## v5.1.9 (2026-08-18) — One Tool

### Added

- **The wrench opens a menu on blocks with more than one setting.** Shift+RMB
  still cycles immediately when a block has exactly one cyclable property — a
  menu with one button is a worse button — but a block with two or more now lists
  them with their current values and a `[Cycle]` button each.
- **A cyclable-property registry**, `ra:tools/wrench/init_registry`. The wrench
  used to be a chain of "if this block tag, run that block's cycler", which
  allowed exactly one setting per block; anything that wanted a second had to
  borrow another tool. A block now declares a *list* and the wrench works out
  whether to cycle or to open a menu. Adding a setting is an edit in one file.
- **Markers record their block type** in `data.type`, because the registry has to
  be keyed by something and a data pack cannot ask an entity which of its tags
  names its kind. `ra_migrations:5.1.8-to-5.1.9` fills it in for existing worlds.

### Removed

- **The goggles tinker.** Sneak+goggles used to cycle modes and toggle `enabled`,
  which meant two tools that both changed blocks with no rule about which owned
  what — the Electric Furnace ended up with its output on the wrench and its power
  mode on the goggles, and a wrench message addressed to a tag only the goggles
  ever set. The goggles read; the wrench changes. Everything the tinker did is on
  the wrench now, including the `enabled` toggle, which is a menu entry on every
  RA Wires block, and the Liquid Drain's drain/place mode.

## v5.1.8 (2026-08-18) — Reach, Lift, Scorch

Three bugs that all looked like "the block just does nothing", and two new toys.

### Added

- **Creative EU Source** and **Creative Fluid Source** (`ra_wires`) — make power
  and fluid out of nothing, for building and testing the consuming half of a
  system without also running a fuel farm to feed it. The EU source refills its
  grid to capacity every tick, so a machine on a creative grid never browns out;
  the fluid source fills with a medium cycled by the wrench, because a network
  holds one medium at a time and a source stuck on water could not test a lava
  line. No recipes, on purpose; both ship in the Wires Bundle, or
  `/function ra_wires:items/give_creative` for just the two.

### Added

- **An upgrade menu, `/trigger ra.jp.kits`.** Lists the three kits with their
  state and two buttons each: switch one off without removing it, or take it off
  and get the kit back as an item. Re-shown after every action, so it behaves like
  a panel rather than a one-shot message.

### Added

- **Electric Furnace** (`ra_wires`) — smelts with EU and no fuel at all, at up to
  forty times a vanilla furnace. Four power modes cycled with the wrench:

  | Mode | Ticks per item | EU per item |
  | --- | --- | --- |
  | low | 100 | 40 |
  | medium | 50 | 100 |
  | high | 20 | 300 |
  | superpowered | 5 | 1000 |

  EU per item climbs faster than speed does, on purpose — four times quicker for
  four times the power would make every mode but superpowered pointless.

  **Input and output are split by slot inside one barrel:** the top row (slots
  0–8) is input and is only ever read, rows two and three (slots 9–26) are output
  and are only ever written. Results are also pushed into any container directly
  below. That combination is what makes hoppers work with no configuration — a
  hopper feeding the block fills the lowest free slot, which is the input row,
  while extraction has to be a push because a hopper underneath would otherwise
  pull unsmelted ore out of slot 0. With nothing below, results stay in the output
  rows, so it still works as a standalone block you open by hand.

### Added

- **Every entity the pack owns now carries the plain `ra` tag**, so
  `/kill @e[tag=ra]` sweeps up the pack and nothing else. A pack built out of
  markers, block displays and text displays has no other way to be cleaned up, and
  killing every marker and display in a world takes other people's builds with it.
  Worlds built before this are caught by `ra_migrations:5.1.7-to-5.1.8` on load.
- **A `ra_migrations` namespace**, one function per version step, named for the
  step it bridges. They run oldest-first from `ra:load`, and every one runs on
  every load — so each must be safe to run twice and may only fill in what a
  newer version expects, never overwrite. (`-to-` rather than `->`: a resource
  location path may only contain `[a-z0-9_.-/]`, and a file with `>` in its name
  is skipped by the datapack loader.)
- **Magic Crate** — a barrel that reaches out and takes
  dropped items from up to 20 blocks away. `radius` (5–20) and `cooldown` are both
  editable in the Data Handler; the sweep takes at most eight items per pulse so a
  hopper standing over a mob farm never spikes the tick. Whole stacks cross
  verbatim, so names, enchantments and damage survive the trip. It refuses items on
  a permanent pickup delay, and drops its whole 27-slot inventory when broken.
- **Jetpack upgrade kits**, three of them, right-clicked while wearing a jetpack:
  - **Thruster** — +45% movement speed, in both flight modes and on the ground.
  - **Lift** — climbs at about six blocks a second instead of three, and sinks
    faster by dropping the slow-falling cushion.
  - **Scorch** — sets fire to anything in a 3×3 column six blocks under your
    exhaust, with flame and lava particles. Players, dropped items, and every
    marker and display entity the pack is built from are excluded, so flying over
    your own base does not burn your machines down.

### Fixed

- **The wrench cycled the Electric Furnace's output in silence.** Its message was
  addressed to `@a[tag=ra.wires.tinker_user]`, which is the tag the *goggles*
  tinker puts on the player. The wrench never touches the player — it runs `as`
  the marker off a raycast — so the selector matched nobody. It now messages
  `@a[distance=..10]` like every other wrench action.

### Fixed

- **The Thruster never engaged in classic mode.** It was gated on a speed floor
  of 0.18 blocks a tick, and in classic you fly by holding sneak — horizontal air
  movement never gets near that, because air control is a small fraction of
  walking speed. The floor was also the wrong idea in general: it cannot tell
  crossing terrain from lining up a block, since both can be slow. **Hold sprint**
  to engage it instead — an explicit input, meaning "go fast" the same way it does
  everywhere else in the game, identical in both flight modes, and nobody sprints
  while placing blocks, which is where the jitter was unwelcome.

### Fixed

- **Every Electric Furnace readout said `N/A`.** The billboard library has both
  `data_line`, which reads `data.status`, and `stacked_data_line`, which reads
  `data.data` — different places behind near-identical names. The furnace wrote
  `data.status` and asked the stacked reader for it, so every value was simply
  absent and rendered as the fallback. Added the missing `stacked_status_line`
  and documented the four-way naming trap at the top of it. The Magic Crate's
  state line had caught the same edge.
- **A switched-off jetpack kit could never be switched back on.** `kit/toggle`
  removed the tag, then tested for it again on the next line — after its own
  removal — and put it straight back, so every click ended muted. Same shape as
  the Block Breaker cooldown bug: a condition re-tested after your own first line
  invalidated it. The state is now read into a score before anything changes it.

### Fixed

- **Standing on the edge of a block counted as flying**, so the Scorch kit set
  fire to whatever was below a player who was, from their own point of view,
  standing still on solid ground. The airborne test sampled one point under the
  player's exact centre, which is over the drop when you stand on a block's edge
  while your feet are still on the corner. It now samples all four corners of the
  hitbox as well — a player is 0.6 wide, so +/-0.3 — and any one of them over
  something solid means supported, which is how vanilla decides it too. Shared by
  the Thruster, which had the same bug more quietly.

### Fixed

- **The Thruster kit did nothing you could feel.** It raised
  `minecraft:movement_speed`, which governs *walking*: once you are off the
  ground, horizontal movement runs on a much smaller air-control factor, so the
  attribute was a lever that is barely read in flight. It now measures how far you
  actually moved last tick and adds 55% of it back, capped at 0.2 blocks a tick
  each way — so it accelerates rather than snapping to speed, pushes whichever way
  you are really going (strafing and reverse included), does nothing at all while
  hovering still, and fades on its own as air drag shrinks the delta. It refuses
  to push you into a wall, since `tp` does not collide.

### Fixed

- **The Scorch kit never set anything on fire.** Its selector carried
  `type=!boat`, and there has been no `minecraft:boat` entity type since 1.21.2 —
  it was split into `oak_boat`, `birch_boat` and the rest. One unknown entity type
  makes the whole selector fail to parse, so the command never ran, while the
  particles — separate commands — carried on looking correct. The selector now
  names only three certain types and excludes this pack's own entities with
  `tag=!ra`; the decoration exclusions moved into one guard line each, so a future
  rename costs one guard rather than the entire feature.
- **Scorch now works in hover mode.** It was gated on an airborne score computed
  earlier in the player tick; it re-tests the ground itself, so it no longer
  depends on where it is called from.

### Fixed

- **The EU Generator's skin vanished.** Teaching it to glow wrote the state into
  the block's NAME — `minecraft:furnace[lit=true]` — and a block_display's `Name`
  is a resource location, which cannot contain brackets. It does not error: the
  display spawns showing nothing, so the block simply loses its skin. Block states
  now go through `ra_lib:skin/apply_lit`, which puts them in `Properties` where
  they belong.
- **A burning generator reported itself inactive.** `active` was set from how much
  EU the grid accepted, so a generator with no Battery filled the 50 EU it
  contributes itself and then read as doing nothing — fuel going down, skin lit,
  readout saying idle. `active` now means burning, and a separate line says
  whether the grid is accepting or full.
- **An unlit Industrial Light re-cleared its whole beam every tick**, about sixty
  commands per light per tick for lights that were simply switched off. It now
  clears on the tick it goes out, with a sweep every ten seconds for beams
  orphaned by a chunk unload.


- **A vertical Liquid Drain ignored everything above it.** It returned early
  unless a player was sneaking next to it, so a barrel of water buckets sitting on
  top — or buckets dropped on the block — did nothing at all, and the hand was the
  only way to load a network. It now tries the hand, then a container above, then
  loose items above. Only the hand path still requires sneaking; the other two run
  unattended, which is what makes a hopper-fed loading point possible.
- **Status billboards rendered inside their own block.** The text anchor was the
  block centre, and the block's top face is only 0.5 above that — so every ladder
  in the pack, which counts *downwards* from its first line, put its third line
  onwards under the top face. Blocks with three or more status lines could only be
  read by standing inside them. The anchor is now above the block, and a floor in
  `show_literal_line` means no ladder can reach back down into it however long it
  gets.
- **EU Generators reported "No fuel" while full of coal.** The fuel check tested
  `Items[0]`, the first occupied stack. A barrel has 27 slots: one stack of
  anything that is not fuel — a stray cobble, an empty bucket left by a hopper —
  sits in front of the coal for ever. It now asks the whole container and takes
  from whichever slot has it. A generator left over from when the block was a real
  blast furnace now says so instead of silently reading the wrong container, and
  the goggles name the fuel ("Coal") rather than printing a registry ID.
- **The Industrial Light never lit, however much EU was on the grid.** It read its
  own `eu_use` with a bare `data get`, and a failed read of a missing path does not
  error — it stores **zero**. The light asked the grid for nothing, got nothing
  back, and read "took 0" as "the grid cannot pay". The amount was never the
  problem, so no amount of EU could fix it. It now reads through
  `ra_lib:util/property` like every other numeric property, and the goggles say
  which of the two conditions is missing — "No redstone" or "No EU" — instead of a
  bare "Dark".
- **Valves and EU Breakers left one side empty while the other stayed full.** They
  compared raw stored amounts, so a tank farm holding 2000 mL of 300000 looked
  *fuller* than a three-pipe stub holding 3000 of 5000, and the bridge called it
  balanced. They now compare how full each side is, not how much it holds.
- **Valves ping-ponged the same litre forever.** Moving the whole difference
  between two sides swaps them rather than levelling them: 3000/2000 became
  2000/3000 and back, which is what the steady "Moving: 1000 mL" was. They now move
  half the gap, which converges.

### Changed

- **The Creative Fluid Source is a beacon**, matching the Creative EU Source. A
  sponge reads as something that *absorbs* fluid, which is the opposite of what it
  does. Both break handlers now clear dropped beacons within one block rather than
  two, so two creative sources side by side cannot eat each other's drop.

### Changed

- **Skins take their brightness from the light around them.** A block_display
  samples light at its own position, which is inside the block it draws, where the
  light is always zero — so every skin needed a brightness override or it rendered
  pitch black. That override was hardcoded to `block:0`, right in daylight and
  wrong beside a torch: the real block would be lit and its skin would not. The
  level is now sampled one block above, by binary search over fifteen light
  predicates — four questions rather than fifteen.
- **The Thruster only engages at travel speed.** A data pack cannot set a
  player's velocity: the only server-side ways to move a player are teleporting,
  knockback and vehicles, so a continuous boost has to be a teleport a tick, and
  that is visible as jitter no matter how well the magnitude is smoothed. What
  can be fixed is *when* it fires. It now engages above 0.18 blocks a tick and
  holds until you drop below 0.10 — two thresholds so a single one at cruising
  speed cannot flicker. Crossing terrain it works as before; placing blocks it
  stays out of the way entirely.

### Changed

- **The Electric Furnace pushes its results to a face you choose** — under,
  front, back or top, cycled with the **wrench**. The old top-row-in/lower-rows-out
  slot split stopped it re-smelting its own output but made automation awkward,
  because a hopper cannot be told which rows to touch. Now any smeltable stack
  anywhere in the barrel is input and nothing ever comes back in, so there is no
  slot rule to remember and no way for it to find its own product. It refuses to
  smelt when the destination is missing or full — checked *before* any EU is spent
  or any input consumed, so a blocked output costs and loses nothing.
  Power mode moved to the **goggles tinker**: two tools, two settings.
- **The furnace skin is a blast furnace**, matching the item you place it from,
  and it now places a light above itself while running — a skin is only a picture
  and emits no light of its own, unlike the vanilla lit block it imitates.
- **The Thruster no longer jitters.** It pushed a fraction of last tick's *raw*
  movement, and a single tick's delta is noisy — it carries the player's input,
  collision nudges and the previous push — so every wobble became a
  different-sized teleport, which is exactly what renders as shaking. The delta
  now feeds a running average (`smoothed = (smoothed * 3 + delta) / 4`, about a
  four-tick time constant) and the push is built from that, so it changes
  gradually. Below 0.025 blocks a tick it does not teleport at all, which removes
  the twitching while drifting to a stop.

### Changed

- **Scorch deals real damage, not only fire.** `/damage 3 minecraft:on_fire`,
  attributed to the pilot so the kill counts and drops experience, on a ten-tick
  cadence — at one tick apart it would be twenty hits a second, which deletes
  anything under a hovering player and makes the burning itself pointless.
  Burning is still applied every tick.
- **Scorch reach is exactly 6 blocks**, down from seven.
- **Scorch runs only while airborne**, gated both at the call site and inside the
  function.
- **No more lava particles** in the exhaust — the popping blobs read as dripping
  stone rather than flame. Flame and small_flame only, with the width coming from
  a much wider spread instead of from the count, and the plume widening with
  depth the way thrust actually does.
- **The Thruster kit is stronger again** — 80% of last tick's travel added back
  (was 55%), capped at 0.35 blocks a tick each axis (was 0.2), about seven extra
  blocks a second on top of whatever you were already doing.

### Changed

- **Scorch's exhaust is far calmer.** It was about a hundred particles a tick
  including `explosion` billows and `large_smoke` — a soot cloud rather than a
  jet, grey swamping the orange and thick enough to hide your own boots. Now
  flame and small_flame for the shape with a single `lava` for weight: no smoke,
  no explosion, nothing grey.

### Changed

- **The Thruster kit is much stronger** — +120% movement speed rather than +45%.
- **Scorch's exhaust is a plume, not a pilot light.** `flame` alone reads as a
  spark however many you ask for; the bulk is `lava` and `large_smoke`, and
  `explosion` is what makes it puff — one large soft billow does more than
  hundreds of flame particles.
- **Converting to an infinite jetpack says so when it keeps your kits.**

### Changed

- **Jetpack upgrades now live on the chestplate, not on the player.** Storing
  them as player tags meant they followed you onto a different chestplate,
  survived losing the jetpack entirely, and — the way it actually showed up —
  made every kit report itself already fitted for ever after the first one. The
  chestplate is now the record: its `custom_data` carries the flags, the lore
  lists them, and the flight code re-derives its tags from the worn item every
  tick, so a jetpack handed to someone else arrives with its upgrades and taking
  one off stops you having them. Converting an iron jetpack to infinite keeps
  the kits fitted to it instead of stripping them.
- **The Industrial Light's beam walk no longer depends on constructs I could not
  verify.** The mode travelled as the word `"on"` in storage and was tested with
  a root-level compound-filter path, on lines that began with `$` while
  containing no macro placeholder at all — all of it in the one code path that
  was silently doing nothing. It is now a number in a score compared with
  `matches`, and empty space is tested as `minecraft:air`/`minecraft:cave_air` by
  name rather than through the `#minecraft:air` tag.

### Changed

- **The EU Generator now walks its own inventory instead of interrogating the
  fuel registry.** Two earlier attempts asked the registry first — "is there coal
  in there? charcoal?" — sixteen questions per attempt, first against `Items[0]`
  and then via `execute if items ... container.*`. Both tested the container
  indirectly, and when it kept reporting no fuel with a full stack of coal in it
  there was no way to tell which half was lying. It now reads `Items` the way the
  rest of the pack reads containers, looks each stack's id up in a new id-keyed
  `fuel_map` in one step, and takes it out by index. Also cheaper: one lookup per
  stack present, rather than sixteen tests regardless.
- **The Thruster kit no longer applies on the ground.** A flight upgrade that
  makes you walk faster around your base is not a flight upgrade; it now applies
  only while airborne, and comes off cleanly on landing.
- **Fitted jetpack kits are listed on the chestplate**, appended to its lore so
  the tier line survives. (They still live on the player, so swapping chestplates
  carries the upgrades but not the lore.)
- **The Scorch kit shows itself.** Flame, lava and falling-lava at the boots plus
  a visible column down the burn reach, and the reach went from six blocks to
  seven.

### Changed

- **The Breeder stops before it starts.** It ran thirty-five `if items` checks
  against its container every tick — one per animal/food pair — before any of
  them could match, and paid all thirty-five whenever nothing did, which is
  nearly always: animals wander off, and the ones present are usually already in
  love or still on cooldown. One entity-type-tag check now answers "is there an
  animal here that could breed right now?" and skips the other thirty-five.
- **The Unboxer stopped pretending sixteen commands were guarded.** Its
  double-chest handling was sixteen macro lines each reading
  `$execute positioned $(input1) run execute if score ... matches 1 ...`. Written
  that way all sixteen still run, still instantiate a macro and still resolve the
  position, every pass, even for a single chest that can never match. The
  condition and the positioning moved to one call site.


- **EU Generators show when they are running** — the furnace skin lights, they
  emit smoke and flame, and they cast a light level 10 above themselves. The repaint
  only happens on the tick the state actually changes.
- New debug readouts: `/function ra_wires:debug/generator` and
  `/function ra_wires:debug/light` report, per block, every condition that has to
  hold for it to work.

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
