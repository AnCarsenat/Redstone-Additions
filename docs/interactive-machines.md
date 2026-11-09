# Interactive Machines

Machines for everyday automation: break and place blocks, move and sort items, breed animals,
endless water, lava and powder snow, item magnets, mob-proof torches, message blocks and
elevators.

## Getting them

| Block | Looks like | Recipe |
| --- | --- | --- |
| Block Breaker | dispenser | ![Block Breaker recipe](images/recipes/ra_interactive/block_breaker.png){ width="220" } |
| Block Placer | dispenser | ![Block Placer recipe](images/recipes/ra_interactive/block_placer.png){ width="220" } |
| Item Pipe | dispenser | ![Item Pipe recipe](images/recipes/ra_interactive/item_pipe.png){ width="220" } |
| Item Mover | observer | ![Item Mover recipe](images/recipes/ra_interactive/item_mover.png){ width="220" } |
| Spitter | dropper | ![Spitter recipe](images/recipes/ra_interactive/spitter.png){ width="220" } |
| Breeder | dispenser | ![Breeder recipe](images/recipes/ra_interactive/breeder.png){ width="220" } |
| Infinite Water Cauldron | cauldron | ![Infinite Water Cauldron recipe](images/recipes/ra_interactive/infinite_water_cauldron.png){ width="220" } |
| Infinite Lava Cauldron | cauldron | ![Infinite Lava Cauldron recipe](images/recipes/ra_interactive/infinite_lava_cauldron.png){ width="220" } |
| Infinite Snow Cauldron | cauldron | ![Infinite Snow Cauldron recipe](images/recipes/ra_interactive/infinite_snow_cauldron.png){ width="220" } |
| Magic Crate | barrel | ![Magic Crate recipe](images/recipes/ra_interactive/magic_crate.png){ width="220" } |
| Big Torch | a tall torch | ![Big Torch recipe](images/recipes/ra_interactive/big_torch.png){ width="220" } |
| Message Block | note block | ![Message Block recipe](images/recipes/ra_interactive/message_block.png){ width="220" } |
| Elevator | white wool | ![Elevator recipe](images/recipes/ra_interactive/elevator.png){ width="220" } |

Operators: `/function ra_interactive:items/give_all`.

## At a glance

| Block | Needs redstone? | Does |
| --- | --- | --- |
| Block Breaker | while powered | Breaks the block in front of it, once a second; the block drops as an item |
| Block Placer | while powered | Places a block from its inventory into the empty space in front, once a second |
| Item Pipe | no | Carries items forward from container to container, and can sort them |
| Item Mover | no | Moves items from the container behind it to the container in front |
| Spitter | no | Throws its items out of its face as dropped items |
| Breeder | while powered | Feeds the animals in front of it from its inventory |
| Infinite Cauldrons | no | Always full of water, lava or powder snow |
| Magic Crate | no | Pulls dropped items within range into itself |
| Big Torch | no | Lights the area and removes hostile mobs that spawn nearby |
| Message Block | when it gets powered | Sends a chat message to nearby players |
| Elevator | no | Jump on it to go up, sneak to go down, to the next Elevator in the column |

"Front" is the direction the block faces when you place it.

## Block Breaker and Block Placer

- Face them at the spot to work on and power them (a lever keeps them running, a clock works too).
- Each acts at most once per `cooldown` ticks: 20 (once a second) by default, 1–1200. The
  cooldown is a server admin's setting: players with server-settings access change it with the
  [Data Handler](tools.md); for everyone else it is locked.
- The Block Breaker cannot break unbreakable blocks such as bedrock.
- The Block Placer places the first stack in its inventory, and only into air (cave air
  counts too). If that stack is not a block (a diamond, say), it waits and uses none of it: take
  it out to let the placer carry on.

## Item Pipe

Pipes move items forward from one container (or pipe) to the next, a whole stack at a time when
there is room, every 4 ticks. When the next container is full, items wait in the pipe — nothing
is dropped.

**Sorting:** a pipe can have a **filter**. Items matching it are pushed into a container at one
of the pipe's four sides instead of continuing forward (never back into the container feeding
it); everything else carries on.

1. Shift + right-click the pipe with the [Data Handler](tools.md).
2. On the `filter_item` row, hold the item to sort and press **[Set from hand]** (or type an item
   id with **[Modify]**).
3. **[Clear]** on the same row removes the filter.

**Inverted filter:** toggle the pipe's `invert` setting (off by default) and the filter works the
other way round: items that do **not** match go to the side, and the filtered item carries on
forward. Without a filter, `invert` does nothing.

Wear the [Goggles](tools.md) to see each pipe's filter (and *Mode: Inverted* when it is), with the
filter item floating above it. The item preview can be turned off for the whole server under
Server Settings › General › **Goggles** › *Filter previews*, and each player can stop asking for it
with *Show filter previews* in their own settings. The label is shared, so a pipe still draws the
item while another Goggles wearer near it wants previews (see [Goggles](tools.md#goggles)).

## Item Mover

Place it with its back against the source container and its face against the destination. It
moves items across continuously, checking the destination has room first.

## Spitter

Put items in it; every 4 ticks it throws the first stack out of its face as a dropped item. No
redstone needed.

## Breeder

1. Put the right food in the Breeder itself (it has a chest-like inventory and never throws its
   items). It feeds only from its own inventory, not from a container next to it.
2. Keep two adult animals of the same kind in front of it (within two blocks of the block it
   faces).
3. Power it. It uses one food per pair it feeds.

| Animal | Food |
|---|---|
| Cow, Mooshroom, Sheep, Goat | wheat |
| Pig | carrot, potato, beetroot |
| Chicken | wheat seeds, melon seeds, pumpkin seeds, beetroot seeds, torchflower seeds, pitcher pod |
| Rabbit | carrot, golden carrot, dandelion |
| Horse | golden apple, golden carrot, enchanted golden apple |
| Llama | hay block |
| Turtle | seagrass |
| Panda | bamboo |
| Fox | sweet berries, glow berries |
| Bee | any flower |
| Wolf | any raw or cooked meat, rotten flesh |
| Cat | cod, salmon |
| Axolotl | bucket of tropical fish |
| Strider | warped fungus |
| Hoglin | crimson fungus |
| Camel | cactus |
| Sniffer | torchflower seeds |
| Frog | slime ball |
| Armadillo | spider eye |

## Infinite Cauldrons

Use them like normal cauldrons: fill buckets from them as often as you like. The water and snow
versions stay at full level, the lava version stays full of lava.

## Magic Crate

A barrel that pulls dropped items into itself.

| Setting | Default | Range | Meaning |
| --- | --- | --- | --- |
| `radius` | 8 | 5–20 (admins: up to 64) | How far it reaches, in blocks |
| `cooldown` | 20 | 1–1200 | Ticks between pulls; a server admin's setting, locked for other players |

It takes up to eight items (whole stacks, enchantments and names kept) per pull, and stops when it
has no empty slot left — the Goggles then show `Full`. Breaking it drops everything inside.

## Big Torch

A torch one block tall that lights its area (light level 14) and stops hostile mobs from
**spawning** near it: any hostile mob that appears within `radius` blocks is removed — dropped out
of the world, so it leaves no loot or experience. Mobs that **walk in** from outside are left alone.

| Setting | Default | Range |
| --- | --- | --- |
| `radius` | 16 | 1–100 (admins: up to 256) |

## Message Block

When it gets powered, it sends its message to the players picked by its selector.

| Setting | Default |
| --- | --- |
| `message_block` (the text) | `Hello World!` |
| `entity_selector` (who reads it) | `@a[distance=..16]` |

Anyone can change the text: it is shown exactly as typed, quotes and braces included. Rich text
(colours, clickable links) is a compound only an operator can write, with the Creative Data
Handler or `/data`; the Clipboard does not paste one for players without server-settings access.
The selector is admin-only unless *Players may edit entity selectors and tags* is on (Sensors
settings page).

A block without a selector sends to the players within its `range` (1–128 blocks; admins: up to
1024).

## Elevator

Put Elevators in a column, one per floor, and stand on one: **jump** to go up to the next
Elevator above, **sneak** to go down to the next one below. You land on top of it, looking the
same way. One craft gives two.

| Setting | Default | Range | Meaning |
| --- | --- | --- | --- |
| `range` | 16 | 2–64 (admins: up to 384) | How far the next Elevator may be, in blocks |

- The next Elevator must be straight above or below, in the same column, within `range`.
- It needs room: the two blocks on top of it must be air. Otherwise nothing happens (a low note
  plays), and an Elevator further on is not tried: the ride only ever goes one floor.
- Pressing sneak moves you once; holding it does not keep going down. Holding jump takes you up
  one floor per landing.
- The teleport sound and particles follow your block sound and particle settings.

## Changing settings

Every setting above is edited with the [Data Handler](tools.md): Shift + right-click the block,
then change the row. The [Goggles](tools.md) show the current values.

---

## Advanced

- **Physical blocks.** The Breeder is a barrel wearing a dispenser skin, so redstone never makes it
  throw its food. The Block Breaker and Block Placer are real dispensers and react to
  `dispenser[triggered=true]`. The Big Torch is an end rod (the light source) wearing an
  enlarged torch display; the Magic Crate is a plain barrel.
- **Big Torch.** Sweeps every 10 ticks. It remembers every hostile mob in a band reaching 16
  blocks past the radius; a mob inside the radius that it never saw there is removed. Mobs are
  teleported far below the world instead of killed. Values above 256 (the admin ceiling) are written back as 256. The
  mob list is the entity type tag `#ra_interactive:spawn_blocked`.
- **Magic Crate.** Skips items with a permanent pickup delay. The radius is capped at 64 (the
  admin ceiling) when used, since an unlimited radius would be a world-wide entity scan; the
  players' 20 is checked when the radius is written, so an admin's larger radius stays.
- **Elevator.** Each Elevator tests only the block above it for a player, once a tick. A jump is
  the `minecraft.jump` statistic (`ra.elev.jump`), a sneak the `ra:is_sneaking` predicate against
  last tick's state (`ra.elev.sn`); a player who was not on an Elevator the tick before
  (`ra.elev.t`, game time) is only synced, so a jump taken elsewhere never fires. The next floor is
  the nearest Elevator marker in a `dx=0,dz=0` box above or below, not a block scan. 6 ticks
  between rides (`ra.elev.cd`).
- **Item Pipe.** The filter is `data.properties.filter_item` (an item id, empty = none);
  `data.properties.invert` (`1b`) sends non-matching stacks to the side instead. Pipes
  built when filters were item frames are converted on load; the frames are left in place.
- **Properties** live in each block marker's `data.properties`; machines pace themselves with the
  `ra.cooldown` score.
