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
| Electric | Copper / L2 Wire, EU Generator, EU Consumer, EU Switch, Solar Panel | Fixed-rate EU generation, transfer and consumption |

## Flow Model

**Fluid contents belong to the network, not to individual pipes.**

A connected run of pipes, tanks, pumps, valves and drains is one *network* with a
single medium, a single amount and a capacity equal to the sum of its members.
Adding a pipe adds capacity; it does not add another buffer that fluid has to be
pushed through.

That means:

- Fluid does not travel. Anything a pump adds is immediately available to every
  drain on the same network, however far away.
- Pipes, tanks and valves do **no** per-tick work. Only pumps, drains and boilers
  are ticked. A hundred-block pipe run is free to keep running.
- Network membership is recomputed only when a node is placed or broken, and is
  debounced so laying a long run costs one rebuild rather than one per block.

Network state:

- `data.properties.*` — configuration the player can change (`enabled`, `mode`, `tier`).
- `data.status.*` — read-only values for goggles, refreshed every 20 ticks.

Pipes and tanks carry no `enabled` property. They are pure conductors and
capacity; use a **valve** to cut a line.

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
- The **EU Generator burns steam**. It does not generate power from nothing.
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
