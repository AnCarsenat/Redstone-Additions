# Transport Networks

The `ra_wires` module adds liquid, gas, and EU transport.

- Namespace: `ra_wires`
- Give all: `/function ra_wires:items/give_all`
- Runtime architecture: [How It Works](how-it-works.md)

## Block Families

| Family | Blocks | Notes |
|---|---|---|
| Fluid | L1 Copper / L2 Iron Pipe, Liquid Tank, Liquid Pump, Liquid Valve, Liquid Drain | Liquids and gases share one pipe network |
| Gas | Gas Tank, Gas Pump, Gas Valve, Boiler | Gases use the same pipes as liquids |
| Electric | Copper / L2 Wire, EU Generator, EU Consumer, EU Switch, EU Breaker, Battery, Solar Panel | Grid-wide EU: batteries store it, breakers bridge grids |

## Flow Model

**Contents belong to the network, not to individual blocks — fluids and EU alike.**

Electric used to be the exception: every wire held its own charge and, once a
tick, handed half the difference to one neighbour that had less. That model
levels charge out, it does not deliver it. Charge crawled one block per tick, a
generator with six neighbours fed exactly one of them, and once a run had evened
out to within 1 EU everywhere the transfer guard stopped it moving at all, so a
consumer at the end of a line was fed by whatever leaked past the wires in front
of it. Electric now runs on the same engine as fluids, and everything below
applies to it word for word.

A connected run of pipes, tanks, pumps, valves and drains is one *network* with a
single medium, a single amount and a capacity equal to the sum of its members.
Adding a pipe adds capacity; it does not add another buffer that fluid has to be
pushed through.

That means:

- Nothing travels. Anything a pump or a generator adds is immediately available
  to every drain or consumer on the same network, however far away, on the same
  tick.
- Pipes, tanks, valves, wires and switches do **no** per-tick work. Only the
  blocks that move something between the world and the network are ticked —
  pumps, drains, boilers, generators, solar panels, consumers. A hundred-block
  run is free to keep running.
- Network membership is recomputed only when a node is placed or broken, and is
  debounced so laying a long run costs one rebuild rather than one per block.

Network state:

- `data.properties.*` — configuration the player can change (`enabled`, `mode`, `tier`).
- `data.status.*` — read-only values for goggles, redrawn once a second.

## Capacity is Storage, and Storage is Finite

A network holds exactly what its members can store, and nothing carries a hidden
buffer. Fluids are measured in **millilitres**; a bucket is 5000 mL.

| Block | Holds |
|---|---|
| Pipe (either tier) | 1000 mL |
| Pump, Drain | 2000 mL |
| Tank | 100000 mL (20 buckets) |
| Wire, EU Switch, EU Breaker | **0 EU** |
| EU Generator, Consumer, Solar Panel | 50 EU |
| **Battery** | 10000 EU |

Network totals live in `storage ra:transport nets.n<id>` rather than on a
scoreboard fake player. A scoreboard gives one number per network, which is
exactly the shape that has to change when a network can hold several media at
once; a compound already has room for the per-medium map. Arithmetic still goes
through scoreboards, because commands have no other way to add two numbers.

Wire stores nothing. A grid with no battery on it holds only the small working
buffers of its generators and consumers, so whatever is generated has to be spent
on the same tick or it is lost — build batteries or waste your output. This is
the electric answer to "a pipe adds a litre": a run of wire is not secretly a
tank.

A world source block pumps in as 5000 mL. A cauldron gives up its bucket across
its three levels, 1667 + 1667 + 1666. The Boiler trades 1000 mL of water for
1000 mL of steam per cycle.

## Media

Each medium carries its own colour and particle, so water splashes, lava sparks
and experience glows rather than everything emitting the same grey smoke.

Registered: water, lava, powder snow, milk, **experience**, **potion**, steam,
smoke, oxygen.

**One experience point is 100 mL**, and the rate is the same in both directions —
what a player pours into a drain is exactly what a drain set to *place* gives
back, as orbs.

!!! warning "One medium at a time, still"
    A network holds a single medium until it is emptied. `potion` is therefore one
    medium covering every variety: a network cannot yet tell a healing potion from
    a swiftness one. Filters wait on the same change.

## The Drain's Three Roles

Two are set with the goggles tinker: **drain** takes a world source into the
network, **place** spends network contents putting a source block back.

The third is decided by how you place the block. Stood **vertically** it stops
working on the world entirely and becomes the hand-loading point: sneak next to
it holding a full bucket, bottle or potion and its contents go into the network,
leaving the empty container in your hand. Sneaking is the gate, because a bucket
is also how you pick fluid up — without it, walking past with a full bucket would
quietly empty it.

With **nothing** it recognises in hand, it takes your **experience** instead: ten
points a cycle, 100 mL each, whole points only.

The network is charged before the swap, and only if it can take the whole
container. There is no such item as a part-full bucket.

## Throughput

The wrench cycles three steps on the two blocks whose whole job is a rate:

| Block | Steps |
|---|---|
| Liquid Drain | 2.5 / **5** / 10 L per second |
| EU Consumer | 20 / **40** / 80 EU per tick |

A consumer takes all or nothing, so the heavy setting simply idles on a grid that
cannot cover it — that is how you decide what a base spends its generation on.

## Bridges

A **bridge** belongs to neither of the networks it sits between, and that is what
makes it work: a node belongs to exactly one network, so anything that joined
would merge the two sides and leave nothing to bridge. The Boiler has always
worked this way. A bridge also breaks connectivity by existing — a pipe run with
a valve in it is two networks, always.

While powered by redstone, a bridge **evens the two networks out**: it takes from
whichever side holds more and gives it to whichever holds less, up to its `rate`
per tick, and stops when they are level.

**It has no facing.** It looks at all six neighbours, finds the networks touching
it, and moves from the fullest to the emptiest. Drop it into a pipe run any way
round and it works — there is nothing to get wrong and nothing to line up.

It cannot oscillate: the amount moved is capped at the difference between the two
sides, so the most that can happen is that they end up level, and once level
neither direction fires.

- **Liquid Valve / Gas Valve** — the fluid bridge. Carries the medium across, and
  refuses if the far side holds something different.
- **EU Breaker** — the same block for grids. Two bases with their own batteries
  share what they have through one.
- **Ender Power Vault** — a *wireless* bridge. It joins its local grid so wires
  connect to it, contributes no capacity, and moves EU to the vault on its channel
  — out of the grid at this end and into the grid at the far end. Whatever the far
  grid cannot accept is put straight back.

!!! warning "The Valve changed meaning"
    The Valve used to be a shutoff that cut a line by leaving its network. It is
    now a two-way link between two networks and needs redstone to do anything. An
    existing build using a valve as a closed tap will find it does not conduct at
    all until powered. To cut a fluid line now, remove a pipe.

Pipes, tanks and wires carry no `enabled` property — they are pure conductors and
capacity. The **EU Switch** is still a true shutoff: it leaves its network when
turned off, so the two halves genuinely become separate grids.

## Media

Media are named, not numbered:

| Key | Name | State | World block |
|---|---|---|---|
| `water` | Water | liquid | `minecraft:water` |
| `lava` | Lava | liquid | `minecraft:lava` |
| `powder_snow` | Powder Snow | liquid | `minecraft:powder_snow` |
| `milk` | Milk | liquid | — |
| `steam` | Steam | gas | — |
| `smoke` | Smoke | gas | — |
| `oxygen` | Oxygen | gas | — |

A network holds one medium at a time. It forgets its medium when drained to
empty, so a different one can be pumped in without breaking anything.

## Pumps and Drains

**Liquid Pump** — pulls a world fluid source into the network. It checks all six
adjacent blocks, so it works whichever way you place it. A source block is
all-or-nothing: the pump only takes it if the whole 1000 units fit, so a nearly
full network cannot delete a lake a partial bucket at a time.

**Liquid Drain** has two modes, cycled with the goggles tinker:

- `drain` — takes a world source into the network (like a pump, on a slower cycle).
- `place` — spends 1000 units putting a source block of the network's medium back
  into the world, on the first free side.

`place` mode is what makes a network worth building: carry lava from a pool to
wherever you want it, or refill cauldrons across a base.

### Infinite Sources

A body of **nine or more** matching source blocks within two blocks of the one
being taken counts as inexhaustible, and the block is left alone. Anything
smaller is genuinely consumed, so small pools empty out.

## Boiler and the EU Chain

The Boiler sits **between two networks** — it is deliberately not a member of
either, because a network holds one medium and it needs water on one side and
steam on the other.

```text
water network → [Boiler over a heat source] → steam network → [EU Generator] → EU
```

- Any block in `#ra_wires:heat_sources` under the Boiler will do: lava, fire,
  soul fire, magma block, campfire, soul campfire, lava cauldron.
- Consumes 100 water and produces 100 steam per cycle.
- The **EU Generator burns steam**, or solid fuel dropped straight into it — it
  is a barrel wearing a furnace skin, so loading it is just putting coal in a box.
  Either way it does not generate power from nothing.
- The **Solar Panel** generates EU from sky light instead, scaling with the
  vanilla daylight detector's own light reading — so night, rain, roofs and snow
  cover all reduce it automatically.

## Valves

A valve's `enabled` state genuinely **splits the network in two**. The halves
then hold separate contents and separate media. Closing a valve is the supported
way to isolate part of a system.

The state can be changed with the goggles tinker or the Data Handler; either way
the network follows.

## Visual Connectivity

Each node draws only **its own half** of a connection — from its core out to its
block boundary, never across into the neighbour. Displays are rebuilt only when a
conduit appears or disappears at a marker, never on a tick where nothing changed.

Fluid pipes render as a chunky 0.56 core in copper or iron. Electric wires are
deliberately thinner (0.26) and use concrete, so the two are easy to tell apart
at a glance.

## Goggles and Tinkering

Goggles show medium, amount, and per-block state lines.

Tinkering — sneak and hold goggles in the main hand near a node:

- Most nodes: toggle `enabled`.
- Drain: cycle mode between `drain` and `place`.

Pumps have nothing to configure; they take whatever they find next to them.

## Extending New Media

1. Add an entry to the registry in `ra_wires:media/init` — display name, state,
   colour, particle, and optionally a world block and bucket.
2. If it can be pumped out of the world, add a `source_blocks` entry naming the
   block state, the medium, the volume, and what the block becomes when drained.

That is all. Nothing else needs a per-medium branch.

---
