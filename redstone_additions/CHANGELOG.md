# Changelog

## [v6.0.3] - Unreleased

### Added

- **Counter** (Logic Gates). Counts the times its input turns on and fires when the count
  reaches its *target* (1 to 1000, default 10). By default it then starts again from 0 and gives
  a 2-tick pulse, so it divides a pulse train; with *hold* on (Shift+RMB with the Wrench, or the
  Data Handler) the output stays on until it is reset. Redstone on the top of the block resets it
  to 0; any other side counts. The Goggles show the count and the target.
- **Edge Detector** (Logic Gates). Gives a pulse of *pulse* ticks (default 2) when its input turns
  on (*rising*), turns off (*falling*), or both. Shift+RMB with the Wrench cycles the mode.
- **Light Sensor** (Sensors). Outputs the light level of the block in front of it as a redstone
  signal of the same strength, 0 to 15, read once a second. It faces the way it was placed, like a
  dispenser, and is itself the source: dust, a repeater or a comparator next to it picks up the
  level, no iron blocks needed. The Goggles show the level and the facing. Crafted from three glass,
  two redstone, a daylight detector and three cobblestone. The world sensors share one slow clock
  that only runs while one of them is loaded.
- **Weather Sensor** (Sensors). Turns on while it rains, during a thunderstorm, by day or by
  night, depending on its mode; Shift+right-click with the Wrench steps through them, and the
  Goggles show the mode and the output. Output as the Entity Detector's: iron blocks around it turn
  into redstone blocks. Read once a second on the same clock as the Light Sensor. Crafted from
  three glass, two redstone, a lightning rod and three copper ingots.

### Changed

- **Anyone can change a Message Block's text.** It was locked to admins in v6.0.1 because rich
  chat text can carry a clickable command. The text a player types is always shown exactly as
  typed, quotes and braces included, so it can never become a command. Rich text (colours,
  links) is something only an operator can write, with the Creative Data Handler or `/data`, and
  the Clipboard no longer pastes it for players without server-settings access. The selector
  still follows *Players may edit entity selectors and tags*.
- **The Clipboard checks text settings like the Data Handler.** For players without
  server-settings access, a copied text value that its block would refuse from the Data Handler
  (a selector with a command after it, a tag with spaces) is left out of the paste; the rest of
  the paste goes through.
- **Admins go past range limits.** Players with server-settings access can set a range or radius
  above the players' maximum, up to a higher ceiling that protects the server: Big Torch radius
  up to 256 (players 100), Magic Crate radius up to 64 (players 20), Message Block range up to
  1024 (players 128), in the Data Handler and on the server settings' placement defaults. The
  block keeps the larger value whoever uses it later; only the ceiling is applied when it runs.
  Other players can still lower it within their own range, and the Clipboard pastes a number only
  within the range its holder could type.
- **Admin-only settings are marked.** A setting only an admin may change shows a gold *(admin)*
  after its name in the Data Handler, for everyone: admins see which changes are theirs alone,
  other players see why the row is locked. A value above the players' maximum is marked the same
  way, and the Wrench's cycle menu marks admin-only rows for admins. The Wrench now also offers an
  admin-only setting to everyone when its server setting opens it to players, like the Data
  Handler.

### Removed

- **The Upgrade Platform.** The netherite-tier multiblock that gambled dropped materials into the
  next tier (10% success, otherwise destroyed) is gone, with its recipes and its two example
  advancements (*Upgrade Platform Base* and *Example Progression*). Everything it made is a
  vanilla item with its normal source, so nothing becomes unobtainable. In an existing world,
  assembled platforms are taken apart on the first load (one in a chunk that is not loaded comes
  apart when the chunk loads); the Netherite Base and the smooth stone stay where they were, and
  the base can be broken and picked up as before. The Netherite Base itself stays, as a tier with
  no machine yet.

### Fixed

- **A Message Block's range reaches as far as it says.** A block without a selector sent to the
  players within its `range`, but the range was rounded to 8, 16, 32 or 64 blocks, so a range of
  10 reached 16 and a range of 128 stopped at 64. It now reaches the exact range.
- **A poured potion shows its own colour when the pour empties the network.** A powered Liquid
  Drain pours a potion out as a cloud in the potion's colour, but the colour was read after the
  potion had been taken from the network, so the pour that emptied it could come out in the
  default water-bottle blue. The colour is now read first.

### For contributors

- **A core sanitiser for player input** (`ra_lib:safe/clean`, `ra_lib:safe/number`,
  `ra/lib/safe/SafeClean.kt`). `clean` turns a value into one that is safe to paste in a given
  text format: free text loses its double quotes, backslashes and line breaks, so it can sit inside
  `{text:"$(value)"}` without closing it; an id, name or tag loses the characters its format does
  not accept; a selector or coordinates are kept only when valid. `number` says whether a value is
  a number and hands it back as an int. The Data Handler's number test now lives there, and the
  Goggles' Teleport Anchor and Fluid Vault lines pass their value through `clean` before pasting
  it. See the developer guide, *safe: player input in commands*.
- **Admin limits in one place** (`ra/core/Admin.kt`). `Admin.TAG` is the `ra.admin` tag every
  admin check uses. A number declares `adminMax` next to `max` (generated as `admin_max`);
  `raiseMaxForAdmin` lifts an edit-time bound for an admin, `overPlayerMax` tells a menu to mark
  a value, and `readCappedProperty` reads a property at use time clamped only to `adminMax`. See
  *Admin limits* in Tools and registries.

## [v6.0.2] - 2026-11-09 - Drains, lists and fixes

### Fixed

- **The Data Handler shows every setting a block has, even one it was placed without.** A block
  placed before a setting existed (an Item Pipe from before *invert*, a Tag Adder from before its
  tag list) did not carry it, and the Data Handler only listed what the block carried, so the
  setting could not be found or changed. Every setting of the block is now listed: one with a
  known starting value shows it, marked *(default)* (an Item Pipe's *invert* off, a Tag Adder's
  tags `["custom_tag"]`), and any other is marked *(not set)*. The first change writes it to the
  block, starting from that default: [Add] on a default list keeps its entry and adds yours.

- **The Redstone Remote's channel prompt no longer cancels itself.** Shift+RMB with the remote
  hands out the Input Form, and the prompt was being called off the moment it opened, with
  "Input cancelled." in chat and the book taken straight back. The game leaves a short-lived
  copy of any item it gives a player floating at their feet for the pickup animation, and that
  copy was being mistaken for the form being thrown away. It is now told apart from a real
  drop, so the form stays in hand until you write on it, drop it or put it away. The same fix
  covers any other text prompt opened by using an item.

- **A Liquid Drain beside a pool drains it.** A drain put down next to a pool stands on the
  bank, one block above the water, and only looked at the six blocks touching it — so it still
  said `no_source` and took nothing in, upright or not. It now also looks one block down and out
  from each side, which is where the pool is.
- **The drain says why it is idle.** Besides `no_source`, the Goggles now show
  `waiting_for_source` when only flowing water or lava is beside the drain (a small pool
  refilling the gap it just took from), and `no_network` when the drain is not part of a network,
  instead of `network_too_small`. `network_full` still means the network has no room for a whole
  5000 mL block.
- **The tools see past redstone again.** Since v6.0.1 the Data Handler, Clipboard, Wrench and
  Multimeter stopped at the first block in the way that was not air, water or grass, so aiming at
  a custom block over redstone dust, a repeater, a comparator, a torch, a lever, a rail, a carpet,
  a sign, a ladder, a flower and the like said "No custom block found". These small blocks are now
  looked past, as is any block you are standing in (an open door), and only a block that fills
  its space stops the look.
- **The Tag Remover's "clear all" leaves the pack's own tags alone.** With an empty `tag`, it
  used to wipe every tag off every matching entity except custom-block markers, including the
  pack's own skins, displays and multiblock markers (which then stopped working) and the `ra.`
  tags on players. It now skips the pack's entities entirely, and on everything else removes
  every tag except the `ra.` ones.
- **The Wrench's [CYCLE] button changes the right block.** With one custom block on top of
  another, clicking [CYCLE] in the menu of the upper one could change the setting of the block
  below it instead.

- **Ender vaults find their partner past a vault on another channel.** A vault paired with the
  nearest receiving vault first and only then checked its channel, so when the closest one was
  on a different channel nothing moved at all, even with a matching vault further away. Item,
  Fluid and Power Vaults now pick the nearest vault that is on their channel.
- **Valves, the Liquid Filter and the EU Breaker ignore networks of another kind.** A bridge
  counted any neighbouring network as a side to level. An Item Pipe next to a Liquid Valve was
  picked as the empty side and left the valve stuck on `blocked`, and an EU Breaker beside a
  fluid pipe tried to push EU into it. Valves and the filter now only work between fluid
  networks (liquid or gas), and the breaker only between electric grids.
- **Big worlds finish settling their networks after a load.** Every loaded network is rebuilt
  once when the world loads or `/reload` runs, and in a world with well over a thousand loaded
  pipes, tanks and wires that single rebuild ran past the game's command limit and stopped half
  way, leaving some blocks out of any network until something else changed. The load now
  rebuilds a few hundred blocks every 5 ticks until all are done, and networks keep working with
  their contents while they wait. A rebuild that does get cut short is picked up by the next one.

### Added

- **Tag Adder and Tag Remover take a list of tags.** The new `tags` property holds up to 16
  tags, edited one entry at a time in the Data Handler ([Edit], [Remove], [Add]). A Tag Remover
  removes every listed tag from the entities its selector matches and leaves their other tags
  alone; with an empty list it clears all of their tags except the `ra.` ones, as an empty `tag`
  did. A Tag Adder adds every listed tag. Like the selector, the list can only be changed by
  admins unless *Players may edit entity selectors and tags* is on. Every tag is checked again
  each time the block fires: one that is not allowed (an `ra.` tag, a space) is skipped while the
  others still apply, and the Goggles show **Refused tags: N** until it is fixed. A list with only
  refused tags does nothing; it is never taken as "clear them all".
- **The Tag Adder's and Tag Remover's Goggles show their selector and tags** (**Targets**, and
  the first three tags then "+N more").

- **A powered Liquid Drain pours into thin air.** Nothing has to be there already: it pours out
  of its front, the side it faces, like a dispenser, into air or anything a bucket would
  wash away (grass, flowers, a snow layer). Water, lava and powder snow come out as one source
  block for every 5000 mL, the same amount a drain takes in for one. Experience comes out as orbs,
  potions as a cloud in the potion's own colour, and milk, steam, smoke and oxygen are poured away
  as white drops, a cloud, smoke or blue mist. In the Nether water evaporates, as from a bucket:
  each block's worth hisses away and the Goggles say `evaporates`. The Goggles say `pouring`, or why not: `blocked`,
  `not_enough` (less than a whole block of that fluid), `network_empty`.

- **Show filter previews** (your own settings, `/trigger ra.settings.open`, on by default):
  whether the Goggles draw the item an Item Pipe filters for above its label. A Goggles label is
  one entity that everyone nearby sees, so it cannot be hidden from one player: a pipe draws its
  filter item while any Goggles wearer within range of it has the setting on. Turn yours off and
  you stop asking for previews, though you still see one drawn for another wearer beside you.
  Hover the setting's name in the menu for this explanation. The server-wide *Filter previews*
  switch (General › Goggles) still turns them off for everyone.


- **"Show the load message" now silences the whole load message.** With it off, a `/reload` no
  longer prints each module's "loaded!" line (Interactive blocks, Storage blocks, Sensors, Logic
  Gates, Wireless Redstone, Wires, Chunk Loader, Enchant crafting, Infinite generators, Jetpacks,
  Ender links) nor the version and settings lines. The notice that some block types are disabled
  still appears, since it is a warning rather than a greeting.
- On/off server settings read **on** / **off** in the settings pages and in the confirmation
  message, instead of `1b` / `0b`.

### Changed

- **List rows read [Add] [Edit all] [List], and start compact.** A list in the Data Handler is
  one line now, its header: [Add] types one new entry for the end, [Edit all] (was "[Edit whole
  list]") writes the whole list at once in the input book, and [List] shows the entries
  underneath, each with its own buttons; click it again, or another list's, to fold it away. A
  list you may not change keeps its locked row, and its [List] shows the entries without buttons.

- **Tag Adders and Tag Removers placed from now on start with `tags: ["custom_tag"]`**, a list
  of one tag, instead of `tag: "custom_tag"`. Blocks you already placed are converted when the
  world loads: their tag becomes the list's one entry (`tag: "red_team"` becomes
  `tags: ["red_team"]`), a Tag Remover without a tag, which cleared every tag, gets the empty
  list and keeps doing exactly that, and a Tag Adder without one gets the empty
  list and keeps adding nothing. The old
  `tag` setting is gone.
- **The Data Handler's list button is [Add]** (it was [Append]). It adds an entry at the end,
  also to an empty list, and asks for it in the input book, checked like any other entry. Once
  the list holds as many entries as it may (16 for a Tag Adder's or Tag Remover's tags), it is
  greyed out, and hovering it says "Maximum 16 entries". The list's header shows how full it is,
  like `tags: list (1/16)`.

- **Liquid Drain `speed` is now `rate`, in mL per second** (5000 by default, one block a second;
  100 to 100000), and it sets how fast a drain takes in and pours out alike. Existing drains and
  the server default are converted when the world loads: a speed of 5 becomes a rate of 5000.
- **Liquid Drains are silent.** They no longer play a bucket sound each time they take in, pour
  out or empty a container.

## [v6.0.1] - 2026-11-08 - Leaner ticks

**Supported version:** 26.3 (data pack format 121).

A performance release. The pack does the same things with far fewer commands per tick, by
moving its arithmetic onto 26.3's `/compute` and by checking multiblock structures with
predicates built when the pack is generated. Worlds on v6.0.0 carry on without a migration.

### Added

- **Lists are edited one entry at a time.** In the Data Handler a list shows each entry on its
  own line with **[Edit]** (type a new value for that entry) and **[Remove]**, and the list
  itself has **[Append]** (greyed out once the list is at its maximum length) next to
  **[List]**. On a list whose positions mean something, such as a Teleport Anchor's
  fifteen strengths, **[Remove]** is **[Clear]**: the slot is emptied and the others stay where
  they are. Every entry is checked against the list's type before it is written.
- **Pick a Teleport Anchor's destination by name.** Every anchor remembers its name in a shared
  list, and the Data Handler shows one line per redstone strength — *Strength 3: Home* — with
  **[Choose]**, which lists the anchors already named in the world: click one and that strength
  leads there. No list typing needed. `/function ra_ender:tools/anchor/list` prints the names,
  and `…/anchor/show` now prints one line per strength that leads somewhere.
- **Players may edit entity selectors and tags** (server setting, Sensors page, off by
  default): lets players without server-settings access change the selectors and tags of the
  Entity Detector, Tag Adder, Tag Remover and Message Block.
- **Inverted Item Pipe filters.** A pipe's new `invert` setting (off by default) sends every
  item that does **not** match the filter to the side and lets the filtered item carry on; the
  Goggles show *Mode: Inverted*.
- **Settings subcategories.** A server settings page can have subcategories, listed at the bottom
  of the page. The Goggles settings moved into **General › Goggles**, which also has the new
  *Filter previews* switch: turn it off and the Goggles stop drawing an Item Pipe's filter item
  above its readout. Values already set carry over.
- **Purge entities** (server settings, **[Purge entities]**): remove Redstone Additions'
  entities from a world while keeping the pack, its settings and scoreboards. **Everything**
  turns every placed RA block into a plain block and clears transport networks, the Teleport
  Anchor name list and pending Data Handler edits; narrower purges remove only **orphaned
  displays** (skins and pipe or wire displays whose block is gone), **goggles readouts**, or
  **dropped RA items**. Each one first shows what it would remove and how many are loaded, and
  its **[CONFIRM]** works only for the admin who asked, for 30 seconds. A purge reaches loaded
  chunks only; after **Everything**, RA blocks from before the purge are removed as the rest of
  the world loads, until you stop that on the Purge page. Other server-settings holders are told
  who purged what.

### Changed

- **Only admins can change a Message Block's text.** The text is shown as rich chat text, so it
  can carry a clickable command that runs as whoever clicks it. Players without server-settings
  access now see it locked in the Data Handler.

- **Worlds are upgraded once, not on every load.** The world now remembers which version of the
  pack it was last loaded with, and the one-time fixes for older worlds run only when that
  version is older than the fix, instead of on every load. Each custom block also remembers its
  version: a block from an older version is brought up to date once, the first time its chunk is
  loaded, rather than by checks that several blocks (the Liquid Drain, Poppy Generator, Clock,
  Boxer, Unboxer, Valves and Filter, Teleport Anchor) ran on every tick. Blocks in chunks that
  were not loaded when the pack updated are now fixed too.
- **The Liquid Drain pours out on redstone, and its speed is a server setting.** The drain's
  `mode` property is gone: an unpowered drain takes in, a drain powered by redstone pours its
  network's contents back out (a source block on a free side, experience orbs, or a potion over
  nearby players), so you can switch it from a lever. The Wrench no longer cycles the drain.
  Its `cooldown` is replaced by `speed`, in litres per second (1 – 100, default 5: one bucket's
  worth a second, the old default rate), which only operators can change — with the Data
  Handler, or for new drains in the Server Settings. Existing drains keep their rate (a drain
  set to 10 ticks becomes speed 10, one set to 40 ticks speed 2); one that was set to `place`
  now needs a redstone signal to pour.
- **Wireless Emitters, Receivers and multiblocks no longer have a hidden on/off switch.**
  Right-clicking one with the Wrench used to flip an `enabled` property that nothing on the block
  showed, so an Emitter, a Receiver or a machine could sit switched off with no sign of why. That
  property is gone: an Emitter is controlled by its redstone input and its channel, a Receiver by
  its channel, and a Blast Forge, Rock Metallic Drill or Upgrade Platform by its redstone control.
  Any of them that had been switched off works again. Right-clicking an Emitter or Receiver with
  the Wrench now says the block cannot be toggled; on an assembled multiblock it says it is
  already assembled, and the Blast Forge's Goggles readout no longer has an "Enabled" line.
  Multiblocks also no longer carry an unused `speed` property.
- **The Thruster Kit costs a quarter of what it did.** Its push is worked out with
  `/compute` instead of a chain of scoreboard operations, and only while you sprint:
  52 → 14 commands per tick for each airborne player using it. Jetpack hover holding and other
  per-player steps shed a few commands each.
- **Pipes, wires and Ender vaults do their sums in one command.** Adding to or taking from a
  network, clamping it to capacity after a rebuild, and the bridge move between two networks
  are each a single `/compute` now. A bridge transfer is 27 → 17 commands; a network offer or
  take writes in 4 – 6 instead of 8 – 10; Experience and Potion drains, the Power and Fluid
  Vault level displays and Teleport Anchors each run 3 – 8 fewer commands. The Big Torch sweep, the
  Magic Crate, Goggles stacking and the settings menu get the same treatment.
- **Multiblocks are checked by predicate.** A structure's blocks are known when the pack is
  built, so each facing of the Blast Forge, the Rock Metallic Drill and the Upgrade Platform is
  now one generated predicate instead of a walk over rotated coordinate tables. A wrench
  assembly is about 37 commands where it was about 130, a periodic structure check 3 – 12
  where it was 36 – 49, and loading the pack no longer rebuilds every structure (about 1,200
  commands each, now 2).
- **The Blast Forge runs without macros.** Its smelting, fuel and smoke steps are generated
  once per facing, so a working forge no longer re-instantiates functions every tick.
- **One debug report for every multiblock.** With the `ra.debug` tag,
  `ra_lib_multiblock:spec/<id>/debug` lists each missing or wrong block and a `found/total`
  count; a structure that falls apart while a debug player is online reports itself. It
  replaces the Blast Forge's own debug function.
- **Old saved data is tidied.** The Blast Forge's `bf_dir` coordinate tables are removed from
  saved worlds on load, and drill markers drop their outdated `registry_driven` flag on their
  first check.
- **Custom blocks are no longer re-centred every tick.** Every custom block in the world was
  moved back onto its block centre on every tick, though nothing ever moves them. They are
  placed at the centre, and one an older version left off-centre is straightened out once.

- **Every block declares its settings.** Each kind of block now lists its settings, the type
  of each (number with its range, on/off, text, list, item) and whether it is read-only,
  admin-only or hidden, in one place next to the block. The Data Handler, the Wrench and the
  Clipboard all follow that list: the Data Handler shows a block's settings in the block's own
  order and always offers the editor its type calls for, whatever value happens to be stored.
- **Admin-only and hidden settings.** A setting can be kept for players with server-settings
  access: other players see it locked, with the reason when they hover it, and a value they
  typed is refused again at the moment it would be written. A hidden setting does not appear
  for them at all; admins see it marked *(hidden from players)*. The Wrench and the Clipboard
  skip what you may not change.
- **Undeclared values are guessed, and flagged.** A value no block declares (added by an
  add-on or with the Creative Data Handler) gets a row when its type can be told from the
  value, marked with a yellow ⚠ that names the guess; players with the `ra.debug` tag get a
  line naming the block and the setting. Anything else is listed under *Undeclared*.
- **Typed values are converted and checked.** A number typed in the book must read as a
  number, an on/off setting accepts `true`/`false`, `on`/`off` or `1`/`0`, and a number prompt
  shows and enforces the setting's range. Text is kept exactly as typed, even when it looks like
  a number. A refused value leaves the block unchanged and says why.
- **Old menus no longer edit.** A Data Handler button in a menu other than the latest one you
  were shown, or one drawn before a `/reload`, redraws the menu instead of changing a setting.
- **The Creative Data Handler lists what the block actually has.** It showed a fixed list of 18
  names, several of which no block uses, and none of the Wires, Ender, storage or multiblock
  settings. It now shows the block's declared settings, each with its type, then any
  undeclared ones, flagged.
- **Goggles labels sit just above the block and fit their content.** A label is now one
  display that starts a little above the block's top face and grows upward with the number of
  lines, instead of a stack of separate lines placed at fixed heights well above it: a block
  with one line no longer floats as high as one with five, and no line can end up inside its
  block. The name and lines share one size, and an Item Pipe's filter item floats just above
  its label. Each label is one entity instead of one per line, so a redraw summons fewer and
  costs fewer commands.
- **The Goggles hide what the Data Handler hides.** A setting hidden from players is no longer
  shown on its block's Goggles label either, and a few fixed or uninteresting values are left
  off the label while staying in the Data Handler: the Mineral, Nether and Poppy Generators'
  cooldown and the Magic Crate's pulse cooldown.

- **Entity selectors and tags are for admins.** The `entity_selector` and `tag` of the Entity
  Detector, Tag Adder, Tag Remover and Message Block can only be changed by players with
  server-settings access, unless the new *Players may edit entity selectors and tags* setting is
  on. A selector reaches any entity in the world (`@a`, `@e`), and a Tag Adder can hand any tag to
  anyone.
- **Text settings say what they accept.** A text setting only takes the characters it can use:
  a name or channel letters, digits, spaces and `_ . + - : / #`; a tag no spaces and never a
  name starting with `ra.`; a selector one `@a`/`@e`/`@n`/`@p`/`@r`/`@s` with an optional
  `[...]`; a Boxer or Unboxer input/output three relative coordinates within 9 blocks
  (`^ ^ ^-1`). Anything else is refused with a message saying what is allowed. The Message
  Block's text still takes anything.
- **Number prompts show their range.** Every number you are asked for — Data Handler settings,
  your own and the server's settings — reads *Enter a number for delay (1–1200)*, or *(≥ 1)* /
  *(≤ 64)* when only one bound exists, and a value out of range is refused with the same range.
- **The Teleport Anchor's help** starts with the Data Handler, and lists the operator commands
  after.
- **The EU Consumer's settings row sets its draw.** The server settings row for the EU Consumer
  was a "Consumer interval" (`cooldown`) the block never reads; it is now *Consumer EU draw*
  (`eu_use`, default 40), so **Apply to placed** writes a setting the block uses.

### Fixed

- **An Item Pipe filter can be removed again.** Item settings in the Data Handler have a
  **[Clear]** button; until now nothing could empty `filter_item` once it was set.
- **The Data Handler edited the wrong block.** Picking the block under the crosshair took the
  nearest block within a block and a half of four points along your look, so a block beside or
  below you often won over the one you were looking at, and a block behind a wall could be
  picked. The Data Handler, Creative Data Handler, Wrench, Multimeter and Clipboard now follow
  your look block by block and take the first block it enters, stopping at walls.

- **No more lag spikes from broken Magic Crates.** When the barrel under a Magic Crate was
  replaced by another container, the crate tried to "spill its contents" by summoning empty item
  entities in a loop that never finished: thousands of "Tried to add entity minecraft:item but it
  was marked as removed already" warnings per tick, the command limit hit every tick, and the
  server falling minutes behind. The Block Placer, Item Pipe and Spitter had the same spill on a
  smaller scale, and could overwrite the block's own dropped item with the container's first
  stack. A broken barrel or dispenser already drops its own contents, so the spill is gone: these
  blocks now only give back their own item. The pack also refuses to build if an item entity is
  ever summoned empty again.

- **Server settings defaults match the blocks.** The Poppy Generator's interval row said 100 where
  the block uses 80, and the Magic Crate radius row allowed up to 32 where the crate stops at 20.
  The Block Breaker and Block Placer intervals now have server-default rows too. The Poppy
  Generator's tooltip no longer mentions the retired patch mode.

- **The Liquid Drain drains.** A drain points where you look when you place it, so one put
  down beside a pool usually ended up facing up — and an upright drain only ever took buckets,
  bottles and experience, never the water or lava next to it, whatever its mode said. An
  upright drain still loads containers and experience first, and with nothing to load it now
  takes in the source block beside it like any other drain. Water and lava fall onto the drain
  as it takes them in (other fluids show their own particle), for players with block particles
  on, with the sound of a bucket filling; its "no source" smoke also respects that setting.

- **Pumps say when a source is the wrong kind.** A Gas Pump next to water, or a Liquid Pump
  next to a gas, skipped it without a word and kept showing its last status; the Goggles now
  read `not_a_gas` or `not_a_liquid`.

- **The EU Consumer's draw is a setting again.** Its Wrench row was called "Rate" and showed a
  `cooldown` value the Consumer never uses, while its [ CYCLE ] button changed the draw, which
  the Data Handler showed locked as a fixed value. The row is now "Draw" and shows the 20 / 40 /
  80 EU per tick it cycles, and the Data Handler lets you set the draw directly.

- **Tanks and batteries out of view keep what they hold.** Placing or breaking any pipe or wire
  recounted every network in the world from the blocks that were loaded at the time. A network
  out of view lost its contents for good, and came back showing 0 / 0, with its pumps and
  generators unable to add anything; one only partly in view was recounted from the loaded part
  and cut down to that part's capacity. Placing a block now only recounts the networks it
  touches, and a network that reaches into unloaded chunks is never recounted or trimmed: a new
  pipe simply joins it, and a break that splits it takes effect once the whole network is loaded
  again. Networks already emptied this way work again as soon as one of their blocks is placed,
  broken or read by a machine or the Goggles.

- **Machines stack their output again.** The Blast Forge, the Rock Metallic Drill and
  Storage Box deliveries put each delivery into a new slot. A chest holding three single
  cobblestone that received one more ended up with four stacks of one instead of a stack of
  two, and filled up long before it was full. Deliveries now top up matching partial stacks
  first (same item, same name and enchantments), up to the item's own stack size, then use
  free slots, and only drop what really does not fit. Custom items such as the drill's Rocks
  stack too: the machine's item is compared in the form the chest actually stores it in, so a
  Rock no longer looks different from the Rocks already there. The Item Mover also sees room
  on partial stacks now: a container with every slot taken used to count as full even when its
  stacks could still take more.

- **Personal settings toggle again.** Clicking a toggle on the *You* page (machine sounds,
  particles, debug messages) did nothing, and the menu kept showing the old value. The click
  handler asked every row for a range and a step that only number rows have, so the game
  refused the whole call for an on/off row. Each kind of row now has its own handler. Server
  settings were not affected.

- **Bridges on large networks.** A Valve, Filter, Breaker, Boiler or Ender Power Vault judges
  which neighbouring network is fuller by its fill ratio. On a network holding more than about
  214,748 mL that ratio wrapped round to a nonsense value, so a big tank farm could look empty
  and be filled from, or drained into, the wrong side. The ratio is now computed in floating
  point and is right at any size.

- **Text prompts give you time to write.** Typing a channel, a name or a setting value in the
  Input Form ran out 30 seconds after the prompt opened, however busy you were writing, and
  the text was thrown away with "Input request expired." A text prompt now lasts 5 minutes,
  and the clock starts again whenever you hold the form in either hand, so it only expires
  once you have really left it alone.

- **One Input Form per prompt.** The prompt looked for its form only in your main inventory.
  Swapping it to your offhand, putting it in a chest, bundle or lectern, or carrying it on the
  cursor handed you a second form, text written in the offhand was never picked up, and a form
  left in a chest stayed behind as a free Book and Quill. The form now works from the offhand
  too, is never handed out twice, and putting it away cancels the prompt just like dropping it
  does. A leftover form is taken back as soon as someone picks it up outside a prompt.

- **Dropping your form cancels even next to someone else's prompt.** A player finishing a text
  prompt removed every dropped Input Form within 8 blocks, including one another player had
  just dropped to cancel theirs, whose prompt then stayed open. Each prompt now removes only its
  own form.

- **`ra.input.trigger` leaves your `/trigger` list.** After your first number prompt the input
  trigger stayed in your `/trigger` suggestions for good. It is now offered only while a number
  prompt is waiting for you, and removed from everyone online on the next load.

- **Typed settings edits are kept per player.** The value you were typing for a setting was
  remembered in one place for the whole server: two people editing at once overwrote each
  other, and a player's typed number could end up in a server setting an operator had just
  asked to change. Each player's edit is now kept on its own, and is cleared once it is
  applied, cancelled or expires. A server setting is only changed if the player typing it still
  has server-settings access when the value arrives; running a setting's `edit` function now
  grants that access, as opening the server settings already did.

- **Tools no longer take each other's typed answers.** The Data Handler, the Redstone Remote
  and the settings menu all ask for input through the same book and number form. The Data
  Handler and the Remote took whichever answer came in, so starting one while another was
  waiting could hand your channel name to a block property, or the other way round. Each now
  only accepts the answer to its own question, and stops waiting when another tool's question
  replaced it.

- **The Data Handler works for several players at once.** Selecting a block used to deselect
  every other player's, and the property being edited was shared too: player A's value could
  be written onto the block player B had just selected, under B's property, even one B's block
  does not allow changing. Each player now has their own selected block and their own pending
  edit. The value is written to the block it was asked about, it is refused if you selected a
  different block since, and the read-only check runs again just before the write.

- **[List] checks what you typed.** Anything that parsed was stored as is, so typing
  `{a:1}` into a Teleport Anchor's targets replaced its table with something the anchor could
  not read. The text must now be a list, every entry must be text when the property holds text,
  and a Teleport Anchor's targets take at most 15 entries, padded with empty rows as
  `ra_ender:tools/anchor/set_table` does. Anything else is refused with a message and leaves the
  block unchanged.

- **The Ender Power Vault never loses EU over a broken channel.** A vault whose channel was
  missing, or held text the transfer could not use, still took its share of EU from the grid
  every cycle and then failed to send it. What it gave back was worked out from the previous
  vault's transfer, so the grid could lose EU, or be handed back the wrong amount. A vault now
  checks its channel before taking anything, and a send that fails puts everything back.

- **Tag Adders, Tag Removers and Entity Detectors use their own settings.** Several Tag Adders
  or Tag Removers switched on in the same tick all used the tag and target of the last one, and
  a block missing its tag or target borrowed the one before it. Each block now reads only its
  own settings. A Tag Adder or Entity Detector with no target, or a Tag Adder with no tag, does
  nothing (the detector's output stays off); a Tag Remover with no tag still clears every tag,
  as before.

- **Repeaters, comparators and observers drive gates again.** A repeater pointing into a logic
  gate or any other redstone-reading block was not seen at all, while one pointing away from it
  powered it at full strength. The pack read these three backwards: their `facing` names the
  input side (for a repeater or comparator) or the watched side (for an observer), and they
  output from the opposite face. A repeater, comparator or observer now powers only the block
  its output faces, from every side including above and below, and one pointing away no longer
  counts as an input on the AND, NAND and other gates that check which sides are wired.

- **Custom blocks place on flowers, rails and torches.** Using a custom block on a flower,
  sapling, rail, pressure plate, button, lever, torch or redstone dust did nothing and the item
  was gone. The block now goes against the face you clicked, as a vanilla block would.

- **A refused placement gives the item back and says why.** When a custom block could not be
  placed (another custom block already there, no room, a player in the way, or the block
  disabled on the server), the item was used up anyway, and only the disabled case said
  anything. You now get the item back in survival and adventure, with a message saying why.
  An item fired from a dispenser is dropped in front of it.
  Older pipes and wires still sitting in chests (L2 pipes and wires, gas pipes) are covered too:
  they give back the Copper Pipe or Wire they now place.

- **Breaking a custom block no longer deletes nearby items.** Removing the plain vanilla item a
  broken custom block drops took the nearest item of the same kind, whatever it was: a stack of
  64 quartz you had dropped beside a Shortener, or the contents a container had just spilled.
  Only the block's own fresh drop is removed now, for every custom block: pipes, wires, tanks,
  pumps, the electric blocks and Multiblock Bases included.

- **The pack no longer stops after a very heavy tick.** If one tick ran past the game's command
  limit, the pack's main loop never scheduled the next one and every machine stopped until
  `/reload`. The loop now schedules its next tick first.

- **Uninstall removes everything.** It left powered Chunk Loaders' chunks force-loaded for good,
  left the Clock and UNI Gate's floating items and the Goggles' item lines in the world, kept
  more than thirty scoreboards (networks, jetpacks, Ender links, enchanting, tool timers) and did not
  actually empty any of the pack's saved data. The lists of what to remove are now read from the
  pack itself, so nothing the pack creates is left behind.

- **Number settings get the number editor.** The Data Handler decided a setting was text
  whenever the game would let it store the value as text, which it does for numbers too, so
  every number setting it did not know by name was edited as text and saved back as text. Types
  are now declared per block, and a value without a declaration is typed by tests only that
  type passes.

- **The Creative Data Handler works for several operators at once.** One shared marker tag
  decided which block it edited, so a second operator's Shift+RMB moved it, and the first
  operator's [Modify], [-] and [Refresh] then changed the other block. Its menu was also shown
  to the player nearest the block rather than the one who clicked. Each operator now has their
  own selected block (the same one the Data Handler uses), and the `/data` commands its buttons
  suggest name that block by number, so they still reach it when typed later.

- **Teleport Anchor targets show in the Data Handler.** The [List] row's hover text broke
  the chat message, so no list setting was ever shown and the anchor's table could not be
  edited from the menu.

- **Multiblock base tiers are locked.** The read-only rule for a base's `tier` and `tier_level`
  was registered under a block type no base carries, so the Data Handler let players change a
  base's tier.

- **The UNI Gate's wrench menu shows its gate type.** The row read a property the gate does not
  have and showed an empty value.

- **The Data Handler names the block you selected.** It read the name from a field markers do
  not carry, so after a refresh it kept whatever name the last player to select anything had
  left behind, and a block with no settings showed the previous block's.

- **Block settings could run any command.** Several blocks pasted a setting straight into a
  command: a Tag Adder whose selector was `@a[limit=1] if function <any function> run say` ran
  that function for whoever placed it, with the server's permission; a Boxer input of
  `~ ~ ~ if function …` did the same; a vault channel or a Teleport Anchor target containing
  `"}` broke out of its quotes. Any player with a Data Handler could set them. Each such setting
  is now checked against what it may contain before the block uses it, every time, whoever
  wrote it — and a Tag Adder can no longer hand out the pack's own `ra.` tags (`ra.admin` gave
  server-settings access). A value that fails the check makes the block do nothing until it is
  corrected.
- **Teleport Anchors no longer put the target name in a command.** The destination is found by
  comparing names directly, so any name works the same way.
- **Damaged anchor tables are repaired.** A target table that was not a list of fifteen (after
  an old Data Handler edit wrote something else) is padded, trimmed or emptied the first time
  the anchor runs.

- **Item Movers, Boxers and Unboxers no longer duplicate items from a double chest.** When both
  halves of a double chest held items, the machine took the first stack of the half it faces but
  then removed the items from the other half: the stack it moved was copied, and the other
  half's first stack disappeared. It now removes items from the half it actually took them from.

- **Item Pipe filters use all four sides.** On a pipe pointing up or down, filtered items were
  only ever sent into containers at two of its sides, and could be pushed back into the container
  feeding the pipe. A filtering pipe now tries the four containers around it, whichever way it
  points, and never the one behind it.

- **The Gas Pump has something to pump.** No world block counted as a gas, so a Gas Pump could
  never pull anything. It now draws Smoke from a lit campfire or soul campfire next to it,
  500 mL a second, and leaves the campfire burning. Pumps and drains also look only for sources
  they can take: a Liquid Pump or Liquid Drain beside both a campfire and water takes the water,
  and a drain never pulls smoke into a liquid network.

- **Generator speed is an admin setting.** A generator's `cooldown` was shown locked to everyone.
  Players with server-settings access can now change it per generator with the Data Handler;
  for everyone else it stays locked. A Poppy Generator that had lost its cooldown value fell back
  to 100 ticks instead of its own 80.

- **Block Breaker and Block Placer speed is an admin setting.** Both showed a locked `cooldown`
  that did nothing: they always acted once a second. The `cooldown` (in ticks, 20 by default) is
  now what they wait between two actions, and players with server-settings access can change it
  per block with the Data Handler.

- **The Block Placer no longer eats items it cannot place.** With an item that is not a block
  first in its inventory, it placed nothing but still used one up every second. It now uses an
  item only when it placed a block, and only then plays its sound. It also places into cave air,
  which it used to treat as occupied.

- **The Magic Crate's radius stops at 20.** The Data Handler accepted up to 32, but the crate
  never reached past 20; it now refuses values above what the crate uses. Its pull interval
  (`cooldown`) is now a server admin's setting.

- **The Breeder feeds from its own inventory.** It looked for food in the container behind it
  (except for horses and llamas), and only ran when its own inventory had something in it, so a
  Breeder stocked with food bred nothing. It now takes the food from its own inventory for every
  animal, and looks for the animals in front of it.

- **Machine sounds and particles follow your settings everywhere.** With *Machine sounds* or
  *Machine particles* turned off you still got some of them: the break sound and puff of almost
  every block, the Block Breaker's dust, the Magic Crate's portal swirl, the Mineral and Nether
  Generators, the Randomizer, the Blast Forge's smoke and flames, the Upgrade Platform, a
  multiblock falling apart, the Message Block's chime, Ender Links, the Ender Power Vault and the
  Teleport Anchor's portal cloud. They now all respect both settings.

- **The Spitter shows when it throws.** A small puff now comes out of its face with each stack,
  for players with *Machine particles* on.

### For contributors

- Migrations are versioned. `storage ra:migrations version` holds the world's version and the
  `ra.ver` score (plus tag `ra.ver.<version>`) each custom block marker's; a step is declared with
  `migration("6.0.0", "6.0.1") { world { ... }; entity("<type>") { ... } }` in `ra/migrations`
  and runs only where the recorded version is older. Block code no longer carries one-time fixes
  (`ra_wires:fluid/drain_migrate`, the `ra.wires.drain_v2`, `ra.ender.anchor_checked` and
  `ra.storage.io_default_migrated` tags are gone). See [Migrations](https://ancarsenat.github.io/Redstone-Additions/architecture/#migrations).
- A property can set `goggles = true` to show on the Goggles label even when `visible = false`
  hides it from the Data Handler, or `goggles = false` to keep a visible one off the label;
  unset, it follows `visible`. Generated as a resolved `goggles` byte in
  `storage ra:dh types."<type>".<name>` (not for `dynamic` properties, which read
  `data.property_types.<name>`). Goggles line writers now append to one `text_display` per
  billboard, in call order: their `y` arguments and `stack_reset` are accepted and ignored.
- Block properties are declared per block type with `properties(block) { number(...); bool(...);
  text(...); list(...); item(...) }` (`ra/core/Properties.kt`), replacing `ra.readonly(...)`,
  `ra.dataHandlerProperties(...)` and the Data Handler's global name lists. They are generated
  into `storage ra:dh types."<type>"` and `order."<type>"`; the build warns about property names
  a block uses without declaring and about every `dynamic(...)` declaration. See
  [Property schemas](https://ancarsenat.github.io/Redstone-Additions/tools-and-registries/#property-schemas).
- `ra/core/Compute.kt` builds and emits `/compute` providers; the [Developer Guide](https://ancarsenat.github.io/Redstone-Additions/developer-guide/#arithmetic-with-compute)
  covers when and how to use it. The constant fake players the old chains needed (`#jp.cap`,
  `#br.two`, `#dr.mul` and others) are gone.
- Multiblocks are declared in Kotlin with `multiblockSpec { }` and `Ra.multiblock(spec)`; see
  the [Multiblock Library](https://ancarsenat.github.io/Redstone-Additions/multiblock-library/).
  Add-ons registering through `ra_lib_multiblock:register` keep working unchanged.

## [v6.0.0] - 2026-11-07 - Kore rewrite

**Supported version:** 26.3 (data pack format 121).

The pack is now written in Kotlin and generated by [Kore](https://kore.ayfri.com), from
`redstone_additions/`. The blocks, commands, function paths, scores, tags and saved data are
the same, so a 26.3 world running v5.1.17 carries on. Older Minecraft versions are no longer
supported: stay on v5.1.17 for 1.21.9 – 26.2.

### Changed

- **26.3 only.** The overlays that bridged 1.21.9 – 26.3 are gone; `pack.mcmeta` declares
  format 121 only, so other versions refuse the pack instead of loading it half-broken.
- **Built with Gradle.** `./gradlew koreBuild` replaces `beet build`. A build fails on any call
  to a function the pack does not define.
- **Enchant crafting: the first matching recipe wins.** Every recipe list in
  `#ra_enchanting:recipes` used to run, and the last one reset the chance of whatever had
  matched. Each recipe now steps aside once an earlier one matched and keeps its own odds. The
  Iron Jetpack Kit → Infinite Iron Jetpack Kit sacrifice is 1%, which is what it has rolled at
  in play; the docs no longer claim 10%.

### Fixed

- **Teleport Anchors teleport.** Since v5.1.8 the step that finds the destination had lost its
  only command, so no anchor ever moved anyone. It is back, and it now finds the nearest other
  anchor with the matching id even when anchors with other ids stand closer — before, only the
  single nearest anchor was ever checked.

### For contributors

- `./gradlew koreRun -Pworld="<world name or path>"` builds the pack and installs it into that
  world; a bare `-Pworld` uses `ra.world` from an untracked `redstone_additions/local.properties`.
  See the contributing guidelines.
- The version lives in one place: `version` in `redstone_additions/gradle.properties`.

## [v5.1.17] - 2026-11-06 - 26.3

**Supported versions:** 1.21.9 - 26.3 (data pack formats 88 - 121).

Hotfix: the pack did not load on 26.3 at all. 26.3 changed the shape of files
the pack ships everywhere, and an old shape in any one of them stops the world
from opening, so it took the whole pack down with it.

### Fixed

- **The pack loads on 26.3.** Every predicate and item modifier now names its
  type with `type` (was `condition` / `function`), and every recipe unlock
  advancement uses the `recipe_unlocked` trigger's new `recipes` field (was
  `recipe`). That is 17 predicates, 18 item modifiers and 67 advancements.
- **Inserting into containers on 26.3.** `ra_lib:inventory/insert` builds an
  inline loot table, whose `functions` list became `modifier` with typed entries.
  Every machine that pushes items into a container goes through it.
- **Block skins, wires and pipes are visible on 26.3.** Block states in NBT
  renamed `Name`/`Properties` to `id`/`properties`; a block display summoned
  with the old keys loads without an error and shows air.

### Changed

- **26.3 gets its own overlay, `overlay_121`, generated at build time.** The old
  shapes are the only ones 1.21.9 - 26.2 understand, so the base pack keeps them
  and `tools/port_26_3.py` writes a converted copy of every affected file into
  the build. New recipes, predicates and modifiers are ported automatically, and
  a shape the plugin cannot convert fails the build instead of shipping.
- `overlay_102` now runs up to format 120 so the two overlays never overlap.
  Formats 108 - 120 (the 26.3 snapshots and pre-releases) are declared but not
  supported.

## [v5.1.16] - 2026-08-20 - Mixed pipes

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
  Handler. It drops what **spawned** inside the radius into the void and leaves
  what walked in, by remembering every mob in a band 16 blocks past it — dropping
  rather than killing is what stops it being a mob farm that also lights the
  room. Built from Enchanted Coal — sacrifice coal on an enchanting table at 1%
  — nine of which make an Enchanted Coal Block, which over a stick makes the
  torch.

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

## [v5.1.15] - 2026-08-19 - Settings

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

### Added

- **A settings system, `ra_settings`.** Two scopes, reached two ways on purpose.
  Server settings are `/function ra_settings:admin/...` and nothing else, because
  `/function` autocompletes and that is the only way an operator finds a setting
  without being told its name. Player preferences are `/trigger ra.settings.open`
  and nothing else, so somebody with no permissions can change what they see and
  hear. `/function ra:settings` is the short way into the operator panel.
- **Blocks can be turned off.** Every placeable block has enable/disable. A
  disabled block cannot be placed and the item is handed back; anything already
  built keeps working, and the item can still be crafted and held.
- **A disabled-blocks page**, listing every switched-off block in red with a button
  to re-enable each, plus a warning on every load when the list is not empty —
  shown whether or not the load message is.
- **Block defaults can be retuned** — generator EU/tick, clock interval, vault
  transfer rates, crate radius and the rest. Applied when a block is placed, so a
  build balanced around the old numbers is not changed underneath it, with
  **[Apply to placed]** as the explicit opt-out.
- **Per-player sound and particle switches**, honoured by all 118 `playsound` and
  `particle` calls in the pack, plus a debug switch wired through the existing
  `ra.debug` tag.
- **Jetpacks and Enchant Crafting settings pages.** Thruster Kit thrust, speed cap
  and deadzone are tunable; enchant crafting can be switched off. The jetpack
  values are read once per tick rather than per flying player.
- Numeric and text settings can be **typed**, through the same input form the Data
  Handler opens for a clock's delay, instead of only stepped.
- The goggles redraw interval and scan range are settings.
- **`ra.admin` grants server-settings access** and persists, so a tagged player
  opens the panel straight from the button. Managed with
  `ra_settings:admin/grant` and `/revoke`.
- **Uninstall warns twice**, the second time listing exactly what is about to be
  destroyed, and is reachable from the settings index. Its own buttons keep the
  confirmation dialog on purpose. It also cleans up what the original missed, and
  the settings half is generated so it cannot drift again.
- [Settings](settings.md) documentation, covering both scopes, the trigger table
  and the JSON contract for adding a page.

### Fixed

- **Ender vaults could not find each other.** `link/send_items` looks for a partner
  wearing `ra.ender.recv_item`; `item_vault/tick` cleared that tag every tick and
  nothing anywhere set it, so a sending vault searched for a tag no vault could be
  wearing. Broken since v5.1.8, when the lines that set it were deleted along with
  the `enabled` property they mentioned. All three vault types were affected.
- **The Electric Furnace skin flickered, vanished and z-fought while working.**
  `apply_lit` killed the old `block_display` and summoned a replacement, but `kill`
  does not remove an entity until the end of the tick — so two identical displays
  overlapped for the rest of it. State changes now edit the display already there.
- **A working furnace was drawn as switched off**, one tick in five on
  superpowered. Whether it looks like it is cooking is now decided by whether it
  can cook, with the cooldown deciding only which tick an item comes out on.
- **A steam-fed EU Generator produced power while drawn permanently unlit** — it
  read `data.data.burn`, the solid-fuel countdown, which the steam path never sets.
- **Jetpack upgrade kits fired with the jetpack switched off.** The Thruster and
  Scorch ran on "wearing the kit and off the ground", so in normal mode — where the
  jetpack only runs while you sneak — jump, sprint, jump gave a forward boost for
  free. They were also called above the fuel check, so they worked on an empty tank.
- **Text input never completed.** A settings text edit opened its input session
  before `ra_lib:input/tick` in the tick order, so the book was scanned in the same
  tick it was handed over — a state the Data Handler cannot reach, which is why its
  text input worked throughout and this did not.
- **Settings could consume another tool's input session**, handing the Data Handler
  an empty answer and leaving it waiting for one already taken.
- **A failed settings read disabled the module that asked.** `ra_settings:get`
  answered 0 for a missing key, and zero is a real value — "off" for a flag,
  "disabled" for a gate. It now requires the caller to say what missing means.
- **The debug setting fought the `ra.debug` tag**, stripping it every tick from
  anyone who had added it by hand.
- **A bare `/trigger ra.settings.admin` performed an arbitrary action** instead of
  opening the index.
- **Back redrew the page you had just left** on top of the index.
- **`/trigger` completion was cluttered** with nine blanket-enabled triggers. They
  are now handed out where usable — a tool's while it is in hand, the jetpack's
  while one is worn, the settings menu's while a menu is drawn — leaving one.

### Changed

- Settings pages are buttons rather than a wall of text, grouped tunables then
  block switches, one namespace per line on the index.
- Player-facing links **run** a trigger rather than suggesting a function: a
  suggested `/function` is useless to somebody who cannot run one.

## [v5.1.14] - 2026-08-18 - Pictures

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

### Added

- Seven screenshots embedded across the wiki; a banner per module in the README
  with a generated media-missing placeholder for modules without one.
- Industrial Light section in Transport Networks.

### Fixed

- README listed removed tiered pipes/wires as separate blocks and omitted eight
  Transport Networks blocks, the Magic Crate, three jetpack kits, the Clipboard
  and the Multimeter. Counts corrected to 57 blocks, 7 tools, 66 recipes.
- Transport Networks referenced tiered pipes and a non-existent give function.

## [v5.1.13] - 2026-08-18 - Documented

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

### Added

- Recipes for the Clipboard and Multimeter, which had none.
- `docs/tools.md`, covering all five tools with recipes and usage.
- Unlock advancements for Battery, EU Breaker, Industrial Light, Boiler, Solar
  Panel, Wrench and Data Handler; `get_*` advancements for Battery, EU Breaker, Industrial Light, Clipboard
  and Multimeter.

### Removed

- Dead advancements: `unlock_electric_wire_netherite`,
  `unlock_liquid_pipe_netherite` (tiers were removed) and `unlock_conveyor` (no
  such recipe or block).

### Changed

- How It Works rewritten for the registry-driven wrench and the goggles/wrench
  split; Developer Guide documents migrations and the read-only registry.

## [v5.1.12] - 2026-08-18 - Depth

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

### Fixed

- Skins z-fought with the block underneath: the 1.004 overlay left too little
  clearance for the depth buffer at range. Now 1.02.
  `ra_migrations:5.1.11-to-5.1.12` clears existing skins so they redraw.

## [v5.1.11] - 2026-08-18 - Read Only

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

### Added

- `ra:tools/readonly/init_registry` - one type-keyed declaration of read-only
  properties, obeyed by both the Data Handler and the wrench. Replaces five
  per-module `hidden_fields` files and the `#ra:hidden_fields` tag.

### Removed

- `enabled` from the Ender vaults, Teleport Anchor and Infinite generators, which
  had no way to toggle it. Kept on the Wireless Emitter/Receiver and Multiblock
  Base, where the wrench's plain right-click is the control.

### Fixed

- The wrench counted cyclable entries before filtering read-only ones; both the
  menu and the click path now build the list through one function so row indices
  cannot diverge.

## [v5.1.10] - 2026-08-18 - No Second Switch

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

### Fixed

- Electric Furnace duplicated items: `find_try` stopped writing the slot into
  `ef.hit`, so `take_input`'s macro had no `$(slot)`, failed entirely, removed
  nothing, and left a stale `#ef.took` that made `deliver` run anyway.

### Removed

- The `enabled` property throughout `ra_wires`; `ra_migrations:5.1.9-to-5.1.10`
  strips it from existing markers.

### Changed

- The EU Switch is redstone-driven: powered conducts, unpowered cuts.
- Menu buttons are bold and spaced.

## [v5.1.9] - 2026-08-18 - One Tool

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

### Added

- Wrench menu for blocks with more than one cyclable property; one property still
  cycles directly with no menu.
- `ra:tools/wrench/init_registry` - each block declares a list of cyclable
  properties, replacing a branch per block.
- Markers record `data.type`; `ra_migrations:5.1.8-to-5.1.9` backfills it.

### Removed

- The goggles tinker. The goggles read, the wrench changes. `enabled` and the
  Liquid Drain's mode moved to the wrench.

## [v5.1.8] - 2026-08-18 - Reach, Lift, Scorch

**Supported versions:** 1.21.9 - 26.2 (data pack formats 88 - 107).

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

- **Magic Crate** - pulls dropped items in from a configurable 5-20 block radius.
  Eight items per pulse, whole stacks moved verbatim, drops its full inventory when
  broken.
- **Jetpack upgrade kits** - Thruster (+45% movement speed), Lift (climbs and sinks
  about twice as fast), Scorch (sets fire to anything in a 3x3 column six blocks
  under the exhaust, excluding players, items and the pack's own marker and display
  entities).

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


- A vertical Liquid Drain only ever looked at a sneaking player's main hand, so a
  container or loose items on top of it were ignored. It now tries the hand, then a
  container above, then loose items above; only the hand path requires sneaking.
- Status billboards drew inside their own block from the third line down: the text
  anchor was the block centre and the top face is only 0.5 above it. The anchor is
  now above the block, with a floor so no ladder can reach back into it.
- EU Generators reported "No fuel" with coal in them. The fuel check only ever
  looked at the first occupied stack, so anything that was not fuel sitting in the
  barrel hid the coal behind it. It now asks the whole container.
- The Industrial Light never lit. It read `eu_use` with an unguarded `data get`,
  which stores **zero** on a failed read, so it drew nothing, was given nothing,
  and read that as the grid being unable to pay - regardless of how much EU there
  was. Now read through `ra_lib:util/property`, and the goggles name the missing
  condition.
- Valves and EU Breakers compared raw amounts instead of fill fraction, so a large
  near-empty tank farm looked fuller than a small near-full pipe stub and never got
  topped up. They also moved the whole gap, which swaps two networks rather than
  levelling them; they now move half.

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


- EU Generators light up, emit smoke and flame, and cast light while burning.
- Added `/function ra_wires:debug/generator` and `/function ra_wires:debug/light`.

## [v5.1.7] - 2026-08-18 - Grids, Bridges, Millilitres

**Supported versions:** 1.21.9 – 26.2 (data pack formats 88 – 107).

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

## [v5.1.6] - 2026-08-17 - Recipe Atlas, Data Handler Repairs, Licence

A follow-up to v5.1.5. One page that holds every recipe, a Data Handler that no
longer mangles the values it edits, blocks that decide for themselves what a survival
player may retune, and a licence that says what it was always meant to say.

**Supported versions:** 1.21.9 – 26.2 (data pack formats 88 – 107).

### Added

- **[Recipe Atlas](https://ancarsenat.github.io/Redstone-Additions/recipe-atlas/)** —
  every recipe in the pack on one page: alphabetically, so you can find one by name,
  and again grouped by module with the station it is made at, its namespace id and the
  give-everything command. Generated by `tools/recipe_render/render.py --atlas` from
  the recipe files, taking each name from the recipe's own `minecraft:item_name`, so it
  cannot drift from the pack. Adding a module is one line in `atlas.py`.
  - It lists what has **no** recipe too: the three generator Cores and the Infinite
    Iron Jetpack Kit, read out of the `enchant_recipes` functions with their sacrifice
    and chance, and indexed by name like everything else.
- Each block declares which of its own fields the survival Data Handler should hide,
  through the new function tag `#ra:hidden_fields`. Generators hide `cooldown`,
  electric blocks hide `transfer_rate` and their rate fields, pipes and wires hide
  `tier`, multiblock bases hide `tier` and `tier_level`, ender vaults hide
  `transfer_rate`. A block that declares nothing hides nothing.
  - It has to belong to the block: `cooldown` is a tuning knob on a generator and the
    entire point of a Clock, so one global list of names would take the Clock's period
    away with it.
  - Creative mode is the test, since a data pack cannot read permission level. The
    Creative Data Handler shows everything.
- `/function ra_ender:debug/vaults` — each vault's mode, channel, reachable partners,
  and whether its channel is a string at all, which is the one thing invisible in chat:
  `5` and `"5"` print identically.

### Fixed

- **The Data Handler asked for a number when editing any string**, and writing one
  broke whatever it touched. Its type probe treated "`data get` succeeded" as "this is
  a number", but `data get` succeeds on a string too — it returns the string's length.
  A vault `channel` written that way matches no string comparison, which is why a
  sending vault stopped finding its partner. The probe now asks
  `data modify … set string`, which only accepts a string.
- **Properties were hidden by default.** The registry decided which fields had rows, so
  a property it did not know about appeared nowhere at all, and the raw `Properties:`
  line printed hidden fields anyway — a field withheld from the editor was still
  readable one line above it. Everything a block carries is listed now, anything
  without an editor is counted and named, and redaction covers the raw line.
- **Twenty-three block types never announced themselves on load.** `ra_wires` printed
  one line for sixteen blocks, `ra_wireless` printed nothing, and the five multiblock
  bases were silent. 51 lines for 51 block types now.
- **Thirteen more announced to everybody.** The interactive machines, the sensors and
  the chunk loader used a bare `tellraw @a`, so every player on the server read a
  registration list at every reload. All gated to `ra.debug`.
- Coal or charcoal in the **offhand** did not count as jetpack fuel: `container.*`
  covers a player's 36 inventory slots, not `weapon.offhand`, while `clear` would have
  taken it happily.
- `ra.ender.grace` was seeded after the anchors had already looked for a player, and a
  selector on a score skips anyone who has none — so an anchor could not fire on a
  player's first tick in the world.
- An enchant-craft product was claimed within 1.5 blocks, wide enough for two tables a
  block apart to take each other's result on a tick where both succeeded. The product
  spawns 0.3 above the table; 0.6 is enough.

### Documentation

- **Licence rewritten (v1.1).** The old §3 forbade redistributing the pack "in whole or
  in part" and then, one paragraph later, permitted redistributing it "in part and
  modified" — so the two rules contradicted each other and the permissive reading was
  there for anyone shipping RA files. Addons are their own clause now: build and publish
  them anywhere, on any terms, no permission needed, no RA namespace inside.
  - Snippets from the documentation and `ra_example` are licensed for addon use. The
    docs hand out 48 code blocks to copy, which the old terms forbade.
  - "RA Namespace" means shipping in the official download, not starting with `ra_` —
    the documented example addon is called `ra_myaddon`.
  - Adds a contributions clause, a Mojang assets clause (the recipe pictures are drawn
    from vanilla textures and are not the author's to reserve rights over), and a
    version and date.
  - Revocation applies to distribution permission, not to the use and addon grants, so
    an addon already written does not stop being allowed.
- The home page warned that redstone on the Boxer and Unboxer had become a **lock** and
  told players to remove the signal from existing builds. Both have always shipped
  running while powered — that inversion was a mid-v5.1.4 workaround, reverted in the
  same release. Following the warning would have broken working builds.
- The readme covers the four new modules: Ender Links in the block list, an Items
  section for the jetpack kits, the casing and the cores, preview rows, and corrected
  counts — 52 blocks, 58 recipes.
- `site/` is no longer committed. It was a mkdocs build from v5.1.2, two releases stale,
  and the reason the docs looked like they had never been bumped; the published wiki is
  built from `docs/` by CI.
- Enchant Crafting opens with a four-line summary of the mechanic.

### Known limitations

- Ender link mode counts **stacks**, so taking part of a stack does not read as an
  extraction and pulls no refill. Take a whole stack, or use `shared` mode.
- A vault beyond ±2,147,483 blocks would move stacks to the wrong block: its block
  coordinates are read as thousandths, which overflows a scoreboard int there.


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

### Diagnostics

- `/function ra_infinite:debug/poppy` — every Poppy Generator's marker position,
  rotation, facing, cooldown, the block in front and the ground verdict.
- `/function ra_wires:debug/electric` — every electric node's buffer, rate and
  properties, whether its `enabled` flag is a byte at all, whether the transfer latch
  is stuck, and how many neighbours the adjacency probe reaches, using the same
  offsets and radius the transfer itself uses.

### Changed

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
