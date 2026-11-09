# Transport Networks

Pipes for liquids and gases, wires for electricity (EU), and the machines that fill, store and use
them: pumps, tanks, drains, spouts, boilers, generators, solar panels, water wheels, batteries, an electric furnace, lights
and a tesla coil.

![An EU grid running](images/screenshots/Electricity.png)

*An EU grid: generator, wire, battery and consumers, with the Goggles showing what each one holds.*

## Getting them

| Block | Recipe |
| --- | --- |
| Copper Pipe | ![Copper Pipe recipe](images/recipes/ra_wires/liquid_pipe_copper.png){ width="220" } |
| Liquid Tank | ![Liquid Tank recipe](images/recipes/ra_wires/liquid_tank.png){ width="220" } |
| Liquid Pump | ![Liquid Pump recipe](images/recipes/ra_wires/liquid_pump.png){ width="220" } |
| Fluid Drain | ![Fluid Drain recipe](images/recipes/ra_wires/liquid_drain.png){ width="220" } |
| Liquid Valve | ![Liquid Valve recipe](images/recipes/ra_wires/liquid_valve.png){ width="220" } |
| Liquid Filter | ![Liquid Filter recipe](images/recipes/ra_wires/liquid_filter.png){ width="220" } |
| Gas Tank | ![Gas Tank recipe](images/recipes/ra_wires/gas_tank.png){ width="220" } |
| Gas Pump | ![Gas Pump recipe](images/recipes/ra_wires/gas_pump.png){ width="220" } |
| Gas Valve | ![Gas Valve recipe](images/recipes/ra_wires/gas_valve.png){ width="220" } |
| Boiler | ![Boiler recipe](images/recipes/ra_wires/boiler.png){ width="220" } |
| Wire | ![Wire recipe](images/recipes/ra_wires/electric_wire_copper.png){ width="220" } |
| EU Generator | ![EU Generator recipe](images/recipes/ra_wires/electric_generator.png){ width="220" } |
| Solar Panel | ![Solar Panel recipe](images/recipes/ra_wires/solar_panel.png){ width="220" } |
| Battery | ![Battery recipe](images/recipes/ra_wires/battery.png){ width="220" } |
| EU Switch | ![EU Switch recipe](images/recipes/ra_wires/electric_switch.png){ width="220" } |
| EU Breaker | ![EU Breaker recipe](images/recipes/ra_wires/electric_breaker.png){ width="220" } |
| EU Consumer | ![EU Consumer recipe](images/recipes/ra_wires/electric_consumer.png){ width="220" } |
| Electric Furnace | ![Electric Furnace recipe](images/recipes/ra_wires/electric_furnace.png){ width="220" } |
| Industrial Light | ![Industrial Light recipe](images/recipes/ra_wires/industrial_light.png){ width="220" } |
| Network Gauge | ![Network Gauge recipe](images/recipes/ra_wires/network_gauge.png){ width="220" } |
| Usage Meter | ![Usage Meter recipe](images/recipes/ra_wires/usage_meter.png){ width="220" } |
| Tesla Coil | ![Tesla Coil recipe](images/recipes/ra_wires/tesla_coil.png){ width="220" } |
| Spout | ![Spout recipe](images/recipes/ra_wires/spout.png){ width="220" } |
| Water Wheel | ![Water Wheel recipe](images/recipes/ra_wires/water_wheel.png){ width="220" } |

Operators: the **Wires Bundle** (`/function ra:items/bundles/give_wires_bundle`) has everything;
`/function ra_wires:items/give_creative` gives the two creative sources.

## How networks work

Blocks of the same family that touch form one **network**:

- **Fluid network** — pipes, tanks, pumps, drains (liquids and gases share the same pipes).
- **Electric grid** — wires, generators, solar panels, batteries, consumers, the furnace, lights.

A network has **one shared pool**. Whatever a pump or generator adds is instantly available to
every drain or machine on the same network, however far away. Each block adds to the network's
capacity:

| Block | Holds |
|---|---|
| Pipe, Spout | 1000 mL |
| Pump, Drain | 5000 mL |
| Liquid Tank, Gas Tank | 100000 mL (20 buckets) |
| Wire, EU Switch, EU Breaker, Industrial Light, Tesla Coil | 0 EU |
| EU Generator, EU Consumer, Solar Panel, Electric Furnace | 50 EU |
| Water Wheel | 1000 EU |
| **Battery** | 10000 EU |

Fluids are measured in millilitres: **a bucket is 5000 mL**.

!!! tip "Wire stores nothing"
    Without a Battery, power that is not used straight away is lost. Put Batteries on any grid
    with a generator or solar panel.

Different families never mix, so an electric wire and a pipe can run side by side.

## Fluids

### Getting fluid in and out

| Block | Does |
| --- | --- |
| **Liquid Pump** | Pulls a fluid source block next to it (any side) into the network — only when the whole 5000 mL fits. Cauldrons give 1667 + 1667 + 1666 mL |
| **Fluid Drain** | Same as the pump, taking in a source block next to it — or, failing that, one block down and out, so a drain standing on the bank of a pool drains the pool. Water and lava fall onto the drain as it does |
| **Fluid Drain**, powered by redstone | Pours out instead, from its **front** (see [Pouring out](#pouring-out)), or on a gas network lets the gas out with its effect ([Venting gases](#venting-gases)). Needs nothing in the world: air in front of it is enough |
| **Fluid Drain**, facing up or down | Also a **loading point**: takes filled buckets/bottles from you (sneak while holding one), from a container on top of it, or from items dropped on it, and gives the empties back; with none of those, a sneaking player's **experience**. With nothing to load, it takes in a source block next to it like any drain |
| **Spout** | Fills empty buckets and bottles under it from the network, one a second ([Spout](#spout)) |
| **Gas Pump** | Like the Liquid Pump, for gases: draws **Smoke** from a lit campfire or soul campfire next to it, 500 mL a second. The campfire keeps burning and is never used up. Steam comes from a Boiler |

A drain points where you look when you place it, so looking down to place one makes it face up.
That is fine: an upright drain still drains the pool beside it.

The Goggles say what a drain is doing: `pumping`; `no_source` (nothing it can take beside it, or
one block down and out); `waiting_for_source` (only flowing water or lava there, as when a small
pool is refilling the gap); `network_full` (wait for the network to have room for a whole
5000 mL block, or add a tank); `no_network` (the drain is not part of a network).

#### Pouring out

A drain powered by redstone pours its network's fluid (the primary one) out of its **front**: the
side it faces, like a dispenser. It never pours out of another side, so you always know where it
will go. A drain stood on the floor faces up, and pours on top of itself.

| Fluid | What comes out of the front |
| --- | --- |
| Water, Lava, Powder Snow | One source block, 5000 mL — the same amount a drain takes in for one, so nothing is gained or lost going round. Goes into air, into grass, flowers, a snow layer or anything else a bucket would wash away (it drops as a bucket would make it drop), or into flowing water or lava of the same kind, which becomes a source |
| Experience | Experience orbs, 1 point per 100 mL, up to 50 points at a time |
| Potions | Poured over everyone within 4 blocks, in the potion's own colour ([Potions](#potions)) |
| Milk, Steam, Smoke, Oxygen, Toxic Gas | Poured away as white drops, a cloud, smoke, blue or green mist. It really leaves the network. Oxygen and Toxic Gas also give their effect ([Venting gases](#venting-gases)) |

The Goggles show `pouring` while it works, and otherwise why it is not:

| Status | Meaning |
| --- | --- |
| `blocked` | The block in front is solid, is already a source block, or is a different liquid |
| `not_enough` | The network holds less than 5000 mL of that fluid, so not a whole block |
| `network_empty` | Nothing to pour |
| `evaporates` | Water in the Nether (or anywhere water boils away): as with a bucket, the 5000 mL is used up and boils off in a puff of steam, and no block is placed |
| `cannot_place_medium` | A fluid with no way out at all |

A gas shows `venting` instead of `pouring`.

A drain makes no sound while it pumps or pours.

#### Venting gases

The **Fluid Drain** (called the Liquid Drain before 6.0.3) works on gas networks too: powered by
redstone, it lets the network's gas out of its front, up to its *rate*, with the gas's effect:

| Gas | Where | Effect |
| --- | --- | --- |
| Oxygen | In the water **straight above** the drain's front, up to 16 blocks or until the water ends, like a magma block's bubble column (with bubbles) | Water Breathing, 10 seconds, given again at each action |
| Toxic Gas | Everyone and every mob within **4 blocks** of the drain's front | Wither, 3 seconds, given again at each action |
| Steam, Smoke | — | none: only vented, as a cloud or smoke |

For an underwater air supply, stand a drain on the floor of the pool facing up, on an oxygen
network, and give it redstone: anyone swimming in the column above it can breathe.

At the default rate an action comes once a second, so the effect lasts as long as the gas does.
Oxygen and Toxic Gas have no source in survival yet; the Creative Fluid Source makes both. The
mist follows each player's particle setting.

A drain's **rate** is in millilitres per second, for taking in and pouring out alike: 5000 (the
default) is one source block a second, 10000 is two, 1000 is one every five seconds, from 100 up
to 100000 (one every tick). Only operators change it, with the [Data Handler](tools.md); the
default for new drains is in the [Server Settings](settings.md).

**Infinite sources:** a source block with **nine or more** matching sources within two blocks is
left in place when pumped, so a large lake never runs dry. Smaller pools do empty.

### Fluids you can move

| Fluid | Kind |
|---|---|
| Water, Lava, Powder Snow, Milk | liquid |
| Steam, Smoke, Oxygen, Toxic Gas | gas |
| Experience | 1 point = 100 mL; a powered Drain gives it back as orbs |
| Potion | keeps its effects (see below) |

### Several fluids in one pipe

A network can carry several fluids at once; it is full when the **total** reaches its capacity. The
fluid that arrived first is the **primary** one: it is what a Valve moves and what a Drain places
unless told otherwise.

To split a mixed pipe back into separate ones, use **Liquid Filters** (below).

### Valves, Filters and Breakers

These connect **two** networks without merging them, and only work **while powered by redstone**.
They move contents from the fuller side to the emptier one until both are level, up to their rate
per tick. They work whichever way round you place them.

| Block | Connects | Moves |
| --- | --- | --- |
| Liquid Valve, Gas Valve | two fluid networks | the primary fluid |
| Liquid Filter | two fluid networks | only its `filter_medium` (default `water`) — set it with the [Data Handler](tools.md) to `lava`, `steam`, `potion`… |
| EU Breaker | two electric grids | EU — lets two bases with their own batteries share power |

To cut a pipe completely, remove a pipe block. To cut a wire, use an **EU Switch**: without a redstone
signal it splits the grid in two; powered, it joins it again.

### Spout

A **Spout** fills empty containers from its network, one a second: put it on a pipe line with a
container under it.

| Empty | Becomes | Takes |
| --- | --- | --- |
| Bucket | Water, Lava, Powder Snow or Milk Bucket | 5000 mL |
| Glass Bottle | Water Bottle | 1000 mL of water |
| Glass Bottle | the network's potion, with its effects and colour ([Potions](#potions)) | 1000 mL of potion |
| Glass Bottle | Bottle o' Enchanting | 700 mL of experience |

- **Under it**: a container (hopper, chest, barrel, brewing stand…) holding empties. The filled
  item goes back into the same container, or is dropped on top of it when it is full. A hopper
  under the Spout feeding a chest is a bottling line: hoppers bring the empties in and carry the
  filled ones on.
- Or an empty **dropped** in the space under it: the filled one is dropped in its place.
- On a network holding several fluids, the one that arrived first is tried first.
- It only takes a whole container's worth, or nothing.
- **Redstone locks it**, as it does a hopper.
- The Goggles show what it is doing: `filling`, `idle` (nothing under it), `nothing_to_fill`
  (nothing under it this network can fill), `not_enough` (less than a whole container of that
  fluid), `network_empty`, `locked`, `no_network`.

### Potions

Pour a potion into a Drain and the network remembers which potion it is (the **first** potion in
wins; a different potion added later becomes the same one). A Drain powered by redstone pours it over
everyone within 4 blocks of its front, up to 5000 mL at a time, as a cloud in the potion's colour.
A full bottle (1000 mL) gives the potion's full duration; more gives longer, less gives a
proportional share — 250 mL of a 3-minute Strength potion is 45 seconds. The level is unchanged.

## Electricity

### Making power

| Source | Makes |
| --- | --- |
| **EU Generator** | 60 EU per tick. Burns **steam** from a network, or solid fuel (coal etc.) put into it — it opens like a chest |
| **Solar Panel** | 10–50 EU per tick from sky light: most at noon, less at night, in rain, under a roof or snow |
| **Water Wheel** | 5 EU per tick for each block of flowing water around its paddles, up to 40, day and night ([Water Wheel](#water-wheel)) |
| **Boiler** | Not power itself: turns 1000 mL of water into 1000 mL of steam every second |

**Steam power chain:**

```text
water network → Boiler (over a heat source) → steam network → EU Generator → EU
```

The Boiler needs one of these under it: lava, fire, soul fire, magma block, campfire, soul
campfire, lava cauldron. It sits **between** the water pipe and the steam pipe and is not part of
either.

### Water Wheel

A **Water Wheel** makes EU from flowing water. Its wheel turns **facing you** as you place it
(its axle points the way you look), so put it where a current runs across your view.

- The eight blocks around the hub in the wheel's plane (above, below, either side and the four
  corners) are where its paddles dip. Each one holding **flowing or falling water** adds
  *eu_per_block* EU per tick (default 5): one block makes 5 EU/t, all eight 40 EU/t. A still
  source block turns nothing: the wheel needs a current, such as water running off a source or a
  waterfall onto the paddles.
- It works day and night, and puts each second's EU on the grid at once.
- The wheel turns smoothly while water flows, faster with three or more flowing blocks, and stops
  where it is when the water does. It keeps turning when the grid is full (the Goggles then say
  `Grid full`).
- *eu_per_block* is for admins only (1 to 1000); the default for new wheels is in the
  [Server Settings](settings.md).
- The Goggles show `Turning`, `No flowing water` or `Grid full`, how many flowing water blocks it
  found, what it makes and the grid's EU. The splash at its foot follows each player's particle
  setting.

### Using power

| Block | Uses |
| --- | --- |
| **EU Consumer** | A power load: draws a fixed amount every tick, all or nothing (20 / **40** / 80 EU per tick, cycled with the Wrench). Sparks while running |
| **Electric Furnace** | Smelts with EU instead of fuel (below) |
| **Industrial Light** | With redstone **and** EU, shines a 10-block beam of light (stops at the first solid block). Uses 10 EU by default (`eu_use`) |
| **Tesla Coil** | With redstone **and** EU, zaps hostile mobs around it once a second ([Tesla Coil](#tesla-coil)) |
| **Battery** | Stores 10000 EU for later |

### Electric Furnace

![Electric Furnace smelting on EU](images/screenshots/ElectricFurnace.png)

1. Put items to smelt into the furnace (it opens like a chest; hoppers work from any side).
2. Put a container on the **output face**: under, front, back or top — choose it with the
   [Wrench](tools.md).
3. Connect it to a powered grid.

Results always go to the output container, never back into the furnace. If that container is
missing or full, the furnace stops and the Goggles show `Output blocked` — nothing is wasted.

| Power mode | Ticks per item | EU per item | EU per tick |
| --- | --- | --- | --- |
| low | 100 | 40 | 0.4 |
| medium | 50 | 80 | 1.6 |
| high | 20 | 160 | 8 |
| superpowered | 5 | 300 | 60 |

It smelts ores and raw metals, iron and gold gear back into nuggets, food, sand, clay, stone,
cracked bricks, and logs into charcoal.

### Tesla Coil

A **Tesla Coil** sits on an EU grid and, while it has a **redstone signal**, zaps every hostile mob
within its *radius* once a second, with an electric arc and a crackle.

- Each mob hit costs *eu_per_zap* EU (default 200) and takes *damage* (default 6, three hearts) of
  lightning damage. The grid pays per mob: with EU for only three, the three nearest are zapped.
  With no mob in range it costs nothing.
- At most 16 mobs a zap.
- Only hostile mobs are hit (the entity tag `#ra_wires:tesla_targets`): never players, pets,
  villagers, golems, animals or armour stands. Bosses are not on the list.
- *radius* is 1 to 16 blocks (default 8): Shift+right-click with the [Wrench](tools.md) steps it
  through 4, 8, 12 and 16, and the [Data Handler](tools.md) sets any value; server admins can go
  up to 32. *damage* and *eu_per_zap* are for admins only; the defaults for new coils are in
  the [Server Settings](settings.md).
- The Goggles show `Zapping`, `No targets`, `No EU`, `No redstone` or `No grid`, the radius, the
  cost per mob and the grid's EU.
- The arcs and the sound follow each player's particle and sound settings.

## Reading a network with redstone

### Network Gauge

A **Network Gauge** gives a redstone signal from how full a network is, the way a comparator reads
a chest: `ceil(15 × stored / capacity)`, so any amount above nothing gives at least 1 and only a
full network gives 15. It works on any network, fluid or electric: one gauge for a tank farm, a
gas line or a battery bank.

- Place it **against** a pipe, tank, wire or battery: like a dispenser it faces you, and it reads
  the network **behind** it. With nothing there, it reads the first network beside it.
- It is a target block and the signal comes from the block itself: dust, a repeater or a
  comparator next to it picks it up.
- It is not part of the network (like a valve), so placing or breaking one never splits or joins
  anything.
- It reads its network twice a second. A network with no capacity (wire with no Battery) reads 0.
- The Goggles show the output level, the network number, and what the network holds out of its
  capacity.

### Usage Meter

A **Usage Meter** counts what goes through a network, for an electricity or water bill: it gives
a short redstone pulse every *every* units (mL or EU), and keeps a running *total*.

- It is placed and finds its network like the Network Gauge, and is also a target block: the
  pulse comes from the block itself.
- *mode* `consumed` (the default) counts what is taken out of the network: what consumers,
  furnaces and lights use, what drains pour out, and what a valve or EU Breaker moves away to
  another network. `produced` counts what is put in: generators, solar panels, pumps, drains
  taking in, and what a valve or breaker brings in. Shift+right-click with the
  [Wrench](tools.md) switches the mode; the meter then counts from that moment.
- *every* is 1 to 100000000 (default 1000; the default for new meters is in the
  [Server Settings](settings.md)), set with the [Data Handler](tools.md).
- *total* is read only and stops at 1000000000. **Right-click with the Wrench** sets it back to 0,
  to start a new billing period.
- It reads its network twice a second and gives at most one 2-tick pulse each time: with a small
  *every* on a busy network, the pulses due wait their turn (shown as *Since last pulse*), so none
  is lost.
- It only counts from when it was placed. Several meters on one network each keep their own total.
- The Goggles show the mode, the total, *every*, what has gone through since the last pulse, and
  the network.

## Creative sources

For testing, with no recipe (`/function ra_wires:items/give_creative`):

| Block | Does |
| --- | --- |
| Creative EU Source | Refills its grid to full every tick |
| Creative Fluid Source | Adds `rate` mL of one fluid per tick (default 1000), until the network is full. Change the fluid with the Wrench: water, lava, powder snow, milk, steam, smoke, oxygen, toxic gas |

A network fed by a Creative Fluid Source fills up quickly, which leaves no room for a second fluid;
for mixed-fluid tests, give the run extra tanks.

## Checking a network

- **[Goggles](tools.md):** each block shows its fluid (or `Multimedium` for several) and amount,
  or its EU and whether it is running.
- **[Multimeter](tools.md):** right-click a block for its network number, contents per fluid,
  capacity and what this block adds.

```
── Copper Pipe ──
  Network 22   this block adds 1000
  Stored 10000 of 20000
  Holding
    Water  5000 mL
    Lava  5000 mL
```

Two blocks showing different network numbers are not connected. A network keeps its number while
you extend it.

![A fluid network](images/screenshots/Pipes.png)

---

## Advanced

- **Engine.** All networks run on `ra_lib:transport`: adjacent nodes of the same class are grouped
  by flood fill when a node is placed or broken (debounced to one rebuild every 5 ticks). Pipes,
  tanks, valves and wires do no work per tick; only blocks that move something between the world
  and a network are ticked.
- **Out of view.** A rebuild only touches the networks a change concerns; every other network,
  loaded or not, keeps its contents and its number. A network that reaches into unloaded chunks
  is never recounted or trimmed: a new pipe simply joins it, and a break that splits it takes
  effect once the whole network is loaded again (until then both halves share one pool).
  See [Scoped rebuilds](developer-guide.md#scoped-rebuilds).
- **After a load.** Every loaded network is rebuilt once, a few hundred blocks at a time every
  5 ticks, so a large world settles over a second or two instead of overrunning the command
  limit in one tick. Networks keep working with their contents while they wait their turn.
- **Storage.** Each network is `storage ra:transport nets.n<id>`: `amount` (total), `capacity`,
  `nodes` (block count), `amounts` (per fluid), `media` (fluids in arrival order), `medium` (the
  primary), and `potion`.
  See the [Developer Guide](developer-guide.md#transport) for the full model.
- **Properties** (`data.properties`): Drain `rate` (mL per second, admin only; one 5000 mL action
  every `100000 / rate` ticks); Filter `filter_medium`, `rate` (2000); Creative Fluid Source `medium`, `rate`; EU
  Consumer and Industrial Light `eu_use`; Electric Furnace `output`, `mode`. Status for the
  Goggles is in `data.status`. Drains from before 6.0.1 have their `cooldown` turned into `speed`
  (`100 / cooldown`) and `mode` removed once, the first time they are loaded, by the 6.0.1
  migration (`ra_migrations:6.0.0-to-6.0.1/liquid_drain`); the 6.0.2 migration
  (`ra_migrations:6.0.1-to-6.0.2/liquid_drain`, and the server default once per world) turns
  `speed` into `rate` (`speed * 1000`).
- **Media `drip`.** A medium may name a `drip` particle (`falling_water`, `falling_lava`) that a
  drain shows on itself while taking that medium in (`ra_wires:fluid/drip`); without one it shows
  the medium's `particle`.
- **Pouring out** (`ra_wires:fluid/drain_pour`): by the medium's registry fields: `block` is
  placed in front, `orb` comes out as experience orbs, `effect` (potions) is applied, and `pour`
  is the particle (with its options) a medium with none of those is poured away as. A potion's
  colour is its `custom_color`, else its first effect's from `ra:wires effect_colors`.
- **Fixed rates** such as a generator's `generation_rate` are read-only: the Data Handler shows them
  locked.
- **Heat sources** are the block tag `#ra_wires:heat_sources`.
- **Readouts** (`ra_wires:readout/*`): the Network Gauge is not a node; it finds the node behind
  it (`^ ^ ^-1` from its marker, which carries the placement facing), else the first face
  neighbour, and runs `net/read` as that node. It is updated by a slow clock scheduled every 10
  ticks only while a readout marker (`ra.wires.readout`) is loaded; there is no per-tick work
  besides the break check. Network records keep no "changed" flag: adding one would put a write
  on every transfer of every network for the sake of a few gauges. Status: `data.status.level`,
  `network`, `amount`, `capacity`, `unit` (`mL` or `EU`).
- **Usage counts.** A Usage Meter sets `meter:1b` and an `epoch` on its network's record; from
  then on `net/offer_write` and `net/take_write` add each transfer to the record's `produced` or
  `consumed` (every other network pays one `if data` per transfer). The counts are shared by
  every meter on the network and only grow: each meter keeps the last count it read
  (`data.data.last`, `data.data.epoch`) and adds the difference to its `total` and
  `data.data.progress`. A rebuild writes a fresh record without the flag; the next meter flags it
  with a new epoch, and a meter seeing a different epoch counts from 0. A count reaching
  1000000000 is lowered by that much by the meter reading it, and the others add it back.
  Properties: `mode` (`consumed` / `produced`), `every` (1–100000000), `total` (read only).
- **Machine clock** (`ra_wires:machine/*`): the Tesla Coil, the Spout and the Water Wheel act once a second, on a
  clock scheduled every 20 ticks only while a marker tagged `ra.wires.clocked` is loaded; their only
  per-tick work is the break check. Tesla Coil properties: `radius` (1–16, admin 32), `damage`
  (1–40) and `eu_per_zap` (1–100000), both admin only. Status `data.status.coil`, `last_hits`.
  Targets are the entity type tag `#ra_wires:tesla_targets`; another pack adds a mob to it to make
  it a target. Spout status `data.status.spout_state`.
- **Water Wheel** (`ra_wires:blocks/water_wheel/*`): property `eu_per_block` (1–1000, admin
  only); status `data.status.wheel`, `water`, `output` (EU/t), `accepted`. The marker's facing
  (placed with dir 3) is the axle: north or south turns the wheel in the X-Y plane, anything else
  in the Y-Z plane, and the hub log's `axis` is set to match. The wheel is sixteen block displays
  at the marker (`ra.wires.water_wheel`, `ra.wires.water_wheel.arm<0-7>`: a spoke and a slab
  paddle per arm). Turning is one `data merge` of `transformation.left_rotation` per arm each
  second, by 22.5 or 45 degrees, with `interpolation_duration` 20 so the client spreads it over
  the second; the translation only runs along the axle, so every point of an arm moves on a
  circle. `data.data.spin` (0–15) is the wheel's position, `data.data.axis` the axle vector. A
  wheel found without its displays rebuilds them on its next run; breaking it removes them.
- **Gas effects** (`storage ra:wires gas_effects`): what a powered Fluid Drain does with each gas
  it vents (`ra_wires:fluid/drain_pour_away`), keyed by medium, each entry
  `{effect, seconds, amplifier, area}` (the first three required; amplifier 0 is level I),
  given with `effect give` at each action. `area` is `"column"` (every living entity in the
  water or bubble column straight above the drain's front, up to 16 blocks, with bubble
  particles; `ra_wires:fluid/drain_pour_away_column`) or `"radius"` (every living entity within 4
  blocks of the front; the default when `area` is missing).
  Another pack registers its gas in the media registry (`state:"gas"`, with a `pour` particle for
  the mist) and sets `gas_effects.<its gas>` after `ra_wires:load`.
- **Filling containers** (`storage ra:wires item_fills`): what a Spout makes, keyed by medium, each
  entry `{empty, item, volume, components?, sound?}`; `potion:1b` gives the network's recorded
  `potion_contents` instead of `components`. Another pack appends to `item_fills.<medium>` after
  `ra_wires:load` to make its medium fillable. `item_sources` is the other direction (what a Drain
  empties).
- **Media registry** (`ra_wires:media/init`): display name, state, colour, particle, optional world
  block and bucket; pumpable blocks are listed as `source_blocks`, each with its medium's
  `state` (a pump or drain only considers sources of the state it carries) and `keep:1b` on a
  source that is never used up (the campfires). Preset potions get their effects
  from `ra_wires:media/potions`; a potion with `custom_effects` is read from the item.
- **Bridges** (Valves, Filters, Breakers, the Boiler, the Ender Power Vault) belong to neither
  network they touch, which is why a valve always splits a run in two. A bridge picks
  its fullest neighbouring network as the source and its emptiest as the destination, by fill
  ratio (`amount / capacity`, in ten-thousandths, divided in floating point so it holds at any
  network size). It then moves the smallest of its rate, what the source holds, the room left
  in the destination, and half the gap between them, so two networks level out instead of
  swapping contents back and forth.
- **Arithmetic.** Every network sum, clamp and bridge move is one 26.3 `/compute` over the
  network's storage; see [Arithmetic with `/compute`](developer-guide.md#arithmetic-with-compute).
- **Visuals.** Each node draws its own half of each connection; fluid pipes are a 0.56 copper or
  iron core, electric wires a thinner 0.26 concrete core.
